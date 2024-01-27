package net.kvibews.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.util.JSONPObject
import io.swagger.v3.core.util.Json
import net.kvibews.config.ApplicationProperties
import net.kvibews.model.DocumentPreview
import net.kvibews.model.DocumentState
import org.redisson.api.*
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec
import org.redisson.codec.JacksonCodec
import org.redisson.codec.TypedJsonJacksonCodec
import org.springframework.stereotype.Repository
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit


@Repository
class DocumentRepository(
    private val redisson: RedissonClient,
    private val mapper: ObjectMapper,
    private val properties: ApplicationProperties
) {
    val documentCodec = JacksonCodec(mapper, DocumentState::class.java)
    val docPreviewCodec = JacksonCodec(mapper, DocumentPreview::class.java)

    companion object {
        const val DOCUMENT = "document"
        const val DOCUMENT_PREVIEW = "document:preview"
        const val DOCUMENT_USERS = "document:users"
        const val DOCUMENTS = "documents"
    }

    fun setDocument(docId: String, value: DocumentState, preview: DocumentPreview) {
        val batch = redisson.createBatch(
            BatchOptions.defaults().executionMode(BatchOptions.ExecutionMode.REDIS_WRITE_ATOMIC)
        )
        batch.getJsonBucket("$DOCUMENT:$docId", documentCodec)
            .setAsync(value, properties.operation.staleDocumentExpiry, TimeUnit.SECONDS)
        batch.getJsonBucket("$DOCUMENT_PREVIEW:$docId", docPreviewCodec)
            .setAsync(preview, properties.operation.staleDocumentExpiry, TimeUnit.SECONDS)
        batch.execute()
    }

    fun getDocument(docId: String): DocumentState? {
        return redisson.getDocumentJsonBucket(documentCodec, docId).get()
    }

    fun addActiveUser(docId: String, sessionId: UUID, username: String): Boolean {
        val batch = redisson.createBatch(
            BatchOptions.defaults().executionMode(BatchOptions.ExecutionMode.REDIS_WRITE_ATOMIC)
        )
        val map = redisson.getMap<String, String>("$DOCUMENT_USERS:$docId")
        val success = map.fastPutAsync(sessionId.toString(), username)
        map.expire(Duration.ofSeconds(properties.operation.staleDocumentExpiry))

        batch.execute()
        return success.get()
    }

    fun removeActiveUser(docId: String, sessionId: UUID): Boolean {
        return redisson.getMap<String, String>("$DOCUMENT_USERS:$docId").remove(sessionId.toString()) != null
    }

    fun getDocumentPreview(docId: String): DocumentPreview {
        return redisson.getJsonBucket("$DOCUMENT_PREVIEW:$docId", docPreviewCodec).get()
    }

    fun setDocumentPreview(docId: String, overview: DocumentPreview) {
        redisson.getJsonBucket("$DOCUMENT_PREVIEW:$docId", docPreviewCodec)
            .set(overview, properties.operation.staleDocumentExpiry, TimeUnit.SECONDS)
    }

    fun getContentSnippet(docId: String, snippetLength: Int): String {
        return redisson.getDocumentJsonBucket(documentCodec, docId)
            .get(StringCodec.INSTANCE, "$.content")
            .substring(0, snippetLength)
    }

    fun getDocumentPreviews(): List<DocumentPreview> {
        val keysByPattern = redisson.keys.getKeysByPattern("$DOCUMENT_PREVIEW*").toList()

        return if (keysByPattern.isEmpty())
            emptyList()
        else {
            val futures = mutableListOf<RFuture<DocumentPreview>>()
            val batch = redisson.createBatch()
            keysByPattern.forEach { key ->
                futures.add(batch.getJsonBucket(key, docPreviewCodec).async)
            }
            batch.execute()
            futures.mapNotNull { r -> r.get() }.toList()
        }
    }

    fun getDocumentOverview(docId: String): DocumentPreview? {
        return redisson
            .getMap<String, DocumentPreview>(
                DOCUMENTS,
                getMapCodec<DocumentPreview>(mapper)
            )[docId]
    }

    fun getActiveUsers(docId: String, username: String): List<String> {
        return redisson.getMap<String, String>("$DOCUMENT_USERS:$docId").values.toList()
    }

    fun getActiveUser(docId: String, sessionId: UUID): String? {
        return redisson.getMap<String, String>("$DOCUMENT_USERS:$docId")[sessionId.toString()]
    }

    fun compareAndSet(docId: String, expectedRevision: Int, snapshot: DocumentState, contentPreview: String): Boolean {
        return redisson.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            """
                local r = tonumber(ARGV[1]);
                local currentDoc = redis.call("JSON.GET", KEYS[1]);
                if currentDoc then
                    local docJson = cjson.decode(currentDoc);
                    if docJson.revision == r then
                        redis.call('JSON.SET', KEYS[1], '$', ARGV[2]);
                        redis.call('JSON.SET', KEYS[2], '$.content', ARGV[3]);
                        redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]));
                        redis.call('EXPIRE', KEYS[2], tonumber(ARGV[4])); 
                        return true;
                    else
                        return false;
                    end;
                else
                    return false;
                end;
            """,
            RScript.ReturnType.BOOLEAN,
            listOf("${DOCUMENT}:${docId}", "${DOCUMENT_PREVIEW}:${docId}"),
            expectedRevision.toString(),
            mapper.writeValueAsString(snapshot),
            mapper.writeValueAsString(contentPreview),
            properties.operation.staleDocumentExpiry.toString()
        )
    }

}

fun RedissonClient.getDocumentJsonBucket(
    codec: JacksonCodec<DocumentState>,
    docId: String
): RJsonBucket<DocumentState> {
    return this.getJsonBucket("${DocumentRepository.DOCUMENT}:${docId}", codec)
}

fun <T> getMapCodec(mapper: ObjectMapper) = CompositeCodec(
    StringCodec(),
    TypedJsonJacksonCodec(object : TypeReference<T>() {}, mapper)
)