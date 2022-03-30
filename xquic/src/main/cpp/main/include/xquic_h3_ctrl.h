#include "xquic_common.h"

#ifndef _Hncluded_XQUIC_H3_CTRL
#define _Hncluded_XQUIC_H3_CTRL

#ifdef __cplusplus

extern "C"{
#endif

ssize_t client_send_h3_content(xqc_cli_user_stream_t *user_stream);

int client_send_h3_requests(xqc_cli_user_conn_t *user_conn,
                            xqc_cli_user_stream_t *user_stream, xqc_cli_request_t *req);

#ifdef __cplusplus
}
#endif
#endif