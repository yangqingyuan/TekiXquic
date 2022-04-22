//
// Created by lizhi on 2022/4/22.
//

#ifndef TEKIXQUIC_CQUEUE_H
#define TEKIXQUIC_CQUEUE_H

#include <stddef.h>
#include <string.h>
#include <stdio.h>
#include <math.h>
#include <stdlib.h>
#include <float.h>
#include <limits.h>
#include <ctype.h>
#include <assert.h>

typedef char* queue_data_t;

typedef struct queue_node_s
{
    struct queue_node_s* next;
    queue_data_t data;
}queue_node_t;


typedef struct queue_s//
{
    queue_node_t* tail;//头指针
    queue_node_t* head;//尾指针
    int size;
}queue_t;


void queue_init(queue_t* pq);

int queue_size(queue_t *pq);

int queue_push(queue_t* pq, queue_data_t x);

void queue_pop(queue_t* pq);

queue_data_t queue_front(queue_t* pq);

queue_data_t queue_back(queue_t* pq);

int queue_empty(queue_t* pq);

void queue_destroy(queue_t* pq);


#endif //TEKIXQUIC_CQUEUE_H
