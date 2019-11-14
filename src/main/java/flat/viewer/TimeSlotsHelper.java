package flat.viewer;

import java.time.LocalTime;
import java.util.*;

import static java.time.temporal.ChronoUnit.MINUTES;

public class TimeSlotsHelper {

    public static Set<Map.Entry<LocalTime, LocalTime>> initTimeSlots(LocalTime start, LocalTime end, int duration) {
        long minutes = MINUTES.between(start, end);
        long count = minutes / duration;
        List<Map.Entry<LocalTime, LocalTime>> timeSlots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int add = duration * i;
            timeSlots.add(new AbstractMap.SimpleEntry<>(start.plus(add, MINUTES), start.plus(add + duration, MINUTES)));
        }
        long left = minutes % duration;
        if (left > 0) {
            Map.Entry<LocalTime, LocalTime> entry = timeSlots.get(timeSlots.size() - 1);
            timeSlots.add(new AbstractMap.SimpleEntry<>(entry.getValue(), entry.getValue().plus(left, MINUTES)));
        }
        return new HashSet<>(timeSlots);
    }
}
