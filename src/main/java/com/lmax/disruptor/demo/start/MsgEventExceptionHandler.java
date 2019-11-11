package com.lmax.disruptor.demo.start;

import com.lmax.disruptor.ExceptionHandler;

/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class MsgEventExceptionHandler implements ExceptionHandler<MsgEvent> {
    @Override
    public void handleEventException(Throwable throwable, long l, MsgEvent msgEvent) {

    }

    @Override
    public void handleOnStartException(Throwable throwable) {

    }

    @Override
    public void handleOnShutdownException(Throwable throwable) {

    }
}
