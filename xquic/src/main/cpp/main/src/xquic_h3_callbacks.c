#include "xquic_h3_callbacks.h"


int xqc_client_h3_conn_create_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data) {
    DEBUG;
    int ret = xqc_h3_conn_is_ready_to_send_early_data(conn);
    LOGI("xqc_h3_conn_is_ready_to_send_early_data:%d\n", ret);
    return 0;
}

int xqc_client_h3_conn_close_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    xqc_conn_stats_t stats = xqc_conn_get_stats(user_conn->ctx->engine, cid);
    LOGI("send_count:%u, lost_count:%u, tlp_count:%u, recv_count:%u, srtt:%lu early_data_flag:%d, conn_err:%d, ack_info:%s\n",
         stats.send_count, stats.lost_count,
         stats.tlp_count, stats.recv_count, stats.srtt, stats.early_data_flag, stats.conn_err,
         stats.ack_info);

    user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].status = TASK_STATUS_FINISHED;

    xqc_cli_ctx_t *ctx = user_conn->ctx;
    xqc_cli_task_t *task = user_conn->task;
    LOGI("task finished, total task_req_cnt: %d, req_fin_cnt: %d, req_sent_cnt: %d, "
         "req_create_cnt: %d\n", task->req_cnt,
         ctx->task_ctx.schedule.schedule_info[task->task_idx].req_fin_cnt,
         ctx->task_ctx.schedule.schedule_info[task->task_idx].req_sent_cnt,
         ctx->task_ctx.schedule.schedule_info[task->task_idx].req_create_cnt);
    free(user_conn);
    return 0;
}

void xqc_client_h3_conn_handshake_finished(xqc_h3_conn_t *h3_conn, void *user_data) {
    DEBUG;
}

void xqc_client_h3_conn_ping_acked_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid,
                                          void *ping_user_data, void *user_data) {
    DEBUG;
}


int xqc_client_request_create_notify(xqc_h3_request_t *h3_request, void *user_data) {
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

    LOGI("client stream fin task[%d], fin_cnt: %d, fin_flag: %d\n", task_idx,
         ctx->schedule.schedule_info[task_idx].req_fin_cnt,
         ctx->schedule.schedule_info[task_idx].fin_flag);

    /* TODO: fix MAX_STREAMS */
    if (ctx->schedule.schedule_info[task_idx].req_create_cnt
        < ctx->tasks[task_idx].user_conn->task->req_cnt) {
        xqc_cli_user_conn_t *user_conn = user_stream->user_conn;
        xqc_cli_ctx_t *ctx = user_conn->ctx;
        int task_idx = user_conn->task->task_idx;
        int req_create_cnt = ctx->task_ctx.schedule.schedule_info[task_idx].req_create_cnt;
        int req_cnt = user_conn->task->req_cnt - req_create_cnt;
        if (req_cnt < 0) {
            LOGE("等待补充逻辑");
        }
    }
}

int xqc_client_request_close_notify(xqc_h3_request_t *h3_request, void *user_data) {
    DEBUG;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;

    /* close stream */
    client_on_stream_fin(user_stream);

    return 0;
}


int xqc_client_request_read_notify(xqc_h3_request_t *h3_request, xqc_request_notify_flag_t flag,
                                   void *user_data) {
    DEBUG;
    unsigned char fin = 0;
    xqc_cli_user_stream_t *user_stream = (xqc_cli_user_stream_t *) user_data;

    /* read headers */
    if (flag & XQC_REQ_NOTIFY_READ_HEADER) {
        xqc_http_headers_t *headers;
        headers = xqc_h3_request_recv_headers(h3_request, &fin);
        if (headers == NULL) {
            LOGE("xqc h3 request recv headers error");
            return -1;
        }

        for (int i = 0; i < headers->count; ++i) {
            LOGI("header %s = %s \n", (char *) headers->headers[i].name.iov_base,
                 (char *) headers->headers[i].value.iov_base);
        }

        if (fin) {
            user_stream->recv_fin = 1;
            return 0;
        }
    }

    if (!(flag & XQC_REQ_NOTIFY_READ_BODY)) {
        return 0;
    }

    char buff[4096] = {0};
    size_t buff_size = 4096;

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

        read_sum += read;
        //char dataTemp[read];
        //memcpy(dataTemp, buff, read);
        LOGI("xqc h3 request recv body length=%d, data=%s", read, buff);

        user_stream->recv_body_len += read;

    } while (read > 0 && !fin);

    if (read > 0) {
        LOGI("xqc h3 request recv body size %zd, fin:%d", read, fin);
    }

    /* finish */
    if (fin) {
        user_stream->recv_fin = 1;

        xqc_request_stats_t stats;
        stats = xqc_h3_request_get_stats(h3_request);
        xqc_msec_t now_us = xqc_now();
        LOGI(">>>>>>>> request time cost:%lu us, speed:%lu K/s , send_body_size:%zu, recv_body_size:%zu <<<<<<<<<\n",
             now_us - user_stream->start_time,
             (stats.send_body_size + stats.recv_body_size) * 1000 /
             (now_us - user_stream->start_time),
             stats.send_body_size, stats.recv_body_size);
    }
    return 0;
}


int xqc_client_request_write_notify(xqc_h3_request_t *h3_request, void *user_data) {
    DEBUG;
    ssize_t ret = 0;

    return ret;
}
