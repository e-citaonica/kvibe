package net.kvibews.exception

import kotlin.RuntimeException

class DocumentNotFoundException(
    documentId: String
) : RuntimeException("Document with $documentId not found")

class InvalidOperationRevisionException(
    message: String
) : RuntimeException(message)