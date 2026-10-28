package dev.areyousureyou.wanttogohere.enforcement;

/** Prevents a ward from reopening until monitoring observes that its target was left. */
public final class TargetVisitGate {
    private String activeTargetId;

    public boolean shouldShow(String targetId) {
        if (targetId == null || targetId.equals(activeTargetId)) return false;
        activeTargetId = targetId;
        return true;
    }

    public void leaveTarget() {
        activeTargetId = null;
    }
}
