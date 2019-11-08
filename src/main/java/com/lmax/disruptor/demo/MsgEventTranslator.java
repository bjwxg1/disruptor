package com.lmax.disruptor.demo;

import com.lmax.disruptor.EventTranslatorOneArg;

/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class MsgEventTranslator implements EventTranslatorOneArg<MsgEvent, String> {
    @Override
    public void translateTo(MsgEvent msgEvent, long l, String s) {
        msgEvent.setMsg(s);
    }
}
