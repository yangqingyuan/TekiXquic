#include "common.h"

#ifndef _Hncluded_XQUIC_H3_CTRL
#define _Hncluded_XQUIC_H3_CTRL

#ifdef __cplusplus

extern "C"{
#endif

int xqc_client_hq_init(client_ctx_t* ctx);
int xqc_client_hq_conn(client_ctx_t* ctx);
int xqu_client_hq_send(client_ctx_t* ctx);

#ifdef __cplusplus
}
#endif
#endif