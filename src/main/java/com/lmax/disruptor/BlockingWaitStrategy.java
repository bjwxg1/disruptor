/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor;

import com.lmax.disruptor.util.ThreadHints;

/**
 * Blocking strategy that uses a lock and condition variable for {@link EventProcessor}s waiting on a barrier.
 * <p>
 * This strategy can be used when throughput and low-latency are not as important as CPU resource.
 */
//阻塞等待
public final class BlockingWaitStrategy implements WaitStrategy {
    private final Object mutex = new Object();

    @Override
    public long waitFor(long sequence, Sequence cursorSequence, Sequence dependentSequence, SequenceBarrier barrier)
        throws AlertException, InterruptedException {
        long availableSequence;
        //如果生产者的游标小于sequence，循环等待
        if (cursorSequence.get() < sequence) {
            synchronized (mutex) {
                //循环校验，防止错误唤醒
                while (cursorSequence.get() < sequence) {
                    barrier.checkAlert();
                    //循环等待，在Sequencer中publish进行唤醒；等待消费时也会在循环中定时唤醒。
                    //循环等待时要检查alert状态。如果不检查将导致不能关闭Disruptor。
                    mutex.wait();
                }
            }
        }
        //获取dependentSequence中最小的游标，判断是否小于sequence，
        //如果sequence大于上一个消费者组最慢消费者（如当前消费者为第一组则和生产者游标序号比较）序号时，需要等待。
        //因为消费者可能存在依赖关系，所以不能消费其依赖的消费者未消费完毕的事件。
        //那么为什么这里没有锁呢？可以想一下此时的场景，代码运行至此，已能保证生产者有新事件，如果进入循环，说明上一组消费者还未消费完毕。
        //而通常我们的消费者都是较快完成任务的，所以这里才会考虑使用Busy Spin的方式等待上一组消费者完成消费
        while ((availableSequence = dependentSequence.get()) < sequence) {
            barrier.checkAlert();
            ThreadHints.onSpinWait();
        }
        return availableSequence;
    }

    @Override
    public void signalAllWhenBlocking() {
        synchronized (mutex) {
            mutex.notifyAll();
        }
    }

    @Override
    public String toString() {
        return "BlockingWaitStrategy{" +
            "mutex=" + mutex +
            '}';
    }
}
