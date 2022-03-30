//
// Created by lizhi on 2022/3/28.
//
#include "xquic_common.h"
#include "xquic_client_short.h"
#include "xquic_engine_callbacks.h"
#include "xquic_transport_callbacks.h"
#include "xquic_socket.h"
#include "xquic_h3_callbacks.h"
#include "xquic_h3_ctrl.h"

/**
 * 打开log文件
 * @param ctx
 * @return
 */
int client_open_log_file(xqc_cli_ctx_t *ctx) {
    ctx->log_fd = open(ctx->log_path, (O_WRONLY | O_APPEND | O_CREAT), 0644);
    if (ctx->log_fd <= 0) {
        return XQC_ERROR;
    }
    return XQC_OK;
}

/**
 * 关闭log文件
 * @param ctx
 */
int client_close_log_file(xqc_cli_ctx_t *ctx) {
    if (ctx->log_fd <= 0) {
        return XQC_ERROR;
    }

    close(ctx->log_fd);
    ctx->log_fd = 0;
    return XQC_OK;
}

/**
 * 打开关键log文件
 * @param ctx
 */
int client_open_keylog_file(xqc_cli_ctx_t *ctx) {
    ctx->keylog_fd = open(ctx->args->env_cfg.key_out_path, (O_WRONLY | O_APPEND | O_CREAT), 0644);
    if (ctx->keylog_fd <= 0) {
        return XQC_ERROR;
    }
    return XQC_OK;
}

/**
 * 关闭log文件
 * @param ctx
 */
int client_close_keylog_file(xqc_cli_ctx_t *ctx) {
    if (ctx->keylog_fd <= 0) {
        return XQC_ERROR;
    }

    close(ctx->keylog_fd);
    ctx->keylog_fd = 0;
    return XQC_OK;
}


/**
 * 初始化ctx
 * @param ctx
 * @param args
 */
void client_init_ctx(xqc_cli_ctx_t *pctx, xqc_cli_client_args_t *args) {
    DEBUG;
    strncpy(pctx->log_path, args->env_cfg.log_path, sizeof(pctx->log_path) - 1);
    pctx->args = args;
    client_open_log_file(pctx);
    client_open_keylog_file(pctx);
}

/**
 * 客户端引擎回调
 * @param main_loop
 * @param io_w
 * @param what
 */
void client_engine_callback(struct ev_loop *main_loop, ev_io *io_w, int what) {
    //DEBUG;
    xqc_cli_ctx_t *ctx = (xqc_cli_ctx_t *) io_w->data;
    xqc_engine_main_logic(ctx->engine);
}

/**
 * 引擎初始化SSL配置
 */
void client_init_engine_ssl_config(xqc_engine_ssl_config_t *cfg, xqc_cli_client_args_t *args) {
    memset(cfg, 0, sizeof(xqc_engine_ssl_config_t));
    if (args->quic_cfg.cipher_suites) {
        cfg->ciphers = args->quic_cfg.cipher_suites;
    } else {
        cfg->ciphers = XQC_TLS_CIPHERS;
    }
    cfg->groups = XQC_INTEROP_TLS_GROUPS;
}

/**
 * 初始化引擎回调
 * @param cb
 * @param transport_cbs
 * @param arg
 */
void
client_init_engine_callback(xqc_engine_callback_t *cb, xqc_transport_callbacks_t *transport_cbs,
                            xqc_cli_client_args_t *arg) {
    static xqc_engine_callback_t callback = {
            .log_callbacks = {
                    .xqc_log_write_err = xqc_client_write_log,
                    .xqc_log_write_stat = xqc_client_write_log
            },
            .keylog_cb = xqc_keylog_cb,
            .set_event_timer =xqc_client_set_event_timer
    };

    static xqc_transport_callbacks_t tcb = {
            .write_socket = write_socket,
            .save_token = save_token,
            .save_session_cb = save_session_cb,
            .save_tp_cb = save_tp_cb,
            .cert_verify_cb = cert_verify_cb,
            .conn_update_cid_notify = conn_update_cid_notify
    };

    *cb = callback;
    *transport_cbs = tcb;
}

/**
 * 初始化协议
 * 注意：这里只初始化H3，hq不做过多的逻辑
 * @param ctx
 * @return
 */
