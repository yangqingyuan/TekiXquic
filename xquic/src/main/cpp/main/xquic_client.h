#include "ev.h"
#include "event.h"
#include "xquic.h"

#ifndef _Hncluded_XQUIC_CLIENT
#define _Hncluded_XQUIC_CLIENT

#ifdef __cplusplus

extern "C"{
#endif

#define XQC_MAX_LOG_LEN 2048
#define DEBUG LOGI("fun:%s,line %d \n", __FUNCTION__, __LINE__);



typedef struct client_ctx_s {
    xqc_engine_t   *engine;
    struct event   *ev_engine;
    int             log_fd;
    int             keylog_fd;
    struct event   *ev_delay;
} client_ctx_t;


long initEngine();
int startEngine();
int destroyEngine();

#ifdef __cplusplus
}
#endif
#endif