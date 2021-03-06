//
// Created by yqy on 2022/6/27.
//

#include "xquic_hq_ctrl.h"

ssize_t client_send_hq_content(xqc_cli_user_stream_t *user_stream) {
    DEBUG;
    ssize_t ret = 0;
    if (!user_stream->hdr_sent) {
        if (user_stream->start_time == 0) {
            user_stream->start_time = xqc_now();
        }

        if (user_stream->send_offset < user_stream->send_body_len) {
            ret = xqc_stream_send(user_stream->hq_request,
                                  (unsigned char *) user_stream->send_body +
                                  user_stream->send_offset,
                                  user_stream->send_body_len -
                                  user_stream->send_offset,
                                  1);
            if (ret < 0) {
                LOGE("client send hq body error ret=%zd", ret);
                return 0;
            } else {
                user_stream->send_offset += ret;
                LOGD("client send hq body success size=%zd", ret);
                if (user_stream->send_offset >= user_stream->send_body_len) {
                    user_stream->hdr_sent = 1;
                }
            }
        }
    }
    return ret;
}

ssize_t client_send_hq_requests(xqc_cli_user_conn_t *user_conn,
                            xqc_cli_user_stream_t *user_stream, xqc_cli_request_t *req) {

    user_stream->hq_request = xqc_stream_create(user_conn->ctx->engine, &user_conn->cid,
                                                user_stream);
    if (user_stream->hq_request == NULL) {
        LOGE("xqc hq request create error");
        return -1;
    }
    return client_send_hq_content(user_stream);
}