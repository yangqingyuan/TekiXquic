#include "xquic_h3_ctrl.h"
#include "xquic_h3_callbacks.h"

char method_type[][16] = {
        {"GET"},
        {"POST"}
};

/**
 * 格式化h3请求
 * @return
 */
int client_format_h3_request(xqc_http_header_t *headers, size_t sz, xqc_cli_request_t *req) {
    xqc_http_header_t req_hdr[] = {
            {
                    .name = {.iov_base = ":method", .iov_len = 7},
                    .value = {.iov_base = method_type[req->method], .iov_len = strlen(
                            method_type[req->method])},
                    .flags = 0,
            },
            {
                    .name = {.iov_base = ":scheme", .iov_len = 7},
                    .value = {.iov_base = req->scheme, .iov_len = strlen(req->scheme)},
                    .flags = 0,
            },
            {
                    .name = {.iov_base = ":path", .iov_len = 5},
                    .value = {.iov_base = req->path, .iov_len = strlen(req->path)},
                    .flags = 0,
            },
            {
                    .name = {.iov_base = ":authority", .iov_len = 10},
                    .value = {.iov_base = req->auth, .iov_len = strlen(req->auth)},
                    .flags = 0,
            }
    };

    size_t req_sz = sizeof(req_hdr) / sizeof(req_hdr[0]);
    if (sz < req_sz) {
        return -1;
    }

    for (size_t i = 0; i < req_sz; i++) {
        headers[i] = req_hdr[i];
    }

    return req_sz;
}

/**
 * 发送h3的内容
 * @param user_stream
 * @return
 */
ssize_t client_send_h3_content(xqc_cli_user_stream_t *user_stream) {
    LOGI(">>>>>>>> start send h3 content <<<<<<<<");
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
            LOGI("client send h3 headers error size=%zd", ret);
        } else {
            LOGI("client send h3 headers success size=%zd", ret);
            if (user_stream->send_body == NULL) {
                user_stream->hdr_sent = 1;
                LOGW("client send h3 content warn,send body is NULL");
                return ret;
            }
        }

        if (user_stream->send_offset < user_stream->send_body_len) {
            ret = xqc_h3_request_send_body(user_stream->h3_request,
                                           user_stream->send_body + user_stream->send_offset,
                                           user_stream->send_body_len -
                                           user_stream->send_offset,
                                           1);
            if (ret < 0) {
                LOGE("client send h3 body error size=%zd", ret);
                return 0;
            } else {
                user_stream->send_offset += ret;
                LOGI("client send h3 body success size=%zd", ret);
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
int client_send_h3_requests(xqc_cli_user_conn_t *user_conn,
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
        user_stream->h3_hdrs.headers = user_callback->h3_hdrs.headers;
        user_stream->h3_hdrs.count = user_callback->h3_hdrs.count;

        xqc_http_header_t *headers = user_stream->h3_hdrs.headers;
        LOGD("=========== request head start =================");
        for (int i = 0; i < user_stream->h3_hdrs.count; i++) {
            xqc_http_header_t header = headers[i];
            LOGD("= header name = %s, value =%s", (char *) header.name.iov_base,
                 (char *) header.value.iov_base);
        }
        LOGD("============ request head end ================");

        //发送h3内容
        client_send_h3_content(user_stream);
    }
    return 0;
}
