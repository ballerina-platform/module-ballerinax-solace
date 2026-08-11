/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.solace.consumer;

import com.solacesystems.jcsmp.FlowEvent;
import com.solacesystems.jcsmp.FlowEventArgs;
import com.solacesystems.jcsmp.FlowEventHandler;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks the latest delivery state reported for a guaranteed consumer flow.
 */
final class ConsumerFlowStateTracker implements FlowEventHandler {

    enum State {
        UNKNOWN,
        ACTIVE,
        INACTIVE,
        DOWN
    }

    private final AtomicReference<State> state;

    ConsumerFlowStateTracker() {
        state = new AtomicReference<>(State.UNKNOWN);
    }

    @Override
    public void handleEvent(Object source, FlowEventArgs eventArgs) {
        FlowEvent event = eventArgs.getEvent();
        if (event == FlowEvent.FLOW_ACTIVE) {
            state.set(State.ACTIVE);
        } else if (event == FlowEvent.FLOW_INACTIVE) {
            state.set(State.INACTIVE);
        } else if (event == FlowEvent.FLOW_DOWN) {
            state.set(State.DOWN);
        } else if (event == FlowEvent.FLOW_UP || event == FlowEvent.FLOW_RECONNECTED) {
            state.compareAndSet(State.DOWN, State.UNKNOWN);
        }
    }

    State state() {
        return state.get();
    }

    void flowStarted(boolean activeFlowIndicationEnabled) {
        if (activeFlowIndicationEnabled) {
            state.compareAndSet(State.UNKNOWN, State.INACTIVE);
        }
    }
}
