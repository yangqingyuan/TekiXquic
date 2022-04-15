#include "common.h"

#ifndef _Hncluded_XQUIC_CLIENT_LONG
#define _Hncluded_XQUIC_CLIENT_LONG


/**
* xquic逻辑入口
*/
#ifdef __cplusplus

extern "C"{
#endif

/**
 * 链接
 * @param user_cfg
 * @return
 */
xqc_cli_ctx_t *client_long_conn(xqc_cli_user_data_params_t *user_cfg);

/**
 * 开始
 * @param ctx
 * @return
 */
int client_long_start(xqc_cli_ctx_t *ctx);

/**
 * 发送ping内容
 * @param ctx
 * @param ping_content
 * @return
 */
int client_long_send_ping(xqc_cli_ctx_t *ctx, char *ping_content);

/**
 * H3的方式发送内容
 * @param ctx
 * @param content
 * @return
 */
int client_long_send(xqc_cli_ctx_t *ctx, char *content);

/**
 * 取消
 * @return
 */
int client_long_cancel();

#ifdef __cplusplus
}
#endif
#endif