package net.kvibews.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@ConfigurationProperties(prefix = "socket-io")
data class SocketIOProperties(
    var host: String = "localhost",
    var port: Int = 8080
)

@Configuration
@EnableConfigurationProperties(SocketIOProperties::class)
class Properties {
    @Bean
    fun socketIOProperties(): SocketIOProperties {
        return SocketIOProperties()
    }
}