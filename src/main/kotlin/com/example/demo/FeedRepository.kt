package com.example.demo

//import com.datastax.oss.driver.api.core.CqlSession
//import com.datastax.oss.driver.api.core.CqlSessionBuilder
//import com.datastax.oss.driver.api.core.cql.*

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toSet
import org.slf4j.LoggerFactory
import java.time.Instant

private enum class Statement {
    FEED_BEFORE,
    CATEGORY_BEFORE,
    USER,
    AUTHOR,
    POST,
    FOLLOWS,
    PENDING,
    RELATIONS,
    FOLLOWERS,
}

suspend fun aDbCall(id: Long): Long? {
    delay(100)
    return 4
}


suspend fun withFlow(s: Set<Long>) = flow<Long> {
    s.forEach { otherUserId ->
        val id = coroutineScope { async { aDbCall(otherUserId) } }
        if (id.await() != null) {
            emit(id.await()!!)
        }
    }
}

suspend fun withMap(s: Set<Long>) =
        s
                .map { o -> GlobalScope.async { aDbCall(o) } }
                .map { o -> runBlocking { o.await() } }
                .toSet()

suspend fun properFlow(s: Set<Long>) =
        s.asFlow()
                .flatMapMerge { id -> flow { emit(aDbCall(id)!!) } }
                .toSet()

fun main() = runBlocking {

//    val sess = CqlSessionBuilder()
//            .addContactPoint(InetSocketAddress("localhost", 9042))
//            .withKeyspace("testkeyspace")
//            .build()
//    val cluster = Cluster.builder().addContactPoint("localhost").withoutJMXReporting().build()
//    val repo = Repository(cluster.connect("testkeyspace"))
//    val serv = FeedService(repo)
//    println(serv.getFeedOfUser(10))
//    println(repo.getFeedItems(10, Instant.now(), 20))

}

@org.springframework.stereotype.Repository
class Repository(private val session: CqlSession) {

    private val statements: Map<Statement, PreparedStatement> = mapOf(
            Statement.FEED_BEFORE to prepareStatement("SELECT postid, authorid, postedat, origauthorid FROM feed_by_user WHERE userid = :u LIMIT :l"),
            Statement.POST to prepareStatement("SELECT * FROM posts_by_id WHERE postid = :p"),
            Statement.USER to prepareStatement("SELECT * FROM users_by_id WHERE userid = :u"),
            Statement.AUTHOR to prepareStatement("SELECT * FROM users_by_id WHERE userid = :u"),
            Statement.FEED_BEFORE to prepareStatement("SELECT postid, authorid, postedat, origauthorid FROM feed_by_user WHERE userid = :u AND postedAt <= :before LIMIT :l"),
            Statement.CATEGORY_BEFORE to prepareStatement("SELECT postid, authorid, origauthorid, createdat FROM posts_by_category WHERE categoryId = :c AND createdAt <= :before LIMIT :l"),
            Statement.RELATIONS to prepareStatement("SELECT * FROM post_relations_by_user WHERE userid = :u AND postid IN :p"),
            Statement.FOLLOWERS to prepareStatement("SELECT followerid FROM available_followers_by_user WHERE userid = :u and followerid IN :a"),
            Statement.PENDING to prepareStatement("SELECT userid FROM pending_requests_by_user WHERE requesterid = :r AND userid IN :ids"),
            Statement.FOLLOWS to prepareStatement("SELECT userid FROM available_followers_by_user WHERE userid = :u AND followerid = :f")
    )
    private val logger = LoggerFactory.getLogger(this.javaClass)
    suspend fun getAuthorProfile(authorId: Long): User? {
//        println("Requested user $authorId")
        val row = execute(Statement.AUTHOR) { it.setLong("u", authorId) }.one() ?: return null
        return Mappers.toAuthorProfile(row)
    }

    suspend fun getUserProfile(userId: Long): User? {
        val row = execute(Statement.USER) { it.setLong("u", userId) }.one() ?: return null
        return Mappers.toUserProfile(row)
    }

    suspend fun getPost(id: Long): Post? {
        val row = execute(Statement.POST) { it.setLong("p", id) }.one() ?: return null
        return Mappers.toPost(row)
    }

    suspend fun getFeedItems(userId: Long, before: Instant, limit: Int): List<PostInfo> {
        val rows = execute(Statement.FEED_BEFORE) {
            it.setLong("u", userId)
            it.setInt("l", limit)
            it.setInstant("before", before)
        }
        val l = rows.map{ Mappers.toPostInfoFromFeedPost(it)}
        return l.all()
    }

    suspend fun getTopCategoryPosts(categoryId: Int, before: Instant, limit: Int): List<PostInfo> {
        val rows = execute(Statement.CATEGORY_BEFORE) {
            it.setInt("c", categoryId)
            it.setInt("l", limit)
            it.setInstant("before", before)
        }
        return rows.map(Mappers::toPostInfoFromCategoryPost).all()
    }

    suspend fun getPostRelation(userId: Long, postIds: List<Long>): List<PostRelation> {
        val rows = execute(Statement.RELATIONS) {
            it.setLong("u", userId)
            it.setList("p", postIds, Long::class.javaObjectType)
        }
        return rows.map(Mappers::toPostRelation).all()
    }

