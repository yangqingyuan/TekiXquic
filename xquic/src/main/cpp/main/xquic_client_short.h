#include "common.h"

#ifndef _Hncluded_XQUIC_CLIENT_SHORT
#define _Hncluded_XQUIC_CLIENT_SHORT


/**
* xquic逻辑入口
*/
#ifdef __cplusplus

extern "C"{
#endif

//H3的方式发送内容
int client_send(const char *url ,const char *token,const char* session,const char *content);

#ifdef __cplusplus
}
#endif
#endif