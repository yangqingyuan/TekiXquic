//
// Created by lizhi on 2022/6/27.
//
#include "xquic_common.h"

#ifndef TEKIXQUIC_XQUIC_HQ_CTRL_H
#define TEKIXQUIC_XQUIC_HQ_CTRL_H

ssize_t client_send_hq_content(xqc_cli_user_stream_t *user_stream, int finish_flag);


ssize_t client_send_hq_requests(xqc_cli_user_conn_t *user_conn,
                                xqc_cli_user_stream_t *user_stream, xqc_cli_request_t *req);

ssize_t client_send_hq_ping(xqc_cli_user_conn_t *user_conn, char *ping_user_data);

#endif //TEKIXQUIC_XQUIC_HQ_CTRL_H
