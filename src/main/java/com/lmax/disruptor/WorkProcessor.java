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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>A {@link WorkProcessor} wraps a single {@link WorkHandler}, effectively consuming the sequence
 * and ensuring appropriate barriers.</p>
 *
 * <p>Generally, this will be used as part of a {@link WorkerPool}.</p>
 *
 * @param <T> event implementation storing the details for the work to processed.
 */
public final class WorkProcessor<T> implements EventProcessor {
    //运行标识
    private final AtomicBoolean running = new AtomicBoolean(false);
    //记录当前WorkProcessor的消费位置
    private final Sequence sequence = new Sequence(Sequencer.INITIAL_CURSOR_VALUE);
    private final RingBuffer<T> ringBuffer;
    private final SequenceBarrier sequenceBarrier;
    //WorkHandler进行工作处理
    private final WorkHandler<? super T> workHandler;
    private final ExceptionHandler<? super T> exceptionHandler;
    //workSequence：处于同一个WorkerPool内的WorkProcessor共享
    private final Sequence workSequence;
    private final EventReleaser eventReleaser = new EventReleaser()
    {
        @Override
        public void release()
        {
            sequence.set(Long.MAX_VALUE);
        }
    };
    private final TimeoutHandler timeoutHandler;

    /**
     * Construct a {@link WorkProcessor}.
     *
     * @param ringBuffer       to which events are published.
     * @param sequenceBarrier  on which it is waiting.
     * @param workHandler      is the delegate to which events are dispatched.
     * @param exceptionHandler to be called back when an error occurs
     * @param workSequence     from which to claim the next event to be worked on.  It should always be initialised
     *                         as {@link Sequencer#INITIAL_CURSOR_VALUE}
     */
    public WorkProcessor(final RingBuffer<T> ringBuffer, final SequenceBarrier sequenceBarrier,
                         final WorkHandler<? super T> workHandler,
                         final ExceptionHandler<? super T> exceptionHandler, final Sequence workSequence) {
        this.ringBuffer = ringBuffer;
        this.sequenceBarrier = sequenceBarrier;
        this.workHandler = workHandler;
        this.exceptionHandler = exceptionHandler;
        this.workSequence = workSequence;

        if (this.workHandler instanceof EventReleaseAware) {
            ((EventReleaseAware) this.workHandler).setEventReleaser(eventReleaser);
        }

        timeoutHandler = (workHandler instanceof TimeoutHandler) ? (TimeoutHandler) workHandler : null;
    }

    @Override
    public Sequence getSequence()
    {
        return sequence;
    }

    @Override
    public void halt() {
        running.set(false);
        sequenceBarrier.alert();
    }

    @Override
    public boolean isRunning()
    {
        return running.get();
    }

    /**
     * It is ok to have another thread re-run this method after a halt().
     *
     * @throws IllegalStateException if this processor is already running
     */
    @Override
    public void run() {
        //启动WorkProcessor
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("Thread is already running");
        }
        sequenceBarrier.clearAlert();
        notifyStart();
        //处理标识，判断上一个获取的Event是否已经被处理，如果已经处理才会获取下一个
        boolean processedSequence = true;
        //cachedAvailableSequence 记录可以消费的Event的offset下限
        long cachedAvailableSequence = Long.MIN_VALUE;
        long nextSequence = sequence.get();
        T event = null;
        while (true) {
            try {
                //如果前一个元素已经被处理，尝试获取下一个
                if (processedSequence) {
                    processedSequence = false;
                    do {
                        //这就是WorkProcessor是为什么消息队列模型的原因？【不同WorkProcessor消费不同的消息】
                        //workSequence记录了消费位移，所有的WorkProcessor公用一个workSequence来记录消费offset
                        nextSequence = workSequence.get() + 1L;
                        //
                        sequence.set(nextSequence - 1L);
                    }
                    //同一个WorkerPool中Processor可能会并发，通过CAS尝试多次
                    while (!workSequence.compareAndSet(nextSequence - 1L, nextSequence));
                }

                //如果存在可用的Sequence，从RingBuffer中进行获取并处理
                if (cachedAvailableSequence >= nextSequence) {
                    event = ringBuffer.get(nextSequence);
                    workHandler.onEvent(event);
                    processedSequence = true;
                } else {
                    //如果不存在可用数据则进行等待
                    cachedAvailableSequence = sequenceBarrier.waitFor(nextSequence);
                }
            } catch (final TimeoutException e) {
                notifyTimeout(sequence.get());
            } catch (final AlertException ex) {
                if (!running.get()) {
                    break;
                }
            } catch (final Throwable ex) {
                exceptionHandler.handleEventException(ex, nextSequence, event);
                processedSequence = true;
            }
        }
        notifyShutdown();
        running.set(false);
    }

    private void notifyTimeout(final long availableSequence)
    {
        try
        {
            if (timeoutHandler != null)
            {
                timeoutHandler.onTimeout(availableSequence);
            }
        }
        catch (Throwable e)
        {
            exceptionHandler.handleEventException(e, availableSequence, null);
        }
    }

    private void notifyStart()
    {
        if (workHandler instanceof LifecycleAware)
        {
            try
            {
                ((LifecycleAware) workHandler).onStart();
            }
            catch (final Throwable ex)
            {
                exceptionHandler.handleOnStartException(ex);
            }
        }
    }

    private void notifyShutdown()
    {
        if (workHandler instanceof LifecycleAware)
        {
            try
            {
                ((LifecycleAware) workHandler).onShutdown();
            }
            catch (final Throwable ex)
            {
                exceptionHandler.handleOnShutdownException(ex);
            }
        }
    }
}
