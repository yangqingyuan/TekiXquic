#include "app_proto_callbacks.h"


int
xqc_client_conn_create_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *user_data)
{
    DEBUG;
    return 0;
}

int
xqc_client_conn_close_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *user_data){
     DEBUG;
     return 0;
}

void
xqc_client_conn_handshake_finished(xqc_connection_t *conn, void *user_data){
     DEBUG;
}

void
xqc_client_conn_ping_acked_notify(xqc_connection_t *conn, const xqc_cid_t *cid, void *ping_user_data, void *user_data)
{
    DEBUG;
    if (ping_user_data) {
        printf("====>ping_id:%d\n", *(int *) ping_user_data);

    } else {
        printf("====>no ping_id\n");
    }
}

int
xqc_client_stream_write_notify(xqc_stream_t *stream, void *user_data)
{
     DEBUG;
     return 0;
}

int
xqc_client_stream_read_notify(xqc_stream_t *stream, void *user_data)
{
     DEBUG;
     return 0;
}

int
xqc_client_stream_close_notify(xqc_stream_t *stream, void *user_data)
{
     DEBUG;
     return 0;
}