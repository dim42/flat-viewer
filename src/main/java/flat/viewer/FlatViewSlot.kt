package flat.viewer

import io.vertx.core.shareddata.Shareable
import java.time.LocalDateTime
import java.util.*

class FlatViewSlot(private val flatId: Int, private val startTime: LocalDateTime) : Shareable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as FlatViewSlot
        return flatId == that.flatId &&
                startTime == that.startTime
    }

    override fun hashCode(): Int {
        return Objects.hash(flatId, startTime)
    }

}
