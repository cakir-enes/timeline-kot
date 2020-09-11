package com.example.demo

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import java.util.*

private enum class Statement {
    FEED_BEFORE,
    CATEGORY_BEFORE,
    USER,
    AUTHOR,
    POST,
    FOLLOWS,
    PENDING,
    RELATIONS,
    FOLLOWWERS,
}

class Repository(private val session: CqlSession) {

    private val statements: Map<Statement, PreparedStatement> = mapOf(
            Statement.FEED_BEFORE to prepareStatement("SELECT postid, authorid, postedat, origauthorid FROM feed_by_user WHERE userid = :u LIMIT :l"),
            Statement.POST to prepareStatement("SELECT * FROM posts_by_id WHERE postid = :p"),
            Statement.USER to prepareStatement("SELECT * FROM users_by_id WHERE userid = :u"),
            Statement.FEED_BEFORE to prepareStatement("SELECT postid, authorid, postedat, origauthorid FROM feed_by_user WHERE userid = :u AND postedAt <= :before LIMIT :l"),
            Statement.CATEGORY_BEFORE to prepareStatement("SELECT postid, authorid, origauthorid, createdat FROM posts_by_category WHERE categoryId = :c AND createdAt <= :before LIMIT :l"),
            Statement.RELATIONS to prepareStatement("SELECT * FROM post_relations_by_user WHERE userid = :u AND postid IN :p"),
            Statement.FOLLOWWERS to prepareStatement("SELECT followerid FROM available_followers_by_user WHERE userid = :u and followerid IN :a"),
            Statement.PENDING to prepareStatement("SELECT userid FROM pending_requests_by_user WHERE requesterid = :r AND userid IN :ids"),
            Statement.FOLLOWS to prepareStatement("SELECT userid FROM available_followers_by_user WHERE userid = :u AND followerid = :f")
    )

    suspend fun getAuthorProfile(authorId: Long): User? {
        TODO()
    }

    suspend fun getUserProfile(userId: Long): User? {
        TODO()
    }

    suspend fun getFeedItems(userId: Long): List<PostInfo> {
        TODO()
    }

    suspend fun getTopCategoryPosts(categoryId: Long): List<PostInfo> {
        TODO()
    }

    suspend fun getPostRelation(userId: Long, postId: Long): PostRelation? {
        TODO()
    }

    suspend fun getUserRelation(userId: Long, otherUserId: Long): UserRelation? {
        TODO()
    }

    private fun prepareStatement(query: String) =
            this.session.prepare(SimpleStatement.newInstance(query).setIdempotent(true))


    private object Mappers {

        fun toPost(row: Row) = Post(
                userId = row.getLong("userid"),
                createdAt = row.getInstant("createdAt")!!,
                postId = row.getLong("postid"),
                content = row.getString("content")!!,
                categoryId = row.getInt("categoryid"),
                mentions = row.getString("mentions")!!,
                repostId = row.getLong("repostid"),
                replyId = row.getLong("replyid"),
                location = row.getString("location")!!,
                likeCount = row.getInt("likecount"),
                dislikeCount = row.getInt("dislikecount"),
                repostCount = row.getInt("repostcount"),
                replyCount = row.getInt("replycount"),
                mainPostId = row.getLong("mainpostid"),
                isCanReply = row.getBoolean("canreply"),
                replies = row.getString("replies")!!,
                viewCount = row.getLong("viewcount"),
                postDetailsViewCount = row.getLong("postdetailsviewcount"),
                postToProfileCount = row.getLong("posttoprofilecount"),
                fileEntries = row.getList("fileentries", String::class.java)!!,
                postLink = row.getString("postlink")!!
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
                profileImg = row.getString("profileImg")!!,
                reputationLevel = row.getInt("reputationlevel"),
                permissions = row.getInt("permissions"),
                categories = row.getSet("categories", Int::class.java) ?: emptySet()
        )

        fun toUserProfile(row: Row) = User(
                userId = row.getLong("userid"),
                userName = row.getString("username")!!,
                name = row.getString("name")!!,
                profileImg = row.getString("profileImg") ?: "",
                reputationLevel = row.getInt("reputationlevel"),
                permissions = row.getInt("permissions"),
                blockedUsers = row.getSet("blockedusers", Long::class.java) ?: emptySet(),
                unfollowedUsers = row.getSet("unfollowedusers", Long::class.java) ?: emptySet(),
                mutedUsers = row.getSet("mutedusers", Long::class.java) ?: emptySet(),
                categories = row.getSet("categories", Int::class.java) ?: emptySet()
        )
    }
}
