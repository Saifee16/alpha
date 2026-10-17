package family.safe.video.nasa

/** Main-thread attempt ownership: old transfers, timers and media callbacks cannot revive playback. */
internal class NasaPlaybackAttempt {
    private var generation = 0L
    private var phase = Phase.CANCELLED
    private enum class Phase { TRANSFERRING, PREPARING, READY, FAILED, CANCELLED }

    fun begin(): Long { generation++; phase = Phase.TRANSFERRING; return generation }
    fun preparing(token: Long): Boolean = transition(token, Phase.TRANSFERRING, Phase.PREPARING)
    fun prepared(token: Long): Boolean = transition(token, Phase.PREPARING, Phase.READY)
    fun timedOut(token: Long): Boolean = transition(token, Phase.PREPARING, Phase.FAILED)
    fun fail(token: Long): Boolean {
        if (token != generation || phase in setOf(Phase.CANCELLED, Phase.FAILED)) return false
        phase = Phase.FAILED
        return true
    }
    fun isReady(token: Long) = token == generation && phase == Phase.READY
    fun cancel() { generation++; phase = Phase.CANCELLED }
    private fun transition(token: Long, from: Phase, to: Phase): Boolean {
        if (token != generation || phase != from) return false
        phase = to
        return true
    }
}
