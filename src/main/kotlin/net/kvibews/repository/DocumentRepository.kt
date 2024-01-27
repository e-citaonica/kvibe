package net.kvibews.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.config.ApplicationProperties
import net.kvibews.model.DocumentOverview
import net.kvibews.model.DocumentState
import org.redisson.api.*
import org.redisson.client.codec.StringCodec
import org.redisson.codec.CompositeCodec
import org.redisson.codec.JacksonCodec
import org.redisson.codec.TypedJsonJacksonCodec
import org.springframework.stereotype.Repository
import java.util.UUID


@Repository
class DocumentRepository(
    private val redisson: RedissonClient,
    private val mapper: ObjectMapper,
    private val properties: ApplicationProperties
) {
    val documentCodec = JacksonCodec(mapper, DocumentState::class.java)

    companion object {
        const val DOCUMENT = "document"
        const val USERS_DOCUMENT = "users:document"
        const val DOCUMENTS = "documents"
    }

    fun setDocument(docId: String, value: DocumentState) {
        return redisson.getDocumentJsonBucket(documentCodec, docId).set(value)
    }

    fun getDocument(docId: String): DocumentState? {
        return redisson.getDocumentJsonBucket(documentCodec, docId).get()
    }

    fun addActiveUser(docId: String, sessionId: UUID, username: String): Boolean {
        return redisson.getMap<String, String>("$USERS_DOCUMENT:$docId").fastPut(sessionId.toString(), username)
    }

    fun removeActiveUser(docId: String, sessionId: UUID): Boolean {
        return redisson.getMap<String, String>("$USERS_DOCUMENT:$docId").remove(sessionId.toString()) != null
    }

    fun getDocumentOverviews(): List<DocumentOverview> {
        return redisson
            .getMap<String, DocumentOverview>(
                DOCUMENTS,
                getMapCodec<DocumentOverview>(mapper)
            )
            .values.toList()
    }

    fun setDocumentOverview(docId: String, overview: DocumentOverview): Boolean {
        return redisson
            .getMap<String, DocumentOverview>(
                DOCUMENTS,
                getMapCodec<DocumentOverview>(mapper)
            )
            .put(docId, overview) != null
    }

    fun getActiveUsersCount(docId: String): Int? {
        return redisson.getMap<String, String>("$USERS_DOCUMENT:$docId").size
    }

    fun getDocuments(): List<DocumentState> {
        val pattern = "$DOCUMENT*"
        val keysByPattern = redisson.keys.getKeysByPattern("$DOCUMENT*")
        val documents: MutableList<DocumentState> = mutableListOf()
        keysByPattern.forEach {
            val document = redisson.getDocumentJsonBucket(documentCodec, it.substring(pattern.length)).get()
            if (document != null) {
                documents.add(document)
            }
        }
        return documents.toList()
    }

    fun getDocumentOverview(docId: String): DocumentOverview? {
        return redisson
            .getMap<String, DocumentOverview>(
                DOCUMENTS,
                getMapCodec<DocumentOverview>(mapper)
            )[docId]
    }

    fun getActiveUsers(docId: String, username: String): List<String> {
        return redisson.getMap<String, String>("$USERS_DOCUMENT:$docId").values.toList()
    }

    fun getActiveUser(docId: String, sessionId: UUID): String? {
        return redisson.getMap<String, String>("$USERS_DOCUMENT:$docId")[sessionId.toString()]
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
                        redis.call("EXPIRE", KEYS[1], tonumber(ARGV[3]));
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
            mapper.writeValueAsString(snapshot),
            properties.operation.staleDocumentExpiry.toString()
        )
    }

}

fun RedissonClient.getDocumentJsonBucket(codec: JacksonCodec<DocumentState>, docId: String): RBucket<DocumentState> {
    return this.getJsonBucket("${DocumentRepository.DOCUMENT}:${docId}", codec)
}

fun <T> getMapCodec(mapper: ObjectMapper) = CompositeCodec(
    StringCodec(),
    TypedJsonJacksonCodec(object : TypeReference<T>() {}, mapper)
)