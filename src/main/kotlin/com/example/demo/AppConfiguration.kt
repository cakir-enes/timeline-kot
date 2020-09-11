package com.example.demo

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.InetSocketAddress

@Configuration
class AppConfiguration {
    @Bean
    fun scyllaSession(): CqlSession {
        val sess = CqlSessionBuilder()
                .addContactPoint(InetSocketAddress("localhost", 9042))
                .withKeyspace("testkeyspace")
                .withLocalDatacenter("datacenter1")
                .build()
        return sess
    }
}