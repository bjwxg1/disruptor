package com.lmax.disruptor.demo;

import com.lmax.disruptor.EventHandler;

/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class MsgEventHandler implements EventHandler<MsgEvent> {

    @Override
    public void onEvent(MsgEvent msgEvent, long l, boolean b) throws Exception {
        System.err.println(msgEvent);
    }
}
