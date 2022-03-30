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

    xqc_request_stats_t stats;
    stats = xqc_h3_request_get_stats(h3_request);
    LOGD("send_body_size:%zu, recv_body_size:%zu, send_header_size:%zu, recv_header_size:%zu, err:%d\n",
         stats.send_body_size, stats.recv_body_size,
         stats.send_header_size, stats.recv_header_size,
         stats.stream_err);

    return 0;
}


int xqc_client_request_read_notify(xqc_h3_request_t *h3_request, xqc_request_notify_flag_t flag,
                                   void *user_data) {
    //DEBUG;
    return 0;
}


int xqc_client_request_write_notify(xqc_h3_request_t *h3_request, void *user_data) {
    DEBUG;
    ssize_t ret = 0;
    return ret;
}
