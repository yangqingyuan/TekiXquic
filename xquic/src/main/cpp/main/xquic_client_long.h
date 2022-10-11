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
int client_long_send_ping(xqc_cli_ctx_t *ctx, const char *ping_content,int len);

/**
 * H3的方式发送内容
 * @param ctx
 * @param data_type 0:json 1:other
 * @param content
 * @return
 */
int client_long_send(xqc_cli_ctx_t *ctx, const char *content, send_data_type_t data_type, int len);

/**
 *
 * @param ctx
 * @param headers
 * @param content
 * @return
 */
int client_long_send_with_head(xqc_cli_ctx_t *ctx, xqc_http_header_t *headers, int headers_size,
                               const char *content);

/**
 * 取消
 * @return
 */
int client_long_cancel(xqc_cli_ctx_t *ctx);

#ifdef __cplusplus
}
#endif
#endif