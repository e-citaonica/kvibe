package net.kvibews.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedissonConfig(val props: RedisProperties) {

    @Bean
    fun redissonClient(): RedissonClient {
        val config = Config()
        config.useSingleServer().apply {
            address = "redis://${props.host}:${props.port}"
            password = props.password
            timeout = props.timeout
            clientName = props.clientName
            connectionPoolSize = props.connectionPoolSize
            connectionMinimumIdleSize = props.connectionMinimumIdleSize
        }
        return Redisson.create(config)
    }
}