int client_init_alpn(xqc_cli_ctx_t *ctx) {
    xqc_h3_callbacks_t h3_cbs = {
            .h3c_cbs={
                    .h3_conn_create_notify = xqc_client_h3_conn_create_notify,
                    .h3_conn_close_notify = xqc_client_h3_conn_close_notify,
                    .h3_conn_handshake_finished = xqc_client_h3_conn_handshake_finished
            },
            .h3r_cbs={
                    .h3_request_create_notify = xqc_client_request_create_notify,
                    .h3_request_close_notify = xqc_client_request_close_notify,
                    .h3_request_read_notify = xqc_client_request_read_notify,
                    .h3_request_write_notify = xqc_client_request_write_notify
            }
    };

    int ret = xqc_h3_ctx_init(ctx->engine, &h3_cbs);
    if (ret != XQC_OK) {
        LOGE("init h3 context error, ret:%d", ret);
        return XQC_ERROR;
    }
    LOGI("client init alpn success");
    return XQC_OK;
}

/**
 * 初始化引擎
 * @param ctx
 * @param args
 * @return
 */
int client_init_engine(xqc_cli_ctx_t *ctx, xqc_cli_client_args_t *args) {
    DEBUG;
    /* init engine ssl config*/
    xqc_engine_ssl_config_t engine_ssl_config;
    client_init_engine_ssl_config(&engine_ssl_config, args);

    /* init engine callback*/
    xqc_transport_callbacks_t transport_cbs;
    xqc_engine_callback_t callback;
    client_init_engine_callback(&callback, &transport_cbs, args);

    xqc_config_t config;
    if (xqc_engine_get_default_config(&config, XQC_ENGINE_CLIENT) < 0) {
        return XQC_ERROR;
    }
    switch (args->env_cfg.log_level) {
        case 'd':
            config.cfg_log_level = XQC_LOG_DEBUG;
            break;
        case 'i':
            config.cfg_log_level = XQC_LOG_INFO;
            break;
        case 'w':
            config.cfg_log_level = XQC_LOG_WARN;
            break;
        case 'e':
            config.cfg_log_level = XQC_LOG_ERROR;
            break;
        default:
            config.cfg_log_level = XQC_LOG_DEBUG;
            break;
    }

    ctx->engine = xqc_engine_create(XQC_ENGINE_CLIENT, &config,
                                    &engine_ssl_config, &callback, &transport_cbs, ctx);

    if (ctx->engine == NULL) {
        LOGE("xqc_engine_create error");
        return XQC_ERROR;
    }

    /* init alpn (初始化协议)*/
    if (client_init_alpn(ctx) < 0) {
        LOGE("init alpn error");
        return XQC_ERROR;
    }

    return XQC_OK;
}


/**
 * 是否资源
 * @param ctx
 */
void client_free_ctx(xqc_cli_ctx_t *ctx) {
    client_close_keylog_file(ctx);
    client_close_log_file(ctx);
    if (ctx->args) {
        free(ctx->args);
        ctx->args = NULL;
    }
    free(ctx);
}


/**
 * 初始化 单链接多流发送多请求
 * @param ctx
 * @param args
 */
void client_init_tasks_scmr(xqc_cli_task_ctx_t *tctx, xqc_cli_client_args_t *args) {
    tctx->task_cnt = 1;/* one task, one connection, all requests */

    /* init task list*/
    tctx->tasks = calloc(1, sizeof(xqc_cli_task_t) * 1);

    /*请求总数*/
    tctx->tasks->req_cnt = args->req_cfg.request_cnt;
    tctx->tasks->reqs = args->req_cfg.reqs;

    /*init schedule 初始化调度器*/
    tctx->schedule.schedule_info = calloc(1, sizeof(xqc_cli_task_schedule_info_t) * 1);
}

/**
 * 初始化串/并行请求
 * @param ctx
 * @param args
 */
