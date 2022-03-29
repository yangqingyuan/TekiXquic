#include "xquic_common.h"

#ifndef _Hncluded_XQUIC_SOCKET
#define _Hncluded_XQUIC_SOCKET

#ifdef __cplusplus

extern "C"{
#endif

int client_create_socket(xqc_cli_user_conn_t *user_conn, xqc_cli_net_config_t *cfg);

#ifdef __cplusplus
}
#endif
#endif