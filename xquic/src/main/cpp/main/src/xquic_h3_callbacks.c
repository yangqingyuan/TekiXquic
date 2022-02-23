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
