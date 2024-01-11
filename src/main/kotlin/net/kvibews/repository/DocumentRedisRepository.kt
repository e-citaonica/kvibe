package net.kvibews.repository

import com.fasterxml.jackson.databind.ObjectMapper
import net.kvibews.model.Document
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository


@Repository
class DocumentRedisRepository(
    private val mapper: ObjectMapper,
    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        const val KEY_PREFIX = "DOCUMENT:"
    }
    fun setDocument(docId: String, value: Document) {
        return redisTemplate.opsForValue().set("$KEY_PREFIX:$docId", mapper.writeValueAsString(value));
    }

    fun getDocument(docId: String): Document? {
        return mapper.readValue(redisTemplate.opsForValue().get("$KEY_PREFIX:$docId"), Document::class.java)
    }
}