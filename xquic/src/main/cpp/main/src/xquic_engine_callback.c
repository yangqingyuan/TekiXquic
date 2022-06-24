#include "xquic_engine_callbacks.h"


void client_keylog_cb(const char *line, void *user_data) {
    DEBUG;
}

void client_write_log(xqc_log_level_t lvl, const void *buf, size_t count, void *engine_user_data) {
    //DEBUG;
    switch (lvl) {
        case XQC_LOG_REPORT:
            break;
        case XQC_LOG_FATAL:
            break;
        case XQC_LOG_ERROR:
            LOGE("log:%s\n", buf);
            break;
        case XQC_LOG_WARN:
            //LOGW("log:%s\n", buf);
            break;
        case XQC_LOG_INFO:
        case XQC_LOG_STATS:
            //LOGI("log:%s\n", buf);
            break;
        case XQC_LOG_DEBUG:
            //LOGD("log:%s\n", buf);
            break;
    }
}

void client_set_event_timer(xqc_msec_t wake_after, void *user_data) {
    //DEBUG;
    xqc_cli_ctx_t *ctx = (xqc_cli_ctx_t *) user_data;

    ctx->ev_engine.repeat = wake_after / 1000000.0;
    //LOGE("client_set_event_timer wake_after:%f", ctx->ev_engine.repeat);
    ev_timer_again(ctx->eb, &ctx->ev_engine);
}