    suspend fun getPostRelations(userId: Long, postIds: List<Long>): Map<Long, PostRelation> {
        val rows = execute(Statement.RELATIONS) {
            it.setLong("u", userId)
            it.setList("p", postIds, Long::class.javaObjectType)
        }
        return rows.map { it.getLong("postid") to Mappers.toPostRelation(it) }.all().toMap()
    }

    suspend fun getUserRelations(userId: Long, otherUserIds: List<Long>): Map<Long, UserRelation> = coroutineScope {
        val followers = async(Dispatchers.IO) {
            execute(Statement.FOLLOWERS) {
                it.setLong("u", userId)
                it.setList("a", otherUserIds, Long::class.javaObjectType)
            }.all().map { it.getLong("followerid") }.toSet()
        }

        val authorsUserFollow = async(Dispatchers.IO) {
            otherUserIds
                    .map { async(Dispatchers.IO) { isFollowing(userId, it) } }
                    .mapNotNull { it.await() }
                    .toSet()
        }

        val pendingRequests = async(Dispatchers.IO) {
            execute(Statement.PENDING) {
                it.setLong("r", userId)
                it.setList("ids", otherUserIds, Long::class.javaObjectType)
            }.all().map { it.getLong("userid") }.toSet()
        }

        otherUserIds.map {
            it to UserRelation(authorsUserFollow.await().contains(it), followers.await().contains(it), pendingRequests.await().contains(it))
        }.toMap()
    }

    private suspend fun isFollowing(userId: Long, otherUserId: Long): Long? = execute(Statement.FOLLOWS) {
        it.setLong("u", otherUserId)
        it.setLong("f", userId)
    }.one()?.getLong("userid")

//    suspend fun getUserRelation(userId: Long, otherUserId: Long): UserRelation? {
//
//    }


    private suspend fun execute(query: Statement, binder: (BoundStatementBuilder) -> Unit): ResultSet {
        val s = statements[query]!!.boundStatementBuilder()
        binder(s)
        return session.execute(s.build())
    }

//    private fun execute(query: BoundStatement): ResultSet = session.execute(query)

    private fun prepareStatement(query: String) =
            this.session.prepare(query)


    private object Mappers {

        fun toPostInfoFromFeedPost(row: Row) = PostInfo(
                id = row.getLong("postid"),
                authorId = row.getLong("authorid"),
                categoryId = null,
                origAuthorId = row.getLong("origauthorid"),
                postedAt = row.getInstant("postedAt")!!
        )

        fun toPostInfoFromCategoryPost(row: Row) = PostInfo(
                id = row.getLong("postid"),
                authorId = row.getLong("authorid"),
                categoryId = null, //row.getInt("categoryid"),
                origAuthorId = row.getLong("origauthorid"),
                postedAt = row.getInstant("createdAt")!!
        )

        fun toPost(row: Row) = Post(
                userId = row.getLong("userid"),
                createdAt = row.getInstant("createdAt")!!,
                postId = row.getLong("postid"),
                content = row.getString("content")!!,
                categoryId = row.getInt("categoryid"),
                mentions = row.getString("mentions"),
                repostId = row.getLong("repostid"),
                replyId = row.getLong("replyid"),
                location = row.getString("location"),
                likeCount = row.getInt("likecount"),
                dislikeCount = row.getInt("dislikecount"),
                repostCount = row.getInt("repostcount"),
                replyCount = row.getInt("replycount"),
                mainPostId = row.getLong("mainpostid"),
                isCanReply = row.getBoolean("canreply"),
                replies = row.getString("replies"),
                viewCount = row.getLong("viewcount"),
                postDetailsViewCount = row.getLong("postdetailsviewcount"),
                postToProfileCount = row.getLong("posttoprofilecount"),
                fileEntries = row.getList("fileentries", String::class.java) ?: listOf(),
                postLink = row.getString("postlink")
        )


        fun toPostRelation(row: Row) = PostRelation(
                userId = row.getLong("userid"),
                postId = row.getLong("postid"),
                reported = row.getBoolean("isReported"),
                liked = row.getBoolean("isliked"),
                disliked = row.getBoolean("isdisliked"),
                reposted = row.getBoolean("isreposted")
        )


        fun toAuthorProfile(row: Row) = User(
                userId = row.getLong("userid"),
                userName = row.getString("username")!!,
                name = row.getString("name")!!,
                profileImg = row.getString("profileImg"),
                reputationLevel = row.getInt("reputationlevel"),
                permissions = row.getInt("permissions"),
                categories = row.getSet("categories", Int::class.javaObjectType) ?: emptySet()
        )

        fun toUserProfile(row: Row) = User(
                userId = row.getLong("userid"),
                userName = row.getString("username")!!,
                name = row.getString("name")!!,
                profileImg = row.getString("profileImg"),
                reputationLevel = row.getInt("reputationlevel"),
                permissions = row.getInt("permissions"),
                blockedUsers = row.getSet("blockedusers", Long::class.javaObjectType) ?: emptySet(),
                unfollowedUsers = row.getSet("unfollowedusers", Long::class.javaObjectType) ?: emptySet(),
                mutedUsers = row.getSet("mutedusers", Long::class.javaObjectType) ?: emptySet(),
                categories = row.getSet("categories", Int::class.javaObjectType) ?: emptySet()
        )
    }
}
