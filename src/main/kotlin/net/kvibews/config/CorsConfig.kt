package net.kvibews.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

@Configuration
class CorsConfig {
    @Bean
    fun corsFilter(): CorsFilter {
        val source = UrlBasedCorsConfigurationSource()
        val config = CorsConfiguration()

        config.addAllowedOrigin("*")

        // Allow credentials (if your API uses cookies, sessions, or HTTP authentication)
        config.allowCredentials = true

        // Allow all headers, methods, and expose certain headers if needed
        config.addAllowedHeader("*")
        config.addAllowedMethod("*")

        //config.addExposedHeader("your-exposed-header");
        source.registerCorsConfiguration("/**", config)

        return CorsFilter(source)
    }
}
