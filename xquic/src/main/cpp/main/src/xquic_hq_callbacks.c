#include "xquic_hq_callbacks.h"
#include "xquic_hq_ctrl.h"

int xqc_client_conn_create_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *user_data,
                                  void *conn_proto_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    xqc_conn_set_alp_user_data(conn, user_conn);
    int ret = xqc_conn_is_ready_to_send_early_data(conn);
    LOGD("xqc_conn_is_ready_to_send_early_data:%d\n", ret);
    return ret;
}

int
xqc_client_conn_close_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *user_data,
                             void *conn_proto_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    xqc_conn_stats_t stats = xqc_conn_get_stats(user_conn->ctx->engine, cid);

    LOGD("send_count:%u, lost_count:%u, tlp_count:%u, recv_count:%u, conn_err:%d\n",
         stats.send_count, stats.lost_count,
         stats.tlp_count, stats.recv_count, stats.conn_err);

    /* chang status to finished */
    user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].status = TASK_STATUS_FINISHED;

    xqc_cli_ctx_t *ctx = user_conn->ctx;
    xqc_cli_task_t *task = user_conn->task;
    LOGI("task finished, total task_req_cnt: %d, req_fin_cnt: %d, req_sent_cnt: %d, "
         "req_create_cnt: %d\n", task->req_cnt,
         ctx->task_ctx.schedule.schedule_info[task->task_idx].req_fin_cnt,
         ctx->task_ctx.schedule.schedule_info[task->task_idx].req_sent_cnt,
         ctx->task_ctx.schedule.schedule_info[task->task_idx].req_create_cnt);

    /* call method client_task_schedule_callback */
    ctx->msg_data.cmd_type = CMD_TYPE_DESTROY;
    ev_async_send(ctx->eb, &ctx->ev_task);
    return 0;
}

void
xqc_client_conn_handshake_finished(xqc_connection_t *conn, void *user_data, void *conn_proto_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    xqc_conn_stats_t stats = xqc_conn_get_stats(user_conn->ctx->engine, &user_conn->cid);
    callback_msg_to_client(user_conn->ctx->args, MSG_TYPE_HANDSHAKE, "handshake_finished", 18);
    char ortt_info[100] = {0};
    char info[50] = "without 0-RTT";
    if (stats.early_data_flag == XQC_0RTT_ACCEPT) {
        strcpy(info, "0-RTT was accepted");
    } else if (stats.early_data_flag == XQC_0RTT_REJECT) {
        strcpy(info, "0-RTT was rejected");
    }

    sprintf(ortt_info, ">>>>>>>> 0rtt_flag:%d(%s)<<<<<<<<<", stats.early_data_flag, info);
    LOGI("%s", ortt_info);
}

void xqc_client_conn_ping_acked_notify(xqc_connection_t *conn, const xqc_cid_t *cid,
                                  void *ping_user_data, void *user_data, void *conn_proto_data) {
    //DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    uint64_t recv_time = xqc_now();
    user_conn->last_sock_read_time = recv_time;

    size_t len = 0;
    if (ping_user_data != NULL) {
        len = strlen(ping_user_data);
    }
    callback_msg_to_client(user_conn->ctx->args, MSG_TYPE_PING, ping_user_data, len);
}

int xqc_client_stream_write_notify(xqc_stream_t *stream, void *user_data) {
    DEBUG;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;
    return client_send_hq_content(user_stream);
}

int xqc_client_stream_read_notify(xqc_stream_t *stream, void *user_data) {
    //DEBUG;
    unsigned char fin = 0;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;
    uint64_t recv_time = xqc_now();
    user_stream->user_conn->last_sock_read_time = recv_time;

    unsigned char buff[4096] = {0};
    size_t buff_size = 4096;

    //TODO 最好根据后端返回动态的调整
    if (user_stream->recv_body == NULL) {
        user_stream->recv_body = malloc(user_stream->recv_body_max_len);
        memset(user_stream->recv_body, 0, user_stream->recv_body_max_len);
    }
    ssize_t read = 0;
    ssize_t read_sum = 0;
    do {
        /* read body */
        read = xqc_stream_recv(stream, buff, buff_size, &fin);
        if (read == -XQC_EAGAIN) {
            break;
        } else if (read < 0) {
            LOGE("xqc hq request recv body error=%zd ,fin=%d", read,fin);
            return 0;
        }

        /* copy body to memory */
        if (user_stream->recv_body_len + read <= user_stream->recv_body_max_len) {
            memcpy(user_stream->recv_body + user_stream->recv_body_len, buff, read);
        } else {
            LOGW("revc data size > recv body max len %lu throw away",
                 user_stream->recv_body_max_len);
        }

        read_sum += read;
        user_stream->recv_body_len += read;

    } while (read > 0 && !fin);


    if (fin) {  /* finish */
        user_stream->recv_fin = 1;

        /* call back to client */
        callback_data_to_client(user_stream->user_conn, XQC_OK, user_stream->recv_body,
                                user_stream->user_tag);

        /* auto to close request */
        int ret = xqc_stream_close(stream);
        LOGD("auto to call xqc_stream_close ret=%d", ret);

        if (user_stream->user_conn->ctx->args->net_cfg.conn_type == CONN_TYPE_SHORT) {
            /* auto to close conn */
            ret = xqc_conn_close(user_stream->user_conn->ctx->engine,
                                 &user_stream->user_conn->cid);
            LOGD("auto to call xqc_conn_close ret=%d", ret);
        }
    }
    return 0;
}

int xqc_client_stream_close_notify(xqc_stream_t *stream, void *user_data) {
    DEBUG;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;

    xqc_cli_task_ctx_t *ctx = &user_stream->user_conn->ctx->task_ctx;
    int task_idx = user_stream->user_conn->task->task_idx;

    /* all reqs are finished, finish the task */
    if (++ctx->schedule.schedule_info[task_idx].req_fin_cnt == ctx->tasks[task_idx].req_cnt) {
        ctx->schedule.schedule_info[task_idx].fin_flag = 1;
    }

    LOGD("client stream fin task[%d], fin_cnt: %d, fin_flag: %d\n", task_idx,
         ctx->schedule.schedule_info[task_idx].req_fin_cnt,
         ctx->schedule.schedule_info[task_idx].fin_flag);

    if (user_stream->user_conn->ctx->args->net_cfg.conn_type == CONN_TYPE_LONG) {

        if (user_stream->send_body != NULL) {
            free(user_stream->send_body);
            user_stream->send_body = NULL;
        }
        if (user_stream->recv_body != NULL) {
            free(user_stream->recv_body);
            user_stream->recv_body = NULL;
        }
        free(user_stream);
        user_stream = NULL;
        LOGD("free stream success");
    }
    return 0;
}