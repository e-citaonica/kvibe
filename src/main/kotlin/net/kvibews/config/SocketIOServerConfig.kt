package net.kvibews.config

import com.corundumstudio.socketio.SocketIOServer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class SocketIOServerConfig(val socketIOProperties: SocketIOProperties) {

    @Bean
    fun socketIOServer(): SocketIOServer {
        val config = com.corundumstudio.socketio.Configuration()
        config.hostname = socketIOProperties.host
        config.port = socketIOProperties.port
        return SocketIOServer(config)
    }
}