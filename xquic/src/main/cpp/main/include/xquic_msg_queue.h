//
// Created by lizhi on 2022/6/27.
//


#include <stdio.h>
#include <memory.h>
#include <unistd.h>
#include <errno.h>
#include <signal.h>
#include <fcntl.h>
#include <stdlib.h>
#include <time.h>
#include <inttypes.h>
#include <netdb.h>
#include <string.h>
#include <cJSON.h>
#include <pthread.h>

#ifndef TEKIXQUIC_XQUIC_MSG_QUEUE_H
#define TEKIXQUIC_XQUIC_MSG_QUEUE_H

typedef struct xqc_cli_message_s {
    int what;
    int arg1;
    int arg2;
    void *obj;
    struct xqc_cli_message_t *next;

    void (*free_l)(void *obj);
} xqc_cli_message_t;

typedef struct xqc_cli_message_queue_s {
    xqc_cli_message_t *first_msg, *last_msg;
    int nb_messages;
    int abort_request;

    xqc_cli_message_t *recycle_msg;
    int recycle_count;
    int alloc_count;
} xqc_cli_message_queue_t;

inline static void msg_free_res(xqc_cli_message_t *msg) {
    if (!msg || !msg->obj)
        return;
    assert(msg->free_l);
    msg->free_l(msg->obj);
    msg->obj = NULL;
}


inline static int msg_queue_put_private(xqc_cli_message_queue_t *q, xqc_cli_message_t *msg) {
    xqc_cli_message_t *msg1;

    if (q->abort_request)
        return -1;
    msg1 = q->recycle_msg;
    if (msg1) {
        q->recycle_msg = msg1->next;
        q->recycle_count++;
    } else {
        q->alloc_count++;
        msg1 = av_malloc(sizeof(AVMessage));
    }
    if (!msg1)
        return -1;

    *msg1 = *msg;
    msg1->next = NULL;

    if (!q->last_msg)
        q->first_msg = msg1;
    else
        q->last_msg->next = msg1;
    q->last_msg = msg1;
    q->nb_messages++;
    return 0;
}


inline static int msg_queue_put(xqc_cli_message_queue_t *q, xqc_cli_message_t *msg) {
    int ret;
    ret = msg_queue_put_private(q, msg);
    return ret;
}

inline static void msg_queue_put_simple(xqc_cli_message_queue_t *q, int what, void *obj, int obj_len)
{
    xqc_cli_message_t msg;
    msg_init_msg(&msg);
    msg.what = what;
    msg.obj = malloc(obj_len);
    memcpy(msg.obj, obj, obj_len);
    msg.free_l = msg_obj_free_l;
    msg_queue_put(q, &msg);
}

inline static void msg_init_msg(xqc_cli_message_t *msg) {
    memset(msg, 0, sizeof(xqc_cli_message_t));
}

inline static void msg_obj_free_l(void *obj) {
    free(obj);
}

inline static void msg_queue_init(xqc_cli_message_queue_t *q) {
    memset(q, 0, sizeof(xqc_cli_message_queue_t));
    q->abort_request = 1;
}


inline static void msg_queue_flush(xqc_cli_message_queue_t *q) {
    xqc_cli_message_t *msg, *msg1;

    for (msg = q->first_msg; msg != NULL; msg = msg1) {
        msg1 = msg->next;
        msg->next = q->recycle_msg;
        q->recycle_msg = msg;
    }
    q->last_msg = NULL;
    q->first_msg = NULL;
    q->nb_messages = 0;
}


inline static void msg_queue_destroy(xqc_cli_message_queue_t *q) {
    msg_queue_flush(q);
    while (q->recycle_msg) {
        xqc_cli_message_t *msg = q->recycle_msg;
        if (msg)
            q->recycle_msg = msg->next;
        msg_free_res(msg);
        av_freep(&msg);
    }
}



inline static void msg_queue_abort(xqc_cli_message_queue_t *q) {
    q->abort_request = 1;
}


/* return < 0 if aborted, 0 if no msg and > 0 if msg.  */
inline static int msg_queue_get(xqc_cli_message_queue_t *q, xqc_cli_message_t *msg, int block) {
    xqc_cli_message_t *msg1;
    int ret;

    for (;;) {
        if (q->abort_request) {
            ret = -1;
            break;
        }
        msg1 = q->first_msg;
        if (msg1) {
            q->first_msg = msg1->next;
            if (!q->first_msg)
                q->last_msg = NULL;
            q->nb_messages--;
            *msg = *msg1;
            msg1->obj = NULL;
            msg1->next = q->recycle_msg;
            q->recycle_msg = msg1;
            ret = 1;
            break;
        } else if (!block) {
            ret = 0;
            break;
        }
    }
    return ret;
}

inline static void msg_queue_remove(xqc_cli_message_queue_t *q, int what) {
    xqc_cli_message_t **p_msg, *msg, *last_msg;
    last_msg = q->first_msg;

    if (!q->abort_request && q->first_msg) {
        p_msg = &q->first_msg;
        while (*p_msg) {
            msg = *p_msg;

            if (msg->what == what) {
                *p_msg = msg->next;
                msg_free_res(msg);
                msg->next = q->recycle_msg;
                q->recycle_msg = msg;
                q->nb_messages--;
            } else {
                last_msg = msg;
                p_msg = &msg->next;
            }
        }

        if (q->first_msg) {
            q->last_msg = last_msg;
        } else {
            q->last_msg = NULL;
        }
    }
}


#endif //TEKIXQUIC_XQUIC_MSG_QUEUE_H
