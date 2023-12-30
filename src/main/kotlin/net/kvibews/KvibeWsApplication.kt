package net.kvibews

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KvibeWsApplication

fun main(args: Array<String>) {
	runApplication<KvibeWsApplication>(*args)
}
