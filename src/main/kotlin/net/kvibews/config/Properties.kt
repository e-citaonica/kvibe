package net.kvibews.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@ConfigurationProperties(prefix = "socket-io")
class SocketIOProperties(
    var host: String = "localhost",
    var port: Int = 8080,
    var origin: String? = null
)

@ConfigurationProperties(prefix = "redis")
class RedisProperties(
    var host: String,
    var port: Int,
    var password: String?,
    var timeout: Int,
    var clientName: String?,
    var connectionPoolSize: Int = 25,
    var connectionMinimumIdleSize: Int = 5,
    var dnsMonitoringInterval: Long = 5000
)

@ConfigurationProperties(prefix = "application")
class ApplicationProperties(
    var operation: Operation,
) {
    class Operation(
        var maxNumberOfRetries: Int = 4,
        var staleDocumentExpiry: Int = 7200
    )
}

@Configuration
@EnableConfigurationProperties(value = [SocketIOProperties::class, RedisProperties::class, ApplicationProperties::class])
class PropertiesConfiguration
