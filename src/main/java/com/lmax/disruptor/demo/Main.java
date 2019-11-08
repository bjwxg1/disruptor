package com.lmax.disruptor.demo;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;


/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class Main {
    public static void main(String[] args) {
        String msg = "hello disruptor";
        int ringBufferSize = 1 << 10;
        Disruptor<MsgEvent> disruptor = new Disruptor(new MsgEventFactory(), ringBufferSize,
                new MsgThreadFactory(), ProducerType.SINGLE, new BlockingWaitStrategy());
        disruptor.setDefaultExceptionHandler(new MsgEventExceptionHandler());
        disruptor.handleEventsWith(new MsgEventHandler());
        RingBuffer<MsgEvent> ringBuffer = disruptor.start();
        MsgEventProducer producer = new MsgEventProducer(ringBuffer);
        producer.onData(msg);
    }
}