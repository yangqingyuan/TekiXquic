//
// Created by lizhi on 2022/4/22.
//
#include "cQueue.h"


void queue_init(queue_t *pq) {
    assert(pq);
    pq->head = pq->tail = NULL;
    pq->size = 0;
}

int queue_push(queue_t *pq, queue_data_t x) {
    assert(pq);
    queue_node_t *newnode = (queue_node_t *) malloc(sizeof(queue_node_t));
    if (newnode == NULL) {
        printf("malloc fail\n");
        return -1;
    }
    newnode->data = x;
    newnode->next = NULL;
    if (pq->tail == NULL) {
        pq->head = pq->tail = newnode;
    } else {
        pq->tail->next = newnode;
        pq->tail = newnode;
    }
    ++pq->size;
    return 0;
}

void queue_pop(queue_t *pq) {
    assert(pq);
    assert(pq->head);

    // 1、一个
    // 2、多个
    if (pq->head->next == NULL) {
        free(pq->head);
        pq->head = pq->tail = NULL;
    } else {
        queue_node_t *next = pq->head->next;
        free(pq->head);
        pq->head = next;
    }
    --pq->size;
    if (pq->size < 0) {
        pq->size = 0;
    }
}

queue_data_t queue_front(queue_t *pq) {
    assert(pq);
    assert(pq->head);

    return pq->head->data;
}

queue_data_t queue_back(queue_t *pq) {
    assert(pq);
    assert(pq->head);

    return pq->tail->data;
}


int queue_empty(queue_t *pq) {
    assert(pq);
    return pq->head == NULL;
}


int queue_size(queue_t *pq){
    assert(pq);
    return pq->size;
}

void queue_destroy(queue_t *pq) {
    assert(pq);

    while (!queue_empty(pq)) {
        char *data = queue_front(pq);
        free(data);
        queue_pop(pq);
    }
}

