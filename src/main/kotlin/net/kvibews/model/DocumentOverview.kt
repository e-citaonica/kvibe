package net.kvibews.model

import java.time.ZonedDateTime

data class DocumentOverview (
    val id: String,
    val name: String,
    val language: String,
    val createdBy: String,
    val created: ZonedDateTime
)