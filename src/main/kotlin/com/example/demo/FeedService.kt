package com.example.demo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.Comparator

@Service
class FeedService(private val repo: Repository) {

    private val dateFormatter = ThreadLocal.withInitial { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone( ZoneId.systemDefault() ) }

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
                s.addAll(categItems.filter { !user.blockedUsers.contains(it.authorId) })
            }
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

        filteredItems.mapNotNull {

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

    private suspend fun getPosts(ids: List<Long>): Map<Long, Post> = coroutineScope {
        ids.map { async { repo.getPost(it) } }
                .mapNotNull { it.await() }
                .map { it.postId to it }.toMap()
    }
}
