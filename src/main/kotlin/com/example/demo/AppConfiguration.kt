package com.example.demo

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.Session
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfiguration {
    @Bean
    fun scyllaSession(): Session {
        val cluster = Cluster.builder().addContactPoint("localhost").withoutJMXReporting().build()
        return cluster.connect("testkeyspace")
    }
}