package com.lmax.disruptor.demo;

import java.util.concurrent.ThreadFactory;

/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class MsgThreadFactory implements ThreadFactory {
    private static final String DEFAULT_NAME_PRIFIX = "msg_event_thread";

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, DEFAULT_NAME_PRIFIX);
    }
}
