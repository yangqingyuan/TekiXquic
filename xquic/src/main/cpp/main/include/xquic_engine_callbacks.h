//
// Created by yqy on 2022/3/23.
//

#include "common.h"
#include "xquic_common.h"

#ifndef _Hncluded_XQUIC_ENGINE_CALLBACKS
#define _Hncluded_XQUIC_ENGINE_CALLBACKS

#ifdef __cplusplus

extern "C"{
#endif

void client_keylog_cb(const char *line, void *user_data);

void client_write_log(xqc_log_level_t lvl, const void *buf, size_t count, void *engine_user_data);

void client_set_event_timer(xqc_msec_t wake_after, void *user_data);

#ifdef __cplusplus
}
#endif
#endif