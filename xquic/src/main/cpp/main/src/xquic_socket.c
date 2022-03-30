#include "xquic_socket.h"

/**
 * 解析服务器地址
 * @param cfg
 * @return
 */
void client_parse_server_addr(xqc_cli_net_config_t *cfg,const char *url) {

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
        LOGI("get addr info from hostname:%s\n", gai_strerror(rv));
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

    LOGI("server[%s] addr:%s:%d", cfg->host, cfg->server_addr, cfg->server_port);
    freeaddrinfo(result);
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

    struct sockaddr *addr = (struct sockaddr*)&cfg->addr;
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

    user_conn->last_sock_op_time = xqc_now();

    return fd;

    err:
    close(fd);
    return -1;
}











///*
//static inline uint64_t
//now()
//{
//    /* get microsecond unit time */
//    struct timeval tv;
//    gettimeofday(&tv, NULL);
//    uint64_t ul = tv.tv_sec * (uint64_t)1000000 + tv.tv_usec;
//    return  ul;
//}
//


//
//static int
//xqc_client_create_socket(int type,
//    const struct sockaddr *saddr, socklen_t saddr_len)
//{
//    int size;
//    int fd = -1;
//
//    /* create fd & set socket option */
//    fd = socket(type, SOCK_DGRAM, 0);
//    if (fd < 0) {
//        LOGE("create socket failed, errno: %d\n", errno);
//        return -1;
//    }
//
//    if (fcntl(fd, F_SETFL, O_NONBLOCK) == -1) {
//        LOGE("set socket nonblock failed, errno: %d\n", errno);
//        goto err;
//    }
//
//    size = 1 * 1024 * 1024;
//    if (setsockopt(fd, SOL_SOCKET, SO_RCVBUF, &size, sizeof(int)) < 0) {
//        LOGE("setsockopt failed, errno: %d\n", errno);
//        goto err;
//    }
//
//    if (setsockopt(fd, SOL_SOCKET, SO_SNDBUF, &size, sizeof(int)) < 0) {
//        LOGE("setsockopt failed, errno: %d\n", errno);
//        goto err;
//    }
//
//    /* connect to peer addr */
//#if !defined(__APPLE__)
//
//    if (connect(fd, (struct sockaddr *)saddr, saddr_len) < 0) {
//        LOGE("connect socket failed, errno: %d\n", errno);
//        goto err;
//    }
//#endif
//
//    return fd;
//
//err:
//    close(fd);
//    return -1;
//}
//
//
//

//
//
//
//static uint64_t last_recv_ts = 0; // FIXME 待完善
//
//void xqc_client_socket_read_handler(client_ctx_t* ctx)
//{
//    DEBUG;
//    ssize_t recv_size = 0;
//    ssize_t recv_sum = 0;
//
//    user_conn_t *user_conn = ctx->user_conn;
//
//    unsigned char packet_buf[XQC_PACKET_TMP_BUF_LEN];
//
//    static ssize_t last_rcv_sum = 0;
//    static ssize_t rcv_sum = 0;
//
//    do {
//            recv_size = recvfrom(user_conn->fd,
//                                 packet_buf, sizeof(packet_buf), 0,
//                                 user_conn->peer_addr, &user_conn->peer_addrlen);
//            if (recv_size < 0 && errno == EAGAIN) {
//                break;
//            }
//
//            if (recv_size < 0) {
//                LOGI("recvfrom: recvmsg = %zd(%s)\n", recv_size, strerror(errno));
//                break;
//            }
//
//            /* if recv_size is 0, break while loop, */
//            if (recv_size == 0) {
//                break;
//            }
//
//            recv_sum += recv_size;
//            rcv_sum += recv_size;
//
//            if (user_conn->get_local_addr == 0) {
//                user_conn->get_local_addr = 1;
//                socklen_t tmp = sizeof(struct sockaddr_in6);
//                int ret = getsockname(user_conn->fd, (struct sockaddr *) user_conn->local_addr, &tmp);
//                if (ret < 0) {
//                    LOGI("getsockname error, errno: %d\n", errno);
//                    break;
//                }
//                user_conn->local_addrlen = tmp;
//            }
//
//            uint64_t recv_time = now();
//
//            static char copy[XQC_PACKET_TMP_BUF_LEN];
//
//            if (xqc_engine_packet_process(ctx->engine, packet_buf, recv_size,
//                                          user_conn->local_addr, user_conn->local_addrlen,
//                                          user_conn->peer_addr, user_conn->peer_addrlen,
//                                          (xqc_msec_t)recv_time, user_conn) != XQC_OK)
//            {
//                LOGI("xqc_client_read_handler: packet process err\n");
//                return;
//            }
//
//        } while (recv_size > 0);
//
//
//        if ((now() - last_recv_ts) > 200000) {
//            LOGI("recving rate: %.3lf Kbps\n", (rcv_sum - last_rcv_sum) * 8.0 * 1000 / (now() - last_recv_ts));
//            last_recv_ts = now();
//            last_rcv_sum = rcv_sum;
//        }
//
//    finish_recv:
//        LOGI("recvfrom size:%zu\n", recv_sum);
//        xqc_engine_finish_recv(ctx->engine);
//}
//
//void
//xqc_client_socket_event_callback(struct ev_loop *main_loop,ev_io*io_w,int what)
//{
//    //DEBUG;
//    client_ctx_t* ctx = (client_ctx_t *) io_w->data;
//
//    if (what & EV_READ) {
//        xqc_client_socket_read_handler(ctx);
//    } else {
//        LOGE("event callback: what=%d\n", what);
//    }
//}
//
//void xqc_client_socket_write_handler(user_conn_t *user_conn){
//    DEBUG;
//
//}
//
//void xqc_client_timeout_callback(struct ev_loop *main_loop,ev_timer*time_w,int what){
//    //DEBUG;
//
//}
//
//user_conn_t *
//xqc_client_user_conn_create(client_ctx_t* ctx,const char *server_addr, int server_port)
//{
//    user_conn_t *user_conn = calloc(1, sizeof(user_conn_t));
//
//    user_conn->ev_timeout.data = user_conn;
//    ev_timer_init(&user_conn->ev_timeout, xqc_client_timeout_callback, -1,0);//一秒后超时
//    ev_timer_start(loop,&user_conn->ev_timeout);
//
//    int ip_type = AF_INET; //(g_ipv6 ? AF_INET6 : AF_INET);
//    xqc_client_init_addr(user_conn, server_addr, server_port,ip_type);
//
//    user_conn->fd = xqc_client_create_socket(ip_type,
//                                             user_conn->peer_addr, user_conn->peer_addrlen);
//    if (user_conn->fd < 0) {
//        LOGE("xqc_create_socket error server_addr=%s ,server_port=%d\n",server_addr,server_port);
//        ev_timer_stop(loop,&user_conn->ev_timeout);
//        return NULL;
//    }
//
//    user_conn->ev_socket.data = ctx;
//    ev_io_init(&user_conn->ev_socket,xqc_client_socket_event_callback,user_conn->fd,EV_READ);
//    ev_io_start(loop,&user_conn->ev_socket);
//
//    return user_conn;
//}
