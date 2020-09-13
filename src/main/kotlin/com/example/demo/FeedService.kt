package com.example.demo

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.Range
import org.springframework.data.redis.connection.RedisZSetCommands
import org.springframework.data.redis.core.*
import org.springframework.stereotype.Service
import reactor.util.function.Tuple2
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalUnit
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Comparator
import kotlin.math.max

@Service
class FeedService(private val repo: Repository, private val redisTemplate: ReactiveRedisTemplate<String, String>) {

    private val dateFormatter = ThreadLocal.withInitial { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()) }
    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
    private val zset: ReactiveZSetOperations<String, String> = redisTemplate.opsForZSet()

    suspend fun getFeedOfUser(userId: Long, limit: Int, before: Instant): List<FeedPost> = supervisorScope {

        val userF = async(Dispatchers.IO) { repo.getUserProfile(userId) ?: throw RuntimeException("User NOT FOUND") }
        val feedItemsF = async(Dispatchers.IO) { repo.getFeedItems(userId, Instant.now(), 20) }

        val user = userF.await()
        val subbedCategoryItems = getCategoryPosts(user.categories, Instant.now(), 20)

        val feedItems = feedItemsF.await()
        val filteredItems = suspend {
            val s: SortedSet<PostInfo> = sortedSetOf(Comparator { o1, o2 -> if (o1.postedAt.isAfter(o2.postedAt)) 1 else -1 })
            s.addAll(feedItems.filter { !user.blockedUsers.contains(it.authorId) })
            subbedCategoryItems.collect { categItems ->
                val infos = categItems
                s.addAll(infos.filter { !user.blockedUsers.contains(it.authorId) })
            }
//            subbedCategoryItems.collect { (categId, categItems) ->
//                val infos = if (categItems.isEmpty())
//                    getAndCacheCategoryPosts(categId, before, limit)
//                else
//                    categItems
//                s.addAll(infos.filter { !user.blockedUsers.contains(it.authorId) })
//            }
            s.take(20)
        }()

        val postIds = filteredItems.map { it.id }
        val posts = getPosts(postIds)
        val postRels = repo.getPostRelations(userId, postIds)
        val authorIds = filteredItems.fold(mutableSetOf<Long>()) { ids, info ->
            ids.add(info.authorId)
            if (info.origAuthorId != null && info.origAuthorId != 0L) {
                ids.add(info.origAuthorId)
            }
            ids
        }.toList()
        val authorsF = async(Dispatchers.IO) { getAuthorProfiles(authorIds) }

        val userRelsF = async(Dispatchers.IO) { repo.getUserRelations(userId, authorIds) }

        val authors = authorsF.await()
        val userRels = userRelsF.await()

        val s = filteredItems.mapNotNull {

            val post = posts[it.id] ?: return@mapNotNull null
            val postRel = postRels[it.id] ?: PostRelation(it.authorId, it.id)

            val author = authors[it.authorId] ?: return@mapNotNull null
            val authorRel = userRels[it.authorId] ?: UserRelation()

            val op = if (it.isRepost) authors[it.origAuthorId] ?: return@mapNotNull null else null
            val opRel = if (it.isRepost) userRels[it.origAuthorId] ?: UserRelation() else null

            if (it.isRepost) {
                FeedPost.createRepost(post, FeedUser.from(author, authorRel), FeedUser.from(op!!, opRel!!), postRel, dateFormatter.get())
            } else {
                FeedPost.createOriginalPost(post, FeedUser.from(author, authorRel), postRel, dateFormatter.get())
            }
        }
        s
    }

    private suspend fun getAuthorProfiles(authorIds: List<Long>): Map<Long, User> = coroutineScope {
        authorIds
                .map { async { repo.getAuthorProfile(it) } }
                .mapNotNull { it.await() }
                .map { it.userId to it }
                .toMap()
    }

    private suspend fun getCategoryPosts(categories: Collection<Int>, before: Instant, limit: Int): Flow<List<PostInfo>> =
            flow {
                categories
                        .map { id -> coroutineScope { async { repo.getTopCategoryPosts(id, before, limit) } } }
                        .map { it.await() }
                        .forEach { emit(it) }
            }

    private suspend fun getAndCacheCategoryPosts(categoryId: Int, before: Instant, limit: Int): List<PostInfo> {
        val infos = repo.getTopCategoryPosts(categoryId, before, limit)
        val ser = infos.map { DefaultTypedTuple(mapper.writeValueAsString(it), it.postedAt.toEpochMilli().toDouble()) }
        zset.addAll("category:$categoryId", ser).subscribe()
        redisTemplate.expire("category:$categoryId", Duration.ofMinutes(1)).subscribe()
        return infos
    }

    private suspend fun getCategoryPostsFromCache(categories: Collection<Int>, before: Instant, limit: Int): Flow<Pair<Int, List<PostInfo>>> =
            categories
                    .asFlow()
                    .flatMapMerge(concurrency = max(categories.size, 1)) { id ->
                        val range = Range.closed(Double.MIN_VALUE, before.toEpochMilli().toDouble())
                        flow {
                            emit(id to zset.reverseRangeByScoreAsFlow("category:$id", range, RedisZSetCommands.Limit.limit().count(limit))
                                    .map { mapper.readValue(it, PostInfo::class.java) }
                                    .toList())
                        }
                    }

    private suspend fun getPosts(ids: List<Long>): Map<Long, Post> = coroutineScope {
        ids.map { async { repo.getPost(it) } }
                .mapNotNull { it.await() }
                .map { it.postId to it }.toMap()
    }
}
