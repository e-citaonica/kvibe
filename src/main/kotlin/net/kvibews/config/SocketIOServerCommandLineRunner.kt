package net.kvibews.config

import com.corundumstudio.socketio.SocketIOServer
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class SocketIOServerCommandLineRunner(val socketIOServer: SocketIOServer) : CommandLineRunner {

    override fun run(args: Array<String>) {
        socketIOServer.start()
    }
}