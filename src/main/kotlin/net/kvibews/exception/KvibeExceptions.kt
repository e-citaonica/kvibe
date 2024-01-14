package net.kvibews.exception

import kotlin.RuntimeException

class DocumentNotFoundException(
    documentId: String
) : RuntimeException("Document with $documentId not found")

class InvalidOperationRevision(
    message: String
) : RuntimeException(message)