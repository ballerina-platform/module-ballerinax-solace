/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.solace.producer;

import com.solacesystems.jcsmp.JCSMPException;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Tracks broker confirmations for guaranteed messages accepted by a non-transacted producer.
 */
final class PublishAcknowledgementTracker {

    private enum State {
        OPEN,
        CLOSING,
        CLOSED
    }

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition settlementChanged = lock.newCondition();
    private final Set<Long> pending = new HashSet<>();
    private long nextCorrelationKey;
    private int rejectedCount;
    private String firstRejection;
    private State state = State.OPEN;

    /**
     * Result of waiting for all registered publishes to settle.
     *
     * @param rejectedCount   number of broker-rejected publishes
     * @param unconfirmedCount number of publishes whose outcome is still unknown
     * @param firstRejection  first broker rejection message, if any
     */
    record DrainResult(int rejectedCount, int unconfirmedCount, String firstRejection) {

        boolean successful() {
            return rejectedCount == 0 && unconfirmedCount == 0;
        }
    }

    /**
     * Registers a guaranteed send before it is submitted to JCSMP.
     *
     * @return correlation key used by the JCSMP publish callback
     */
    long register() {
        lock.lock();
        try {
            if (state != State.OPEN) {
                throw new IllegalStateException("Producer is closing");
            }
            long key = ++nextCorrelationKey;
            pending.add(key);
            return key;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes a registration when JCSMP rejects the send synchronously.
     *
     * @param key correlation key to remove
     */
    void cancel(long key) {
        settle(key, null);
    }

    /**
     * Records a positive broker acknowledgement.
     *
     * @param key JCSMP correlation key
     */
    void acknowledge(Object key) {
        settle(key, null);
    }

    /**
     * Records a negative broker acknowledgement.
     *
     * @param key   JCSMP correlation key
     * @param cause broker rejection
     */
    void reject(Object key, JCSMPException cause) {
        settle(key, cause);
    }

    /**
     * Prevents new registrations before close takes its pending-publish snapshot.
     */
    void beginClose() {
        lock.lock();
        try {
            if (state == State.OPEN) {
                state = State.CLOSING;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Waits until all registered publishes settle or the timeout expires.
     *
     * @param timeout maximum drain duration
     * @return settlement summary
     * @throws InterruptedException if the waiting thread is interrupted
     */
    DrainResult awaitSettlement(Duration timeout) throws InterruptedException {
        long remaining = timeout.toNanos();
        lock.lockInterruptibly();
        try {
            while (!pending.isEmpty() && remaining > 0) {
                remaining = settlementChanged.awaitNanos(remaining);
            }
            return new DrainResult(rejectedCount, pending.size(), firstRejection);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Marks the tracker closed and wakes any waiter.
     */
    void markClosed() {
        lock.lock();
        try {
            state = State.CLOSED;
            settlementChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private void settle(Object key, JCSMPException rejection) {
        if (!(key instanceof Long correlationKey)) {
            return;
        }
        lock.lock();
        try {
            if (!pending.remove(correlationKey)) {
                return;
            }
            if (rejection != null) {
                rejectedCount++;
                if (firstRejection == null) {
                    firstRejection = rejection.getMessage();
                }
            }
            settlementChanged.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
