#include "transport_callbacks.h"

ssize_t xqc_client_write_socket(const unsigned char *buf, size_t size,
    const struct sockaddr *peer_addr, socklen_t peer_addrlen, void *user)
{
    DEBUG;


    user_conn_t *user_conn = (user_conn_t *) user;
    ssize_t res = 0;
    int fd = user_conn->fd;
    /* COPY to run corruption test cases */
    unsigned char send_buf[XQC_PACKET_TMP_BUF_LEN];
    size_t send_buf_size = 0;

    if (size > XQC_PACKET_TMP_BUF_LEN) {
        LOGE("xqc_client_write_socket err: size=%zu is too long\n", size);
        return XQC_SOCKET_ERROR;
    }
    send_buf_size = size;
    memcpy(send_buf, buf, send_buf_size);

    do {
        errno = 0;
        res = sendto(fd, send_buf, send_buf_size, 0, peer_addr, peer_addrlen);
        if (res < 0) {
            LOGE("xqc_client_write_socket err %zd %s\n", res, strerror(errno));
            if (errno == EAGAIN) {
                res = XQC_SOCKET_EAGAIN;
            }
        }
    } while ((res < 0) && (errno == EINTR));

    return res;
}

void xqc_client_save_token(const unsigned char *token, unsigned token_len, void *user_data)
{
    DEBUG;
}

void save_session_cb(const char * data, size_t data_len, void *user_data)
{
    DEBUG;
}

void save_tp_cb(const char * data, size_t data_len, void * user_data)
{
    DEBUG;
}

int xqc_client_cert_verify(const unsigned char *certs[],
    const size_t cert_len[], size_t certs_len, void *conn_user_data)
{
    /* self-signed cert used in test cases, return >= 0 means success */
    DEBUG;
    return 0;
}