void client_init_tasks_scsr(xqc_cli_task_ctx_t *tctx, xqc_cli_client_args_t *args) {
    tctx->task_cnt = args->req_cfg.request_cnt;

    /*init task list */
    tctx->tasks = calloc(1, sizeof(xqc_cli_task_t) * tctx->task_cnt);
    for (int i = 0; i < tctx->task_cnt; i++) {
        tctx->tasks[i].task_idx = i;
        tctx->tasks[i].req_cnt = 1;
        tctx->tasks[i].reqs = (xqc_cli_request_t *) args->req_cfg.reqs + i;
    }

    /*init schedule 初始化调度器*/
    tctx->schedule.schedule_info = calloc(1, sizeof(xqc_cli_task_schedule_info_t) * tctx->task_cnt);
}

/**
 * 初始化任务
 * @param ctx
 */
void client_init_tasks(xqc_cli_ctx_t *ctx) {

    /*任务模式*/
    ctx->task_ctx.mode = ctx->args->net_cfg.mode;
    switch (ctx->args->net_cfg.mode) {
        case MODE_SCMR://单链接多流发送多请求
            client_init_tasks_scmr(&ctx->task_ctx, ctx->args);
            break;
        case MODE_SCSR_SERIAL://串行：一个请求一个链接
        case MODE_SCSR_CONCURRENT://并行：一个请求一个链接
            client_init_tasks_scsr(&ctx->task_ctx, ctx->args);
            break;
        default:
            LOGE("init tasks error,unKnow mode");
            break;
    }
}


/**
 * 关闭任务
 * @param ctx
 * @param task
 */
int client_close_task(xqc_cli_ctx_t *ctx, xqc_cli_task_t *task) {
    DEBUG;
    xqc_cli_user_conn_t *user_conn = task->user_conn;

    /*close xquic conn*/
    if (ctx->args->quic_cfg.alpn_type == ALPN_H3) {
        xqc_h3_conn_close(ctx->engine, &user_conn->cid);
    } else {
        LOGE("client close task error: unKnow alpn type:%d", ctx->args->quic_cfg.alpn_type);
    }

    /* remove task event handle */
    ev_io_stop(ctx->eb, &user_conn->ev_socket);
    ev_timer_stop(ctx->eb, &user_conn->ev_timeout);

    /* close socket */
    close(user_conn->fd);

    return 0;
}

/**
 * socket 事件回调
 * @param main_loop
 * @param io_w
 * @param what
 */
void client_socket_event_callback(struct ev_loop *main_loop, ev_io *io_w, int what) {
    //DEBUG;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) io_w->data;
    if (what & EV_READ) {
        client_socket_read_handler(user_conn);
    } else if (what & EV_WRITE) {
        client_socket_write_handler(user_conn);
    } else {
        LOGE("socket event callback error,unKnow what:%d", what);
    }
}


/**
 * socket 超时后关闭流
 * @param main_loop
 * @param io_t
 * @param what
 */
void client_idle_callback(struct ev_loop *main_loop, ev_timer *io_t, int what) {
    DEBUG;
    int rc = 0;
    xqc_cli_user_conn_t *user_conn = (xqc_cli_user_conn_t *) io_t->data;
    if (user_conn->ctx->args->quic_cfg.alpn_type == ALPN_H3) {
        rc = xqc_h3_conn_close(user_conn->ctx->engine, &user_conn->cid);
    } else {
        LOGE("不支持其他协议");
    }

    if (rc != XQC_OK) {
        LOGE("client idle callback,close conn error");
        return;
    }

    LOGI("socket idle timeout, task failed, total task_cnt: %d, req_fin_cnt: %d, req_sent_cnt: %d, req_create_cnt: %d\n",
         user_conn->ctx->task_ctx.tasks[user_conn->task->task_idx].req_cnt,
         user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_fin_cnt,
         user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_sent_cnt,
         user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_create_cnt);

    //修改为失败状态
    user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].status = TASK_STATUS_FAILED;

    LOGI("task failed, total task_req_cnt: %d, req_fin_cnt: %d, req_sent_cnt: %d, "
         "req_create_cnt: %d\n", user_conn->task->req_cnt,
         user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_fin_cnt,
         user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_sent_cnt,
         user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_create_cnt);
}

/**
 * 初始化0rtt
 *
 * @param args
 */
void client_init_0rtt(xqc_cli_client_args_t *args) {
    /* read session ticket */
    DEBUG;
}

/**
 * 初始化链接设置
 * @param args
 */
