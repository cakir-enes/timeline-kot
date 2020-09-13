package com.example.demo

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisSocketConfiguration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
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

//    @Bean("reactive")
//    fun redisConn(): ReactiveRedisConnectionFactory {
//        return LettuceConnectionFactory(RedisSocketConfiguration("/var/run/redis.sock"))
//    }
}