#include "xquic_common.h"

#ifndef _Hncluded_XQUIC_SOCKET
#define _Hncluded_XQUIC_SOCKET

#ifdef __cplusplus

extern "C"{
#endif

int client_parse_server_addr(xqc_cli_net_config_t *cfg, const char *url,
                             xqc_cli_user_callback_t *user_callback);

int client_create_socket(xqc_cli_user_conn_t *user_conn, xqc_cli_net_config_t *cfg);

void client_socket_read_handler(xqc_cli_user_conn_t *user_conn);

void client_socket_write_handler(xqc_cli_user_conn_t *user_conn);

#ifdef __cplusplus
}
#endif
#endif