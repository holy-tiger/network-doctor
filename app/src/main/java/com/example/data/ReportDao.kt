package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Query("SELECT * FROM diagnostic_reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM diagnostic_reports WHERE id = :id LIMIT 1")
    suspend fun getReportById(id: Long): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity): Long

    @Query("DELETE FROM diagnostic_reports WHERE id = :id")
    suspend fun deleteReportById(id: Long)

    @Query("DELETE FROM diagnostic_reports")
    suspend fun clearAllReports()
}
