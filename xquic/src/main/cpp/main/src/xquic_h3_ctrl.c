#include "xquic_h3_ctrl.h"
#include "xquic_h3_callbacks.h"

/**
 * 发送h3的内容
 * @param user_stream
 * @return
 */
ssize_t client_send_h3_content(xqc_cli_user_stream_t *user_stream, int finish_flag) {
    ssize_t ret = 0;
    if (!user_stream->hdr_sent) {
        if (user_stream->start_time == 0) {
            user_stream->start_time = xqc_now();
        }

        int fin = 0;
        if (user_stream->send_body == NULL) {
            fin = 1;
        }

        ret = xqc_h3_request_send_headers(user_stream->h3_request, &user_stream->h3_hdrs, fin);
        if (ret < 0) {
            LOGE("client send h3 headers error size=%zd", ret);
        } else {
            LOGD("client send h3 headers success size=%zd", ret);
            if (user_stream->send_body == NULL) {
                user_stream->hdr_sent = 1;
                LOGW("client send h3 content warn,send body is NULL");
                return ret;
            }
        }

        if (user_stream->send_offset < user_stream->send_body_len) {
            ret = xqc_h3_request_send_body(user_stream->h3_request,
                                           (unsigned char *) user_stream->send_body +
                                           user_stream->send_offset,
                                           user_stream->send_body_len -
                                           user_stream->send_offset,
                                           finish_flag);
            if (ret < 0) {
                LOGE("client send h3 body error size=%zd", ret);
                return 0;
            } else {
                user_stream->send_offset += ret;
                LOGD("client send h3 body success size=%zd", ret);
                if (user_stream->send_offset >= user_stream->send_body_len) {
                    user_stream->hdr_sent = 1;
                }
            }
        }
    }
    return ret;
}

/**
 * 发送h3请求
 * @param user_conn
 * @param args
 * @return
 */
ssize_t client_send_h3_requests(xqc_cli_user_conn_t *user_conn,
                                xqc_cli_user_stream_t *user_stream, xqc_cli_request_t *req) {
    DEBUG;

    /* 创建请求 */
    user_stream->h3_request = xqc_h3_request_create(user_conn->ctx->engine, &user_conn->cid,
                                                    user_stream);

    if (user_stream->h3_request == NULL) {
        LOGE("xqc h3 request create error");
        return -1;
    }

    xqc_cli_user_data_params_t *user_callback = user_conn->ctx->args->user_callback;

    if (user_callback->h3_hdrs.count > 0) {
        if (req != NULL && req->count > 0) {
            user_stream->h3_hdrs.headers = req->headers;
            user_stream->h3_hdrs.count = req->count;
        } else {
            user_stream->h3_hdrs.headers = user_callback->h3_hdrs.headers;
            user_stream->h3_hdrs.count = user_callback->h3_hdrs.count;
        }

        xqc_http_header_t *headers = user_stream->h3_hdrs.headers;
        LOGD("=========== request head start =================");
        for (int i = 0; i < user_stream->h3_hdrs.count; i++) {
            xqc_http_header_t header = headers[i];
            LOGD("--> %s, %s", (char *) header.name.iov_base,
                 (char *) header.value.iov_base);
        }
        LOGD("============ request head end ================");
        //发送h3内容
        return client_send_h3_content(user_stream, user_conn->ctx->args->req_cfg.finishFlag);
    }
    return 0;
}

/**
 * send ping
 * @param user_conn
 * @return
 */
ssize_t client_send_H3_ping(xqc_cli_user_conn_t *user_conn, char *ping_user_data) {
    xqc_h3_conn_send_ping(user_conn->ctx->engine, &user_conn->cid, ping_user_data);
    return 0;
}