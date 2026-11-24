package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_sources")
data class NetworkSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val basePath: String,
    val createdAt: Long = System.currentTimeMillis()
)
