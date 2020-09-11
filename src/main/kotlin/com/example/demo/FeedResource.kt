package com.example.demo

import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.*

@RestController
@RequestMapping("/api")
class FeedResource(private val feedService: FeedService) {

    private val log = LoggerFactory.getLogger(FeedResource::class.java)

    @GetMapping("/feeds-by-user-id/{id}/{count}")
    suspend fun feedByUserId(
            @PathVariable id: Long,
            @PathVariable count: Int,
            @RequestParam(value = "beforeDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") beforeDate: Instant?): List<FeedPost?>? = coroutineScope {
        log.debug("REST request to delete Feed : {}", id)
        val s = feedService.getFeedOfUser(id, count, beforeDate ?: Instant.now())
        return@coroutineScope s

    }
}