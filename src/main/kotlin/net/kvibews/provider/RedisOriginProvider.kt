package net.kvibews.provider

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class RedisOriginProvider {
    val value: String = UUID.randomUUID().toString()
}