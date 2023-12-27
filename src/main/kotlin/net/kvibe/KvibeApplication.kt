package net.kvibe

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KvibeApplication

fun main(args: Array<String>) {
	runApplication<KvibeApplication>(*args)
}
