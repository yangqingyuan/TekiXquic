#include "common.h"

#ifndef _Hncluded_XQUIC_CLIENT
#define _Hncluded_XQUIC_CLIENT


/**
* xquic逻辑入口
*/
#ifdef __cplusplus

extern "C"{
#endif

//初始化引擎
client_ctx_t * client_init();

//初始化完毕后链接
int client_connect(client_ctx_t * client,const char *host ,int port,const char *token,const char* session,const char*transport);

//链接完毕后开始
void client_start(client_ctx_t * client);

//H3的方式发送内容
int client_send_h3(client_ctx_t * client,xqc_http_headers_t* headers,const char *body);

//HQ的方式返送内容
int client_send_hq(client_ctx_t * client,const char *body);

//销毁
int client_destroy(client_ctx_t * client);

#ifdef __cplusplus
}
#endif
#endif