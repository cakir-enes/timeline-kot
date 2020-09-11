package com.example.demo

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*
import kotlin.Comparator

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

class FeedService(val repo: Repository) {

    suspend fun getFeedOfUser(userId: Long): List<FeedPost> {

        val user = repo.getUserProfile(userId) ?: throw Exception()
        val feedItems = repo.getFeedItems(userId, Instant.now(), 20)
        val subbedCategoryItems = getCategoryPosts(user.categories, Instant.now(), 20)

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
        val authorIds = filteredItems.fold(mutableListOf<Long>()) { ids, info ->
            ids.add(info.authorId)
            if (info.origAuthorId != null && info.origAuthorId != 0L) {
                ids.add(info.origAuthorId)
            }
            ids
        }
        val userRels = repo.getUserRelations(userId, authorIds)

        return filteredItems.map {
            val post = posts[it.id] ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
            val authorRel = userRels[it.authorId] ?: UserRelation(false, false, false)
            val postRel = postRels[it.id] ?: PostRelation(it.authorId, it.id, false, false, false, false)
            val opRel = if (it.origAuthorId != null) userRels[it.origAuthorId]
                    ?: UserRelation(false, false, false) else null
            FeedPost.from(post, authorRel, postRel, opRel)
        }
    }


    private suspend fun getCategoryPosts(categories: Collection<Int>, before: Instant, limit: Int): Flow<List<PostInfo>> = coroutineScope {
        flow {
            categories
                    .map { id -> async { repo.getTopCategoryPosts(id, before, limit) } }
                    .map { it.await() }
                    .forEach { emit(it) }
        }
    }

    private suspend fun getPosts(ids: List<Long>): Map<Long, Post> = coroutineScope {
        ids.map { async { repo.getPost(it) } }
                .mapNotNull { it.await() }
                .map { it.postId to it }.toMap()
    }
}

data class FeedPost(val id: Long) {
    companion object {
        suspend fun from(post: Post, authorRel: UserRelation, postRel: PostRelation, opRel: UserRelation?): FeedPost {
            return FeedPost(post.postId)
        }
    }
}
