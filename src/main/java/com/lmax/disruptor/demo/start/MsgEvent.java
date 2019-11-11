package com.lmax.disruptor.demo.start;

/**
 * @Descriprion:
 * @Author:wuxiaoguang@58.com
 * @Date：created in 2019/11/8
 */
public class MsgEvent<T> {
    private T msg;

    public MsgEvent() {

    }

    public MsgEvent(T msg) {
        this.msg = msg;
    }

    public T getMsg() {
        return this.msg;
    }

    public void setMsg(T msg) {
        this.msg = msg;
    }
}
