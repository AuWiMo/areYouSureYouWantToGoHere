package dev.areyousureyou.wanttogohere.enforcement;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class TargetVisitGateTest {
    @Test public void targetShowsOnceUntilAVisitOutsideItIsObserved() {
        TargetVisitGate gate = new TargetVisitGate();
        assertTrue(gate.shouldShow("reddit-target"));
        assertFalse(gate.shouldShow("reddit-target"));

        gate.leaveTarget();
        assertTrue(gate.shouldShow("reddit-target"));
    }

    @Test public void changingToAnotherTargetCanShowItsWard() {
        TargetVisitGate gate = new TargetVisitGate();
        assertTrue(gate.shouldShow("reddit-target"));
        assertTrue(gate.shouldShow("mail-target"));
    }
}
