package net.kvibews.model

import java.time.ZonedDateTime

data class DocumentPreview(
    val id: String,
    val name: String,
    val languageId: Int,
    val updatedAt: ZonedDateTime
) {
    constructor(document: DocumentState) : this(document.id, document.name, document.languageId, document.updatedAt)
}