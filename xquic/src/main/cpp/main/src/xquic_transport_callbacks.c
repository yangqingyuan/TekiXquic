#include "xquic_transport_callbacks.h"

ssize_t client_write_socket(const unsigned char *buf, size_t size,
                            const struct sockaddr *peer_addr, socklen_t peer_addrlen, void *user) {
    //DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user;
    ssize_t res = 0;
    do {
        errno = 0;
        res = sendto(user_conn->fd, buf, size, 0, peer_addr, peer_addrlen);
        if (res < 0) {
            LOGE("write socket err %zd %s ,fd:%d, buf:%p, size:%zu, server_add:%s \n", res,
                 strerror(errno), user_conn->fd, buf, size,
                 user_conn->ctx->args->net_cfg.server_addr);
            if (errno == EAGAIN) {
                res = XQC_SOCKET_EAGAIN;
            }
        }

        user_conn->last_sock_write_time = xqc_now();

    } while ((res < 0) && (errno == EINTR));
    return res;
}

void client_save_token(const unsigned char *token, unsigned token_len, void *user_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    if (!user_conn) {
        LOGE("save token error,user_conn is NULL");
        return;
    }
    callback_msg_to_client(user_conn->ctx->args, MSG_TYPE_TOKEN, (char *)token, token_len);
}

void client_save_session_cb(const char *data, size_t data_len, void *user_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    if (!user_conn) {
        LOGE("save session error,user_conn is NULL");
        return;
    }
    callback_msg_to_client(user_conn->ctx->args, MSG_TYPE_SESSION, data, data_len);
}

void client_save_tp_cb(const char *data, size_t data_len, void *user_data) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) user_data;
    if (!user_conn) {
        LOGE("save tp cb error,user_conn is NULL");
        return;
    }
    callback_msg_to_client(user_conn->ctx->args, MSG_TYPE_TP, data, data_len);
}

int client_cert_verify_cb(const unsigned char **certs,
                          const size_t *cert_len, size_t certs_len, void *conn_user_data) {
    /* self-signed cert used in test cases, return >= 0 means success */
    DEBUG;
    return 0;
}

void client_conn_update_cid_notify(xqc_connection_t *conn, const xqc_cid_t *retire_cid,
                                   const xqc_cid_t *new_cid, void *user_data) {
    DEBUG;
}