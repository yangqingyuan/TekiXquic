#include "xquic_socket.h"

/**
 * 解析服务器地址
 * @param cfg
 * @return
 */
int client_parse_server_addr(xqc_cli_net_config_t *cfg, const char *url,
                             xqc_cli_user_data_params_t *user_callback) {

    /* get hostname and port */
    char s_port[16] = {0};
    sscanf(url, "%*[^://]://%[^:]:%[^/]", cfg->host, s_port);

    /* parse port */
    cfg->server_port = atoi(s_port);

    /* set hit for hostname resolve */
    struct addrinfo hints = {0};
    memset(&hints, 0, sizeof(struct addrinfo));
    hints.ai_family = AF_UNSPEC;        /* allow IPv4 or IPv6 */
    hints.ai_socktype = SOCK_DGRAM;     /* datagram socket */
    hints.ai_flags = AI_PASSIVE;        /* For wildcard IP address */
    hints.ai_protocol = 0;              /* Any protocol */
    hints.ai_canonname = NULL;
    hints.ai_addr = NULL;
    hints.ai_next = NULL;

    /* resolve server's ip from hostname */
    struct addrinfo *result = NULL;
    int rv = getaddrinfo(cfg->host, s_port, &hints, &result);
    if (rv != 0) {
        char err_msg[1024];
        sprintf(err_msg, "get addr info from hostname:%s, url:%s", gai_strerror(rv), url);
        LOGE("%s\n", err_msg);
        callback_data_to_client_2(user_callback, XQC_ERROR, err_msg);
        return -1;
    }
    memcpy(&cfg->addr, result->ai_addr, result->ai_addrlen);
    cfg->addr_len = result->ai_addrlen;

    /* convert to string */
    if (result->ai_family == AF_INET6) {
        inet_ntop(result->ai_family, &(((struct sockaddr_in6 *) result->ai_addr)->sin6_addr),
                  cfg->server_addr, sizeof(cfg->server_addr));
    } else {
        inet_ntop(result->ai_family, &(((struct sockaddr_in *) result->ai_addr)->sin_addr),
                  cfg->server_addr, sizeof(cfg->server_addr));
    }

    LOGI("client parse server addr server[%s] addr:%s:%d", cfg->host, cfg->server_addr,
         cfg->server_port);
    freeaddrinfo(result);
    return 0;
}

/**
 * 创建socket 链接
 * @param user_conn
 * @param cfg
 * @return
 */
int client_create_socket(xqc_cli_user_conn_t *user_conn, xqc_cli_net_config_t *cfg) {
    DEBUG;
    int size = 0;
    int fd = 0;
    int ret;

    struct sockaddr *addr = (struct sockaddr *) &cfg->addr;
    fd = socket(addr->sa_family, SOCK_DGRAM, 0);
    if (fd < 0) {
        LOGE("create socket failed,erron:%d", errno);
        return -1;
    }

    if (fcntl(fd, F_SETFL, O_NONBLOCK) == -1) {
        LOGE("set socket nonblock failed,errno:%d", errno);
        goto err;
    }

    size = 1 * 1024 * 1024;
    if (setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, sizeof(int)) < 0) {
        LOGE("setsockopt failed,errno:%d", errno);
        goto err;
    }

    if (setsockopt(fd, SOL_SOCKET, SO_SNDBUF, &size, sizeof(int)) < 0) {
        LOGE("setsockopt failed,errno:%d", errno);
        goto err;
    }

    return fd;

    err:
    close(fd);
    return -1;
}

/**
 * socket读取数据
 * @param user_conn
 */
void client_socket_read_handler(xqc_cli_user_conn_t *user_conn) {
    //DEBUG;
    ssize_t recv_size = 0;
    ssize_t recv_sum = 0;

    struct sockaddr addr;
    socklen_t addr_len = 0;
    unsigned char packet_buf[XQC_PACKET_TMP_BUF_LEN];
    do {
        recv_size = recvfrom(user_conn->fd, packet_buf, sizeof(packet_buf), 0, (
                struct sockaddr *) &addr, &addr_len);
        if (recv_size < 0 && errno == EAGAIN) {
            //LOGE("recvfrom error recv_size=%d and errno == EAGAIN",recv_size);
            break;
        }

        if (recv_size <= 0) {
            LOGE("recvfrom error recv_size=%zd", recv_size);
            break;
        }

        user_conn->local_addrlen = sizeof(struct sockaddr_in6);
        xqc_int_t ret = getsockname(user_conn->fd, (struct sockaddr *) &user_conn->local_addr,
                                    &user_conn->local_addrlen);
        if (ret != 0) {
            LOGE("get_socket_name error,error:%d", errno);
        }

        recv_sum += recv_size;
        uint64_t recv_time = xqc_now();
        //user_conn->last_sock_read_time = recv_time;
        ret = xqc_engine_packet_process(user_conn->ctx->engine, packet_buf, recv_size,
                                        (struct sockaddr *) (&user_conn->local_addr),
                                        user_conn->local_addrlen, (struct sockaddr *) (&addr),
                                        addr_len, (xqc_msec_t) recv_time, user_conn);
        if (ret != XQC_OK) {
            char err_msg[214];
            sprintf(err_msg, "xqc_engine_packet_process error (%d),tag(%s)",
                    user_conn->ctx->args->user_stream.user_tag);
            LOGE("%s", err_msg);
            callback_data_to_client(user_conn, ret, err_msg, strlen(err_msg),
                                    user_conn->ctx->args->user_stream.user_tag);
            return;
        }

    } while (recv_size > 0);

    xqc_engine_finish_recv(user_conn->ctx->engine);
}

/**
 * socket 写
 * @param user_conn
 */
void client_socket_write_handler(xqc_cli_user_conn_t *user_conn) {
    DEBUG;
}
