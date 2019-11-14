package flat.viewer;

import java.time.LocalDateTime;
import java.util.Objects;

public class ViewSlot {
    private final LocalDateTime startTime;
    private final Integer newTenantId;
    private SlotState state;

    public ViewSlot(LocalDateTime start, Integer tenantId) {
        this.startTime = start;
        this.newTenantId = tenantId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public Integer getNewTenantId() {
        return newTenantId;
    }

    public void setState(SlotState rejected) {
        this.state = rejected;
    }

    public SlotState getState() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ViewSlot viewSlot = (ViewSlot) o;
        return Objects.equals(startTime, viewSlot.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime);
    }
}
