#include "xquic_engine_callbacks.h"


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
    //DEBUG;
    client_ctx_t *ctx = (client_ctx_t *) user_data;
    LOGE("xqc_client_set_event_timer wake_after:%f",wake_after/1000000.0);

    ev_tstamp time = wake_after/1000000.0;
    if(time>=1){
        LOGE("xqc_client_set_event_timer ev_timer_again:%f",time);
        ctx->ev_engine.repeat =time; //单位秒
        ev_timer_again (loop, &ctx->ev_engine);//重新设置重复时间，每次调用会覆盖之前的时间，时间开始时间为当前时间
    }
}
