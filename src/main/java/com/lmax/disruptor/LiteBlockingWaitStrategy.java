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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Variation of the {@link BlockingWaitStrategy} that attempts to elide conditional wake-ups when
 * the lock is uncontended.  Shows performance improvements on microbenchmarks.  However this
 * wait strategy should be considered experimental as I have not full proved the correctness of
 * the lock elision code.
 */
public final class LiteBlockingWaitStrategy implements WaitStrategy {
    private final Object mutex = new Object();
    private final AtomicBoolean signalNeeded = new AtomicBoolean(false);

    @Override
    public long waitFor(long sequence, Sequence cursorSequence, Sequence dependentSequence, SequenceBarrier barrier)
        throws AlertException, InterruptedException {
        long availableSequence;
        if (cursorSequence.get() < sequence) {
            synchronized (mutex) {
                //和BlockingWaitStrategy相比，LiteBlockingWaitStrategy使用do...While循环。
                //在第一次循环前，如果有生产者写入了数据（因为signalNeeded为false）所以signalAllWhenBlocking不需要进行唤醒；
                //在生产者生产数据之后，第一次进行循环判断如果cursorSequence.get() >= sequence就会跳出循环，也不会进行wait()操作；
                //和BlockingWaitStrategy相比这种操作会特定的场景下减少notifyAll()和wait()操作的次数
                do {
                    signalNeeded.getAndSet(true);
                    if (cursorSequence.get() >= sequence) {
                        break;
                    }
                    barrier.checkAlert();
                    mutex.wait();
                } while (cursorSequence.get() < sequence);
            }
        }
        while ((availableSequence = dependentSequence.get()) < sequence) {
            barrier.checkAlert();
            ThreadHints.onSpinWait();
        }
        return availableSequence;
    }
    @Override
    public void signalAllWhenBlocking() {
        if (signalNeeded.getAndSet(false)) {
            synchronized (mutex) {
                mutex.notifyAll();
            }
        }
    }

    @Override
    public String toString() {
        return "LiteBlockingWaitStrategy{" +
            "mutex=" + mutex +
            ", signalNeeded=" + signalNeeded +
            '}';
    }
}
