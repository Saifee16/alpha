package family.safe.video.nasa

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

class NasaOperationCancelled : IllegalStateException("NASA operation cancelled")
class NasaOperationDeadline : IllegalStateException("NASA operation deadline exceeded")

/** One monotonic budget shared by every request and local verification step in an operation. */
class NasaOperation(
    timeoutMillis: Long = 60_000,
    private val clock: () -> Long = System::nanoTime
) : AutoCloseable {
    private enum class State { ACTIVE, CANCELLED, DEADLINE, CLOSED }
    private val lock = Any()
    private var state = State.ACTIVE
    private var active: HttpsURLConnection? = null
    private val started = clock()
    private val budgetNanos: Long
    private val watchdog: java.util.concurrent.ScheduledFuture<*>

    init {
        require(timeoutMillis in 1..600_000) { "Invalid NASA operation timeout" }
        budgetNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        watchdog = scheduler.schedule({ stop(State.DEADLINE) }, timeoutMillis, TimeUnit.MILLISECONDS)
    }

    fun check() {
        if (Thread.currentThread().isInterrupted) cancel()
        if (clock() - started >= budgetNanos) stop(State.DEADLINE)
        when (synchronized(lock) { state }) {
            State.ACTIVE -> Unit
            State.DEADLINE -> throw NasaOperationDeadline()
            State.CANCELLED, State.CLOSED -> throw NasaOperationCancelled()
        }
    }

    fun cancel() {
        stop(State.CANCELLED)
        watchdog.cancel(false)
    }

    override fun close() {
        stop(State.CLOSED)
        watchdog.cancel(false)
    }

    fun <T> withConnection(connection: HttpsURLConnection, block: () -> T): T {
        try {
            check()
            synchronized(lock) {
                check()
                check(active == null) { "Concurrent NASA connections are forbidden" }
                active = connection
            }
            configureNasaConnection(connection)
            val remainingMillis = TimeUnit.NANOSECONDS.toMillis(budgetNanos - (clock() - started)).coerceAtLeast(1)
            connection.connectTimeout = minOf(connection.connectTimeout.toLong(), remainingMillis).toInt()
            connection.readTimeout = minOf(connection.readTimeout.toLong(), remainingMillis).toInt()
            check()
            return block().also { check() }
        } finally {
            synchronized(lock) { if (active === connection) active = null }
            try {
                connection.disconnect()
            } catch (failure: Exception) {
                check() // Preserve cancellation/deadline if cleanup itself fails.
                throw failure
            }
        }
    }

    private fun stop(next: State) {
        val connection = synchronized(lock) {
            if (state != State.ACTIVE) return
            state = next
            active.also { active = null }
        }
        // Activity cancellation and watchdog ticks must never wait for platform transport cleanup.
        // Abort is best effort; terminal state and monotonic checks still deny every later result.
        if (connection != null) {
            try {
                cleanup.execute { runCatching { connection.disconnect() } }
            } catch (_: RejectedExecutionException) {
                // A saturated cleanup pool cannot delay cancellation; the worker still disconnects in finally.
            }
        }
    }

    private companion object {
        val scheduler = ScheduledThreadPoolExecutor(1) { task ->
            Thread(task, "familysafe-nasa-deadline").apply { isDaemon = true }
        }.apply { removeOnCancelPolicy = true }
        val cleanup = ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, ArrayBlockingQueue(16), { task ->
            Thread(task, "familysafe-nasa-abort").apply { isDaemon = true }
        }).apply { allowCoreThreadTimeOut(true) }
    }
}

internal fun <T> withNasaOperation(operation: NasaOperation?, block: (NasaOperation) -> T): T =
    if (operation != null) {
        operation.check()
        block(operation).also { operation.check() }
    } else NasaOperation().use { owned -> block(owned).also { owned.check() } }
