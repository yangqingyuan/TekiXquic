#include "engine_callbacks.h"


void xqc_keylog_cb(const char *line, void *user_data)
{
    DEBUG;
    client_ctx_t *ctx = (client_ctx_t*)user_data;
}

void xqc_client_write_log(xqc_log_level_t lvl, const void *buf, size_t count, void *engine_user_data)
{
    DEBUG;
    client_ctx_t *ctx = (client_ctx_t*)engine_user_data;
    switch(lvl){
        case XQC_LOG_REPORT:
        case XQC_LOG_FATAL:
        case XQC_LOG_ERROR:
            LOGE("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
        case XQC_LOG_WARN:
            LOGW("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
        case XQC_LOG_INFO:
        case XQC_LOG_STATS:
            LOGI("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
        case XQC_LOG_DEBUG:
            LOGD("fun:%s,line:%d,log:%s\n",__FUNCTION__,__LINE__,buf);
        break;
    }
}

void xqc_client_set_event_timer(xqc_msec_t wake_after, void *user_data)
{
    DEBUG;
    client_ctx_t *ctx = (client_ctx_t *) user_data;
    //printf("xqc_engine_wakeup_after %llu us, now %llu\n", wake_after, now());
    struct timeval tv;
    tv.tv_sec = wake_after / 1000000;
    tv.tv_usec = wake_after % 1000000;
    //event_add(ctx->ev_engine, &tv);

}
