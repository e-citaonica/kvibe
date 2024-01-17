package net.kvibews.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.kvibews.model.DocumentState
import org.redisson.api.RBucket
import org.redisson.api.RFuture
import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.redisson.codec.JacksonCodec
import org.springframework.stereotype.Repository


@Repository
class DocumentRepository(
    private val redisson: RedissonClient,
    objectMapper: ObjectMapper
) {
    val documentCodec = JacksonCodec(objectMapper, DocumentState::class.java)
    companion object {
        const val DOCUMENT = "document"
    }
    fun setDocument(docId: String, value: DocumentState) {
        return redisson.getDocumentJsonBucket(documentCodec, docId).set(value)
    }

    fun setDocumentAsync(docId: String, value: DocumentState): RFuture<Void> {
        return redisson.getDocumentJsonBucket(documentCodec, docId).setAsync(value)
    }

    fun getDocument(docId: String): DocumentState? {
        return redisson.getDocumentJsonBucket(documentCodec, docId).get()
    }

    fun compareAndSet(docId: String, prev: DocumentState, new: DocumentState): Boolean {
        return redisson.getDocumentJsonBucket(documentCodec, docId).compareAndSet(prev, new)
    }

}
fun RedissonClient.getDocumentJsonBucket(codec: JacksonCodec<DocumentState>, docId: String): RBucket<DocumentState> {
    return this.getJsonBucket("${DocumentRepository.DOCUMENT}:${docId}", codec)
}