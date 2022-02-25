#include "xquic_h3_callbacks.h"

int
xqc_client_h3_conn_create_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data)
{
     DEBUG;
     user_conn_t *user_conn = (user_conn_t *) user_data;
     int ret = xqc_h3_conn_is_ready_to_send_early_data(conn);
     LOGI("xqc_h3_conn_is_ready_to_send_early_data:%d\n",ret );
     return 0;
}

int
xqc_client_h3_conn_close_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *user_data)
{
    DEBUG;

    user_conn_t *user_conn = (user_conn_t *) user_data;
    LOGE("conn errno:%d\n", xqc_h3_conn_get_errno(conn));

    xqc_conn_stats_t stats = xqc_conn_get_stats(ctx.engine, cid);
    LOGE("send_count:%u, lost_count:%u, tlp_count:%u, recv_count:%u, srtt:%"PRIu64" early_data_flag:%d, conn_err:%d, ack_info:%s\n",
           stats.send_count, stats.lost_count, stats.tlp_count, stats.recv_count, stats.srtt, stats.early_data_flag, stats.conn_err, stats.ack_info);
    return 0;
}

void
xqc_client_h3_conn_handshake_finished(xqc_h3_conn_t *h3_conn, void *user_data)
{
    DEBUG;
}

void
xqc_client_h3_conn_ping_acked_notify(xqc_h3_conn_t *conn, const xqc_cid_t *cid, void *ping_user_data, void *user_data)
{
    DEBUG;
}

void
xqc_client_h3_conn_update_cid_notify(xqc_h3_conn_t *conn, const xqc_cid_t *retire_cid, const xqc_cid_t *new_cid, void *user_data)
{
    DEBUG;
}


int
xqc_client_request_close_notify(xqc_h3_request_t *h3_request, void *user_data)
{
    DEBUG;
    xqc_request_stats_t stats;
    stats = xqc_h3_request_get_stats(h3_request);
    LOGD("send_body_size:%zu, recv_body_size:%zu, send_header_size:%zu, recv_header_size:%zu, err:%d\n",
           stats.send_body_size, stats.recv_body_size,
           stats.send_header_size, stats.recv_header_size,
            stats.stream_err);
    return 0;
}


int
xqc_client_request_read_notify(xqc_h3_request_t *h3_request, xqc_request_notify_flag_t flag, void *user_data)
{
    DEBUG;
    return 0;
}


int
xqc_client_request_write_notify(xqc_h3_request_t *h3_request, void *user_data)
{
    DEBUG;
    ssize_t ret = 0;
    return ret;
}
