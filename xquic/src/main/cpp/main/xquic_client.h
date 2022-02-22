#include "common.h"

#ifndef _Hncluded_XQUIC_CLIENT
#define _Hncluded_XQUIC_CLIENT

#ifdef __cplusplus

extern "C"{
#endif


client_ctx_t * client_init();
int client_connect(client_ctx_t * client,const char *host ,int port,const char *token,const char* session,const char*transport);
int client_destroy(client_ctx_t * client);

#ifdef __cplusplus
}
#endif
#endif