package net.kvibews.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.kvibews.model.DocumentState
import org.redisson.api.*
import org.redisson.client.codec.LongCodec
import org.redisson.client.codec.StringCodec
import org.redisson.codec.JacksonCodec
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
class DocumentRepository(
    private val redisson: RedissonClient,
    private final val objectMapper: ObjectMapper
) {
    val documentCodec = JacksonCodec(objectMapper, DocumentState::class.java)

    companion object {
        const val DOCUMENT = "document"
        const val DOCUMENT_USERS = "document:users"
    }

    fun setDocument(docId: String, value: DocumentState) {
        return redisson.getDocumentJsonBucket(documentCodec, docId).set(value)
    }

    fun getDocument(docId: String): DocumentState? {
        return redisson.getDocumentJsonBucket(documentCodec, docId).get()
    }

    fun addActiveUser(docId: String, sessionId: UUID, username: String): Boolean {
        return redisson.getMap<UUID, String>("$DOCUMENT_USERS:$docId").fastPut(sessionId, username)
    }

    fun removeActiveUser(docId: String, sessionId: UUID): Boolean {
        return redisson.getMap<String, String>("$DOCUMENT_USERS:$docId").remove(sessionId.toString()) != null
    }

    fun getActiveUsers(docId: String, username: String): List<String> {
        return redisson.getMap<String, String>("$DOCUMENT_USERS:$docId").values.toList()
    }

    fun getActiveUser(docId: String, sessionId: UUID): String? {
        return redisson.getMap<String, String>("$DOCUMENT_USERS:$docId")[sessionId.toString()]
    }

    fun compareAndSet(docId: String, expectedRevision: Int, snapshot: DocumentState): Boolean {
        return redisson.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            """
                local r = tonumber(ARGV[1]);
                local currentDoc = redis.call("JSON.GET", KEYS[1]);
                if currentDoc then
                    local docJson = cjson.decode(currentDoc);
                    if docJson.revision == r then
                        redis.call("JSON.SET", KEYS[1], "$", ARGV[2]);
                        return true;
                    else
                        return false;
                    end;
                else
                    return false;
                end;
            """,
            RScript.ReturnType.BOOLEAN,
            listOf("${DOCUMENT}:${docId}"),
            expectedRevision.toString(),
            objectMapper.writeValueAsString(snapshot)
        )
    }

}

fun RedissonClient.getDocumentJsonBucket(codec: JacksonCodec<DocumentState>, docId: String): RBucket<DocumentState> {
    return this.getJsonBucket("${DocumentRepository.DOCUMENT}:${docId}", codec)
}