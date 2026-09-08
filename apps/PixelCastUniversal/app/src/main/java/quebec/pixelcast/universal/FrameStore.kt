package quebec.pixelcast.universal

object FrameStore {
    private val lock = Object()
    @Volatile private var jpeg: ByteArray? = null
    @Volatile private var seq: Long = 0
    fun update(frame: ByteArray) { synchronized(lock) { jpeg = frame; seq++; lock.notifyAll() } }
    fun waitNext(lastSeq: Long, timeoutMs: Long = 1000): Pair<Long, ByteArray?> {
        synchronized(lock) { if (seq == lastSeq) lock.wait(timeoutMs); return seq to jpeg }
    }
}
