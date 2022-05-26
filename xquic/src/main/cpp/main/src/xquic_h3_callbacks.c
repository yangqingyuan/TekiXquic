#include "xquic_h3_callbacks.h"
#include "xquic_h3_ctrl.h"

int client_h3_conn_create_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data) {
    DEBUG;
    int ret = xqc_h3_conn_is_ready_to_send_early_data(conn);
    LOGD("xqc_h3_conn_is_ready_to_send_early_data:%d\n", ret);
    return 0;
}

int client_h3_conn_close_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    xqc_conn_stats_t stats = xqc_conn_get_stats(user_conn->ctx->engine, cid);
    LOGD("send_count:%u, lost_count:%u, tlp_count:%u, recv_count:%u, srtt:%lu early_data_flag:%d, conn_err:%d, ack_info:%s\n",
         stats.send_count, stats.lost_count,
         stats.tlp_count, stats.recv_count, stats.srtt, stats.early_data_flag, stats.conn_err,
         stats.ack_info);

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

void client_h3_conn_handshake_finished(xqc_h3_conn_t *h3_conn, void *user_data) {
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

void client_h3_conn_ping_acked_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid,
                                      void *ping_user_data, void *user_data) {
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


int client_h3_request_create_notify(xqc_h3_request_t *h3_request, void *user_data) {
    DEBUG;

    return 0;
}


/**
 * [return] 1: all req suc, task finished, 0: still got req underway
 */
void client_on_stream_fin(xqc_cli_user_stream_t *user_stream) {
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

    /* TODO: fix MAX_STREAMS */
    /*if (ctx->schedule.schedule_info[task_idx].req_create_cnt
        < ctx->tasks[task_idx].user_conn->task->req_cnt) {
        xqc_cli_user_conn_t *user_conn = user_stream->user_conn;
        xqc_cli_ctx_t *ctx = user_conn->ctx;
        int task_idx = user_conn->task->task_idx;
        int req_create_cnt = ctx->task_ctx.schedule.schedule_info[task_idx].req_create_cnt;
        int req_cnt = user_conn->task->req_cnt - req_create_cnt;
        if (req_cnt < 0) {
            LOGE("等待补充逻辑");
        }
    }*/
}

int client_h3_request_close_notify(xqc_h3_request_t *h3_request, void *user_data) {
    DEBUG;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;

    /* close stream */
    client_on_stream_fin(user_stream);

    return 0;
}


int client_h3_request_read_notify(xqc_h3_request_t *h3_request, xqc_request_notify_flag_t flag,
                                  void *user_data) {
    //DEBUG;
    unsigned char fin = 0;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;
    uint64_t recv_time = xqc_now();
    user_stream->user_conn->last_sock_read_time = recv_time;

    /* read headers */
    if (flag & XQC_REQ_NOTIFY_READ_HEADER) {
        xqc_http_headers_t *headers;
        headers = xqc_h3_request_recv_headers(h3_request, &fin);
        if (headers == NULL) {
            LOGE("xqc h3 request recv headers error");
            return -1;
        }

        cJSON *usr = cJSON_CreateObject();
        LOGD("============ response head start ================");
        for (int i = 0; i < headers->count; ++i) {
            cJSON_AddStringToObject(usr, (char *) headers->headers[i].name.iov_base,
                                    (char *) headers->headers[i].value.iov_base);
            LOGD("--> %s, %s \n", (char *) headers->headers[i].name.iov_base,
                 (char *) headers->headers[i].value.iov_base);
        }
        char *out = cJSON_Print(usr);
        callback_msg_to_client(user_stream->user_conn->ctx->args, MSG_TYPE_HEAD, out, strlen(out));
        cJSON_Delete(usr);
        free(out);
        LOGD("============ response head end ================");

        if (fin) {
            user_stream->recv_fin = 1;
            LOGW("client_h3_request_read_notify fin ,only recv header data,not body data !!");
            goto finish;
        }
    }

    /* continue to recv body */
    if (!(flag & XQC_REQ_NOTIFY_READ_BODY)) {
        return 0;
    }

    char buff[4096] = {0};
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
        read = xqc_h3_request_recv_body(h3_request, buff, buff_size, &fin);
        if (read == -XQC_EAGAIN) {
            break;
        } else if (read < 0) {
            LOGE("xqc h3 request recv body error %zd", read);
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

    if (flag & XQC_REQ_NOTIFY_READ_EMPTY_FIN) {
        fin = 1;
    }

    if (read > 0) {
        LOGI("xqc h3 request recv body size %lu, fin:%d", user_stream->recv_body_len, fin);
    }

    finish:
    if (fin) {  /* finish */
        user_stream->recv_fin = 1;

        xqc_request_stats_t stats;
        stats = xqc_h3_request_get_stats(h3_request);
        xqc_msec_t now_us = xqc_now();
        LOGI(">>>>>>>> request time cost:%lu us, speed:%lu K/s , send_body_size:%zu, recv_body_size:%zu <<<<<<<<<\n",
             now_us - user_stream->start_time,
             (stats.send_body_size + stats.recv_body_size) * 1000 /
             (now_us - user_stream->start_time),
             stats.send_body_size, stats.recv_body_size);

        /* call back to client */
        callback_data_to_client(user_stream->user_conn, XQC_OK, user_stream->recv_body,
                                user_stream->user_tag);

        /* auto to close request */
        int ret = xqc_h3_request_finish(h3_request);
        //int ret = xqc_h3_request_close(h3_request);
        LOGD("auto to call xqc_h3_request_close ret=%d", ret);

        if (user_stream->user_conn->ctx->args->net_cfg.conn_type == CONN_TYPE_SHORT) {
            /* auto to close conn */
            ret = xqc_h3_conn_close(user_stream->user_conn->ctx->engine,
                                    &user_stream->user_conn->cid);
            LOGD("auto to call xqc_h3_conn_close ret=%d", ret);
        }
    }
    return 0;
}


int client_h3_request_write_notify(xqc_h3_request_t *h3_request, void *user_data) {
    DEBUG;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;
    return client_send_h3_content(user_stream);
}
