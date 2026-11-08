package com.taki.petrobot.storage

import android.content.Context
import androidx.room.Room
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class StructuredLogStore internal constructor(
    context: Context,
    private val onStored: (StructuredEventType, Long) -> Unit,
    private val onError: (Throwable) -> Unit,
) : AutoCloseable {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        PetRobotDatabase::class.java,
        DATABASE_NAME,
    )
        .addMigrations(PetRobotDatabase.MIGRATION_1_2)
        .build()
    private val dao = database.structuredLogDao()
    private val executor: ExecutorService =
        Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "PetRobotStructuredLog")
        }
    private val closed = AtomicBoolean(false)

    fun append(entry: StructuredLogEntry) {
        submit {
            val rowId = dao.insert(entry.toEntity())
            onStored(entry.eventType, rowId)
        }
    }

    fun count(onResult: (Long) -> Unit) {
        submit {
            onResult(dao.count())
        }
    }

    fun readRecent(
        limit: Int,
        onResult: (List<StructuredLogEntry>) -> Unit,
    ) {
        require(limit > 0)
        submit {
            onResult(dao.readRecent(limit).map(StructuredLogEntity::toEntry))
        }
    }

    fun readSummary(
        window: StructuredLogSummaryWindow,
        onResult: (StructuredLogSummary) -> Unit,
    ) {
        submit {
            val counts = dao.readSummaryCounts(
                startEpochMillis = window.startEpochMillis,
                endInclusiveEpochMillis = window.endInclusiveEpochMillis,
            )
            onResult(counts.withWindow(window))
        }
    }

    fun readReplayEntries(
        window: StructuredLogSummaryWindow,
        batchSize: Int = DEFAULT_REPLAY_BATCH_SIZE,
        onBatch: (List<StructuredLogEntry>) -> Unit,
        onComplete: () -> Unit,
    ) {
        require(batchSize > 0)
        submit {
            var afterWallTimeEpochMillis = window.startEpochMillis
            var afterId = 0L
            while (true) {
                val page = dao.readReplayPage(
                    startEpochMillis = window.startEpochMillis,
                    endInclusiveEpochMillis = window.endInclusiveEpochMillis,
                    afterWallTimeEpochMillis = afterWallTimeEpochMillis,
                    afterId = afterId,
                    limit = batchSize,
                )
                if (page.isEmpty()) break
                onBatch(page.map(StructuredLogEntity::toEntry))
                afterWallTimeEpochMillis = page.last().wallTimeEpochMillis
                afterId = page.last().id
                if (page.size < batchSize) break
            }
            onComplete()
        }
    }

    fun deleteBefore(
        cutoffEpochMillis: Long,
        onResult: (Int) -> Unit = {},
    ) {
        require(cutoffEpochMillis >= 0L)
        submit {
            onResult(dao.deleteBefore(cutoffEpochMillis))
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        executor.execute(database::close)
        executor.shutdown()
    }

    private fun submit(block: () -> Unit) {
        if (closed.get()) return
        runCatching {
            executor.execute {
                runCatching(block).onFailure(onError)
            }
        }.onFailure(onError)
    }

    private companion object {
        const val DATABASE_NAME = "petrobot-v0.db"
        const val DEFAULT_REPLAY_BATCH_SIZE = 1_000
    }
}
