package com.lmax.disruptor.demo.start;

import com.lmax.disruptor.EventFactory;

/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class MsgEventFactory implements EventFactory<MsgEvent> {
    @Override
    public MsgEvent newInstance() {
        return new MsgEvent();
    }
}
