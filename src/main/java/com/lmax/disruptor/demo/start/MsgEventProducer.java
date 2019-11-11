package com.lmax.disruptor.demo.start;

import com.lmax.disruptor.RingBuffer;

/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class MsgEventProducer {
    private RingBuffer<MsgEvent> ringBuffer;

    public MsgEventProducer(RingBuffer<MsgEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    /**
     * 将接收到的消息输出到ringBuffer
     */
    public void onData(String  msg){
        MsgEventTranslator translator = new MsgEventTranslator();
        ringBuffer.publishEvent(translator,msg);
    }
}
