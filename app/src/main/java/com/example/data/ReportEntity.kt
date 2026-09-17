package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val testType: String, // PING, TCP, DNS, TRACEROUTE, FULL_DIAG
    val targetHost: String,
    val targetPort: Int = 0,
    val status: String, // SUCCESS, WARNING, FAILED
    val summary: String,
    val detailsJson: String
)
