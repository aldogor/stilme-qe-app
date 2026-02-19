package com.aldogor.stilme_qe_app.sync

import android.content.Context
import androidx.room.*
import com.aldogor.stilme_qe_app.study.SubmissionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Room database for offline submission queue.
 * Stores questionnaire submissions that couldn't be sent to REDCap.
 */

// ============================================================================
// ENTITY
// ============================================================================

@Entity(tableName = "submission_queue")
data class QueuedSubmission(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "study_id")
    val studyId: String,

    @ColumnInfo(name = "event_name")
    val eventName: String,

    @ColumnInfo(name = "payload")
    val payload: String, // JSON string

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,

    @ColumnInfo(name = "status")
    val status: SubmissionStatus = SubmissionStatus.PENDING,

    @ColumnInfo(name = "last_error")
    val lastError: String? = null,

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null
)

// ============================================================================
// DAO
// ============================================================================

@Dao
interface SubmissionDao {
    @Query("SELECT * FROM submission_queue WHERE status = :status ORDER BY created_at ASC")
    suspend fun getByStatus(status: SubmissionStatus): List<QueuedSubmission>

    @Query("SELECT * FROM submission_queue WHERE status IN (:statuses) ORDER BY created_at ASC")
    suspend fun getByStatuses(statuses: List<SubmissionStatus>): List<QueuedSubmission>

    @Query("SELECT * FROM submission_queue ORDER BY created_at ASC")
    suspend fun getAll(): List<QueuedSubmission>

    @Query("SELECT * FROM submission_queue ORDER BY created_at ASC")
    fun getAllFlow(): Flow<List<QueuedSubmission>>

    @Query("SELECT COUNT(*) FROM submission_queue WHERE status = :status")
    suspend fun countByStatus(status: SubmissionStatus): Int

    @Query("SELECT COUNT(*) FROM submission_queue WHERE status IN (:statuses)")
    suspend fun countByStatuses(statuses: List<SubmissionStatus>): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(submission: QueuedSubmission): Long

    @Update
    suspend fun update(submission: QueuedSubmission)

    @Delete
    suspend fun delete(submission: QueuedSubmission)

    @Query("DELETE FROM submission_queue WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM submission_queue WHERE status = :status")
    suspend fun deleteByStatus(status: SubmissionStatus)

    @Query("DELETE FROM submission_queue")
    suspend fun deleteAll()

    @Query("""
        UPDATE submission_queue
        SET retry_count = retry_count + 1,
            status = :status,
            last_error = :error,
            last_attempt_at = :attemptTime
        WHERE id = :id
    """)
    suspend fun updateRetry(
        id: Long,
        status: SubmissionStatus,
        error: String?,
        attemptTime: Long = System.currentTimeMillis()
    )

    @Query("UPDATE submission_queue SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: SubmissionStatus)

    @Query("SELECT * FROM submission_queue WHERE study_id = :studyId AND event_name = :eventName LIMIT 1")
    suspend fun findByStudyIdAndEvent(studyId: String, eventName: String): QueuedSubmission?
}

// ============================================================================
// TYPE CONVERTERS
// ============================================================================

class Converters {
    @TypeConverter
    fun fromSubmissionStatus(status: SubmissionStatus): String = status.name

    @TypeConverter
    fun toSubmissionStatus(value: String): SubmissionStatus = SubmissionStatus.valueOf(value)
}

// ============================================================================
// DATABASE
// ============================================================================

@Database(
    entities = [QueuedSubmission::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StilmeDatabase : RoomDatabase() {
    abstract fun submissionDao(): SubmissionDao

    companion object {
        @Volatile
        private var INSTANCE: StilmeDatabase? = null

        fun getInstance(context: Context): StilmeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StilmeDatabase::class.java,
                    "stilme_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ============================================================================
// REPOSITORY
// ============================================================================

class SubmissionRepository(context: Context) {
    private val dao = StilmeDatabase.getInstance(context).submissionDao()

    suspend fun getPending(): List<QueuedSubmission> =
        dao.getByStatus(SubmissionStatus.PENDING)

    suspend fun getPendingAndFailed(): List<QueuedSubmission> =
        dao.getByStatuses(listOf(SubmissionStatus.PENDING, SubmissionStatus.FAILED))

    suspend fun getAll(): List<QueuedSubmission> = dao.getAll()

    fun getAllFlow(): Flow<List<QueuedSubmission>> = dao.getAllFlow()

    suspend fun countPending(): Int = dao.countByStatus(SubmissionStatus.PENDING)

    suspend fun countPendingAndFailed(): Int =
        dao.countByStatuses(listOf(SubmissionStatus.PENDING, SubmissionStatus.FAILED))

    suspend fun enqueue(
        studyId: String,
        eventName: String,
        payload: String
    ): Long {
        // Check if already exists to avoid duplicates
        val existing = dao.findByStudyIdAndEvent(studyId, eventName)
        if (existing != null) {
            // Update existing submission
            dao.update(existing.copy(
                payload = payload,
                status = SubmissionStatus.PENDING,
                retryCount = 0,
                lastError = null
            ))
            return existing.id
        }

        return dao.insert(
            QueuedSubmission(
                studyId = studyId,
                eventName = eventName,
                payload = payload
            )
        )
    }

    suspend fun markSubmitting(id: Long) {
        dao.updateStatus(id, SubmissionStatus.SUBMITTING)
    }

    suspend fun markSuccess(id: Long) {
        dao.deleteById(id)
    }

    suspend fun markFailed(id: Long, error: String?) {
        dao.updateRetry(id, SubmissionStatus.FAILED, error)
    }

    suspend fun delete(submission: QueuedSubmission) {
        dao.delete(submission)
    }

    suspend fun deleteAll() {
        dao.deleteAll()
    }
}
