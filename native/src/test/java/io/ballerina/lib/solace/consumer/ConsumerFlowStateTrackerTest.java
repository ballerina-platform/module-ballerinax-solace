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
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ConsumerFlowStateTrackerTest {

    @Test
    public void testFlowStateTransitions() {
        ConsumerFlowStateTracker tracker = new ConsumerFlowStateTracker();
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.UNKNOWN);

        tracker.handleEvent(this, event(FlowEvent.FLOW_ACTIVE));
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.ACTIVE);

        tracker.handleEvent(this, event(FlowEvent.FLOW_INACTIVE));
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.INACTIVE);

        tracker.handleEvent(this, event(FlowEvent.FLOW_DOWN));
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.DOWN);
    }

    @Test
    public void testRecoveredFlowClearsDownState() {
        ConsumerFlowStateTracker tracker = new ConsumerFlowStateTracker();
        tracker.handleEvent(this, event(FlowEvent.FLOW_DOWN));
        tracker.handleEvent(this, event(FlowEvent.FLOW_RECONNECTED));
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.UNKNOWN);

        tracker.handleEvent(this, event(FlowEvent.FLOW_DOWN));
        tracker.handleEvent(this, event(FlowEvent.FLOW_UP));
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.UNKNOWN);
    }

    @Test
    public void testStateUnknownUntilFirstEvent() {
        ConsumerFlowStateTracker tracker = new ConsumerFlowStateTracker();
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.UNKNOWN);
    }

    @Test
    public void testFlowStartAppliesInactiveFallback() {
        ConsumerFlowStateTracker tracker = new ConsumerFlowStateTracker();
        tracker.flowStarted(true);
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.INACTIVE);
    }

    @Test
    public void testFlowStartDoesNotOverwriteReportedState() {
        ConsumerFlowStateTracker tracker = new ConsumerFlowStateTracker();
        tracker.handleEvent(this, event(FlowEvent.FLOW_ACTIVE));
        tracker.flowStarted(true);
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.ACTIVE);
    }

    @Test
    public void testFlowStartWithoutIndicationRemainsUnknown() {
        ConsumerFlowStateTracker tracker = new ConsumerFlowStateTracker();
        tracker.flowStarted(false);
        assertEquals(tracker.state(), ConsumerFlowStateTracker.State.UNKNOWN);
    }

    private static FlowEventArgs event(FlowEvent event) {
        return new FlowEventArgs(event, event.name(), null, 0);
    }
}
