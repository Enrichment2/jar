package com.jar.app

data class ExportData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val notes: List<ExportNote>,
    val tags: List<ExportTag>
)

data class ExportNote(
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tagNames: List<String>
)

data class ExportTag(
    val name: String,
    val color: Int
)