void client_init_connection_settings(xqc_conn_settings_t *settings, xqc_cli_client_args_t *args) {

    /* 拥塞控制*/
    xqc_cong_ctrl_callback_t cong_ctrl;
    switch (args->net_cfg.cc) {
        case CC_TYPE_BBR:
            cong_ctrl = xqc_bbr_cb;
            break;
        case CC_TYPE_CUBIC:
            cong_ctrl = xqc_cubic_cb;
            break;
        case CC_TYPE_RENO:
            cong_ctrl = xqc_reno_cb;
            break;
        default:
            break;
    }

    memset(settings, 0, sizeof(xqc_conn_settings_t));
    settings->pacing_on = args->net_cfg.pacing;
    settings->cong_ctrl_callback = cong_ctrl;//拥塞控制算法
    settings->cc_params.customize_on = 1;//是否打开自定义
    settings->cc_params.init_cwnd = 32;//拥塞窗口数
    settings->so_sndbuf = 1024 * 1024;//socket send  buf的大小
    settings->proto_version = XQC_VERSION_V1;
    settings->spurious_loss_detect_on = 1;//散列丢失检测
    settings->keyupdate_pkt_threshold = args->quic_cfg.keyupdate_pkt_threshold;//单个 1-rtt 密钥的数据包限制，0 表示无限制
}

/**
 * 初始化ssl配置
 * @param args
 */
void client_init_connection_ssl_config(xqc_conn_ssl_config_t *conn_ssl_config,
                                       xqc_cli_client_args_t *args) {
    memset(conn_ssl_config, 0, sizeof(xqc_conn_ssl_config_t));

    /*set session ticket and transport parameter args */
    if (args->quic_cfg.st_len < 0 || args->quic_cfg.tp_len < 0) {
        conn_ssl_config->session_ticket_data = NULL;
        conn_ssl_config->transport_parameter_data = NULL;
    } else {
        conn_ssl_config->session_ticket_data = args->quic_cfg.st;
        conn_ssl_config->session_ticket_len = args->quic_cfg.st_len;
        conn_ssl_config->transport_parameter_data = args->quic_cfg.tp;
        conn_ssl_config->transport_parameter_data_len = args->quic_cfg.tp_len;
    }

}

/**
 * 初始化链接
 * @return
 */
int client_init_connection(xqc_cli_user_conn_t *user_conn, xqc_cli_client_args_t *args) {
    DEBUG;
    client_init_0rtt(args);

    /* init connection settings */
    xqc_conn_settings_t conn_settings;
    client_init_connection_settings(&conn_settings, args);

    /* init config*/
    xqc_conn_ssl_config_t conn_ssl_config;
    client_init_connection_ssl_config(&conn_ssl_config, args);

    if (args->quic_cfg.alpn_type == ALPN_H3) {
        const xqc_cid_t *cid = xqc_h3_connect(user_conn->ctx->engine, &conn_settings,
                                              (const unsigned char *) args->quic_cfg.token,
                                              args->quic_cfg.token_len,
                                              args->net_cfg.host, 0, &conn_ssl_config,
                                              (struct sockaddr *) &args->net_cfg.addr,
                                              args->net_cfg.addr_len, user_conn);

        if (cid == NULL) {
            LOGE("xqc h3 connect error");
            return -1;
        }

        memcpy(&user_conn->cid, cid, sizeof(xqc_cid_t));
    } else {
        LOGE("只支持H3，暂时不支持其他类型");
        return -1;
    }
    return 0;
}


/**
 * 发送请求
 * @param user_conn
 * @param args
 * @param reqs
 * @param req_cnt
 */
void client_send_requests(xqc_cli_user_conn_t *user_conn, xqc_cli_client_args_t *args,
                          xqc_cli_request_t *reqs, int req_cnt) {
    DEBUG;

    /*send request */
    for (int i = 0; i < req_cnt; i++) {
        args->user_stream.user_conn = user_conn;

        if (args->quic_cfg.alpn_type == ALPN_H3) {
            if (client_send_h3_requests(user_conn, &args->user_stream, reqs + i) < 0) {
                LOGE("send h3 req blocked,will try later,total sent_cnt :%d",
                     user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_create_cnt);
                return;
            }
        } else {
            LOGE("支持者 h3发送");
        }
        user_conn->ctx->task_ctx.schedule.schedule_info[user_conn->task->task_idx].req_create_cnt++;
    }
}

