package flat.viewer;

import java.time.LocalDateTime;
import java.util.Objects;

public class FlatViewSlot {
    private final Integer flatId;
    private final LocalDateTime startTime;

    public FlatViewSlot(Integer flatId, LocalDateTime start) {
        this.flatId = flatId;
        this.startTime = start;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FlatViewSlot that = (FlatViewSlot) o;
        return Objects.equals(flatId, that.flatId) &&
                Objects.equals(startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(flatId, startTime);
    }
}
