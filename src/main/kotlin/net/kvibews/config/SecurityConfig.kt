package net.kvibews.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource = CorsConfiguration()
        .apply { allowedOrigins = listOf("http://localhost:4200") }
        .apply { allowedMethods = listOf("*") }
        .apply { allowedHeaders = listOf("*") }
        .let { corsConfig ->
            UrlBasedCorsConfigurationSource()
                .apply { registerCorsConfiguration("/**", corsConfig) }
        }

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors { disable() }
            csrf { }
            authorizeRequests {
                authorize("/**", permitAll)
            }
        }
        return http.build()
    }
}