/**
 * 开始真正的执行任务
 * @param user_conn
 * @param args
 * @param reqs
 * @param ree_cnt
 */
void client_task_start(xqc_cli_user_conn_t *user_conn, xqc_cli_client_args_t *args,
                       xqc_cli_request_t *reqs, int req_cnt) {
    DEBUG;
    if (XQC_OK != client_init_connection(user_conn, args)) {
        return;
    }

    /*发送请求 TODO : fix MAX_STREAM bug*/
    client_send_requests(user_conn, args, reqs, req_cnt);
}

/**
 * 处理任务
 * @param ctx
 * @param task
 * @return
 */
int client_handle_task(xqc_cli_ctx_t *ctx, xqc_cli_task_t *task) {
    //DEBUG;

    /* create socket and connection callback user data*/
    xqc_cli_user_conn_t *user_conn = calloc(1, sizeof(xqc_cli_user_conn_t));
    user_conn->ctx = ctx;
    user_conn->task = task;
    user_conn->fd = client_create_socket(user_conn, &ctx->args->net_cfg);
    if (user_conn->fd < 0) {
        LOGE("client_create_socket error\n");
        return -1;
    }

    /*socket event*/
    user_conn->ev_socket.data = user_conn;
    ev_io_init(&user_conn->ev_socket, client_socket_event_callback, user_conn->fd,
               EV_READ);
    ev_io_start(ctx->eb, &user_conn->ev_socket);

    /*xquic timer */
    user_conn->ev_timeout.data = user_conn;
    ev_timer_init(&user_conn->ev_timeout, client_idle_callback, ctx->args->net_cfg.conn_timeout, 0);
    ev_timer_start(ctx->eb, &user_conn->ev_timeout);

    /* start client */
    client_task_start(user_conn, ctx->args, task->reqs, task->req_cnt);

    task->user_conn = user_conn;

    return 0;
}

/**
 * 任务调度回调
 * @param main_loop
 * @param io_w
 * @param what
 */
void client_task_schedule_callback(struct ev_loop *main_loop, ev_async *io_w, int what) {
    //DEBUG;
    xqc_cli_ctx_t *ctx = (xqc_cli_ctx_t *) io_w->data;
    uint8_t all_task_fin_flag = 1;
    uint8_t idle_flag = 1;
    int idle_waiting_task_id = -1;

    for (int i = 0; i < ctx->task_ctx.task_cnt; i++) {
        /* if task finished,close task */
        if (ctx->task_ctx.schedule.schedule_info[i].fin_flag) {
            client_close_task(ctx, ctx->task_ctx.tasks + i);
            ctx->task_ctx.schedule.schedule_info[i].fin_flag = 0;
        }

        /* if task finished,close task */
        if (ctx->task_ctx.schedule.schedule_info[i].status <= TASK_STATUS_RUNNING) {
            all_task_fin_flag = 0;
        }

        /* record the first waiting task */
        if (idle_waiting_task_id == -1
            && ctx->task_ctx.schedule.schedule_info[i].status == TASK_STATUS_WAITTING) {
            idle_waiting_task_id = i;
        }
    }

    if (all_task_fin_flag) {
        LOGW("all tasks are finished,will break loop and exit!!");
        ev_break(main_loop,EVBREAK_ALL);
        return;
    }

    /* if dle and got a waiting task, run the task */
    if (idle_flag && idle_waiting_task_id >= 0) {
        /* handle task and set status to RUNNING */
        int ret = client_handle_task(ctx, ctx->task_ctx.tasks + idle_waiting_task_id);

        if (0 == ret) {
            ctx->task_ctx.schedule.schedule_info[idle_waiting_task_id].status = TASK_STATUS_RUNNING;
        } else {
            ctx->task_ctx.schedule.schedule_info[idle_waiting_task_id].status = TASK_STATUS_FAILED;
        }
    }

    /* start next round 开始下一轮检查*/
    ev_async_send(main_loop, io_w);
}

/**
 * 生命时间过后，无论结果如何直接kill掉
 * @param main_loop
 * @param io_w
 * @param what
 */
void client_kill_it_any_way_callback(struct ev_loop *main_loop, ev_timer *io_w, int what) {
    DEBUG;
    ev_break(main_loop, EVBREAK_ALL);
}

