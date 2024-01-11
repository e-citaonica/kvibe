package net.kvibews.exception

class DocumentNotFoundException(
    documentId: String
) : Exception("Document with $documentId not found")