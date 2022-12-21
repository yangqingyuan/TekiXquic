//
// Created by yqy on 2022/6/27.
//

#include "xquic_hq_ctrl.h"

ssize_t client_send_hq_content(xqc_cli_user_stream_t *user_stream, int finish_flag) {
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
                                  finish_flag);
            if (ret == -XQC_EAGAIN) {
                return 0;
            } else if (ret < 0) {
                LOGW("client send hq body error ret=%zd,finish_flag=%d", ret, finish_flag);
                return -1;
            } else {
                user_stream->send_offset += ret;
                LOGD("client send hq body success size=%zd,finish_flag=%d", ret, finish_flag);
                if (user_stream->send_offset >= user_stream->send_body_len) {
                    user_stream->hdr_sent = 1;
                }
            }
        }
    }

    /*if reuse stream,reset data */
    if (!finish_flag) {
        user_stream->hdr_sent = 0;
        user_stream->send_offset = 0;
    }
    return 0;
}

ssize_t client_send_hq_requests(xqc_cli_user_conn_t *user_conn,
                                xqc_cli_user_stream_t *user_stream, xqc_cli_request_t *req) {

    if (user_conn->ctx->args->req_cfg.finish_flag) {
        /* if not reuse stream create every time */
        user_stream->hq_request = xqc_stream_create(user_conn->ctx->engine, &user_conn->cid,
                                                    user_stream);
    } else {
        /* if reuse stream create stream when not create */
        if (!user_stream->hq_request || user_stream->hq_request == NULL) {
            user_stream->hq_request = xqc_stream_create(user_conn->ctx->engine, &user_conn->cid,
                                                        user_stream);
        }
    }

    if (user_stream->hq_request == NULL) {
        LOGE("xqc hq request create error");
        return -1;
    }
    return client_send_hq_content(user_stream, user_conn->ctx->args->req_cfg.finish_flag);
}

ssize_t client_send_hq_ping(xqc_cli_user_conn_t *user_conn, char *ping_user_data) {
    xqc_conn_send_ping(user_conn->ctx->engine, &user_conn->cid, ping_user_data);
    return 0;
}