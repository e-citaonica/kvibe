package net.kvibews.model

import java.time.ZonedDateTime

data class DocumentPreview(
    val id: String,
    val name: String,
    val language: String,
    val content: String,
    val updatedAt: ZonedDateTime
) {
    constructor(document: DocumentState, previewContentLen: Int) : this(
        document.id,
        document.name,
        document.language,
        document.content.trimStart().substring(0, document.content.trimStart().length.coerceAtMost(previewContentLen)),
        document.updatedAt
    )
}