/**
 * 开始任务管理器
 * @param ctx
 */
void client_start_task_manager(xqc_cli_ctx_t *ctx) {
    DEBUG;
    /*init tasks */
    client_init_tasks(ctx);

    /*init add arm task timer 这里轮询检查是否任务状态*/
    ctx->ev_task.data = ctx;
    ev_async_init(&ctx->ev_task, client_task_schedule_callback);
    ev_async_start(ctx->eb, &ctx->ev_task);

    client_task_schedule_callback(ctx->eb, &ctx->ev_task, 0);

    /* kill it anyway, to protect from endless task (如果设置了生命时长，并超时了生命时长，直接kill掉)*/
    if (ctx->args->env_cfg.life > 0) {
        ctx->ev_kill.data = ctx;
        ev_timer_init(&ctx->ev_kill, client_kill_it_any_way_callback, ctx->args->env_cfg.life, 0);
        ev_timer_start(ctx->eb, &ctx->ev_kill);
    }
}


/**
 * 初始化参数
 * （1）网络配置
 * （2）环境配置
 * （3）quic配置
 */
void client_init_args(xqc_cli_client_args_t *args, const char *url) {
    DEBUG;
    memset(args, 0, sizeof(xqc_cli_client_args_t));

    /*网络配置*/
    args->net_cfg.conn_timeout = 30;
    args->net_cfg.mode = MODE_SCMR;
    client_parse_server_addr(&args->net_cfg, url);//根据url解析地址跟port

    args->req_cfg.request_cnt = 1;//一个url一个请求

    /*环境配置 */
    args->env_cfg.log_level = XQC_LOG_DEBUG;
    //strncpy(args->env_cfg.log_path,"xxxx",sizeof (args->env_cfg.log_path));
    //strncpy(args->env_cfg.out_file_dir,"xxxx",sizeof (args->env_cfg.out_file_dir));

    /*quic配置 */
    args->quic_cfg.alpn_type = ALPN_H3;
    strncpy(args->quic_cfg.alpn, "hq-interop", sizeof(args->quic_cfg.alpn));
    args->quic_cfg.keyupdate_pkt_threshold = UINT16_MAX;
}

/**
 * 解析参数
 * @param args
 * @param token
 * @param session
 * @param content
 */
void client_parse_args(xqc_cli_client_args_t *args, const char *token,
                       const char *session,
                       const char *content) {
    if (token != NULL) {
        int token_len = strlen(token);
        strcpy(args->quic_cfg.token, token);//拷贝token
        args->quic_cfg.token_len = token_len;
    }
    if (session != NULL) {
        int session_len = strlen(session);
        strcpy(args->quic_cfg.st, session);//拷贝session
        args->quic_cfg.st_len = session_len;
    }

    /* stream 配置 */
    if (content != NULL) {
        int content_len = strlen(content);
        args->user_stream.send_body = malloc(content_len);
        strcpy(args->user_stream.send_body, content);//拷贝发送的内容
    }
}

/**
 * 发送内容
 * @param host
 * @param port
 * @param token
 * @param session
 * @param content
 * @return
 */
int client_send(const char *url, const char *token, const char *session,
                const char *content) {

    /*get input client args */
    xqc_cli_client_args_t *args = calloc(1, sizeof(xqc_cli_client_args_t));
    client_init_args(args, url);
    client_parse_args(args, token, session, content);

    /*init client ctx*/
    xqc_cli_ctx_t *ctx = calloc(1, sizeof(xqc_cli_ctx_t));
    client_init_ctx(ctx, args);

    /*engine event*/
    ctx->eb = ev_loop_new(EVFLAG_AUTO);
    ctx->ev_engine.data = ctx;
    ev_io_init(&ctx->ev_engine, client_engine_callback, 0, EV_READ);//EV_READ=1,EV_WRITE=2
    ev_io_start(ctx->eb, &ctx->ev_engine);
    client_init_engine(ctx, args);

    /* start task scheduler */
    client_start_task_manager(ctx);
    ev_run(ctx->eb, 0);

    /* free ctx */
    xqc_engine_destroy(ctx->engine);
    client_free_ctx(ctx);

    LOGE("client send end(发送结束)");
    return XQC_OK;
}