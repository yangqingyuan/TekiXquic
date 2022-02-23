#include "common.h"

#ifndef _Hncluded_XQUIC_SOCKET
#define _Hncluded_XQUIC_SOCKET

#ifdef __cplusplus

extern "C"{
#endif

user_conn_t * xqc_client_user_conn_create(client_ctx_t* ctx,const char *server_addr, int server_port);

#ifdef __cplusplus
}
#endif
#endif