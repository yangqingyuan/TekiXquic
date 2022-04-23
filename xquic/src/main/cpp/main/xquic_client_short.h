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
int client_short_send(xqc_cli_user_data_params_t *user_param);

/**
 * cancel
 * @param ctx
 * @return
 */
int client_short_cancel(xqc_cli_ctx_t *ctx);


#ifdef __cplusplus
}
#endif
#endif