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
int client_send(xqc_cli_user_data_params_t *user_cfg);

#ifdef __cplusplus
}
#endif
#endif