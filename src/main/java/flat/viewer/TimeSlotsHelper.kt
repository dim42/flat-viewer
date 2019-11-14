package flat.viewer

import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

object TimeSlotsHelper {
    fun initTimeSlots(start: LocalTime, end: LocalTime?, duration: Int): Set<Map.Entry<LocalTime, LocalTime>> {
        val minutes = ChronoUnit.MINUTES.between(start, end)
        val count = minutes / duration
        val timeSlots: MutableList<Map.Entry<LocalTime, LocalTime>> = ArrayList()
        for (i in 0 until count) {
            val add = (duration * i).toInt()
            timeSlots.add(AbstractMap.SimpleEntry(start.plus(add.toLong(), ChronoUnit.MINUTES), start.plus(add + duration.toLong(), ChronoUnit.MINUTES)))
        }
        val left = minutes % duration
        if (left > 0) {
            val entry = timeSlots[timeSlots.size - 1]
            timeSlots.add(AbstractMap.SimpleEntry(entry.value, entry.value.plus(left, ChronoUnit.MINUTES)))
        }
        return HashSet(timeSlots)
    }
}
