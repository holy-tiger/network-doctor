package com.example.data

import kotlinx.coroutines.flow.Flow

class ReportRepository(private val reportDao: ReportDao) {
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()

    suspend fun insert(report: ReportEntity): Long = reportDao.insertReport(report)

    suspend fun deleteById(id: Long) = reportDao.deleteReportById(id)

    suspend fun clearAll() = reportDao.clearAllReports()

    suspend fun getById(id: Long): ReportEntity? = reportDao.getReportById(id)
}
