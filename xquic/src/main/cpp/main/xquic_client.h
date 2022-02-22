#include "common.h"

#ifndef _Hncluded_XQUIC_CLIENT
#define _Hncluded_XQUIC_CLIENT

#ifdef __cplusplus

extern "C"{
#endif

client_ctx_t * init_client(const char *host ,int port,const char *token,const char* session);
int start_client();
int destroy_client();

#ifdef __cplusplus
}
#endif
#endif