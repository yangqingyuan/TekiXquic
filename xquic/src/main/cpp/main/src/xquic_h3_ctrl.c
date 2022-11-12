#include "xquic_h3_ctrl.h"
#include "xquic_h3_callbacks.h"

/**
 * 发送h3的内容
 * @param user_stream
 * @return
 */
ssize_t client_send_h3_content(xqc_cli_user_stream_t *user_stream) {
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
                                           1);
            if (ret == -XQC_EAGAIN) {
                return 0;
            } else if (ret < 0) {
                LOGW("client send h3 body error size=%zd", ret);
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

    xqc_cli_user_data_params_t *user_params = &(user_conn->ctx->args->user_params);

    if (user_params->header_count > 0 && req != NULL) {
        if (req->count > 0) {
            user_stream->h3_hdrs.headers = req->headers;
            user_stream->h3_hdrs.count = req->count;
        } else {
            LOGD("=========== request head start =========A========");
            for (int i = 0; i < user_params->header_count; ++i) {
                req->headers[i].name.iov_base = (void *) user_params->headers[i].name;
                req->headers[i].name.iov_len = user_params->headers[i].name_len;

                req->headers[i].value.iov_base = (void *) user_params->headers[i].value;
                req->headers[i].value.iov_len = user_params->headers[i].value_len;
                req->headers[i].flags = user_params->headers[i].flags;
                LOGD("--> %s, %s", (char *) req->headers[i].name.iov_base,
                     (char *) req->headers[i].value.iov_base);
            }
            LOGD("============ request head end =======A=========");
        }
        user_stream->h3_hdrs.headers = req->headers;
        user_stream->h3_hdrs.count = user_params->header_count;

        //发送h3内容
        return client_send_h3_content(user_stream);
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