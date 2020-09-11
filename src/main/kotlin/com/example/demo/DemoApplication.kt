package com.example.demo

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.format.DateTimeFormatter

@SpringBootApplication
class DemoApplication

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}

data class FeedUser(
        val id: Long,
        val name: String,
        val username: String,
        val profileImage: String?,
        val permissions: Int,
        val reputationLevel: Int,
        val isPending: Boolean,
        val followingYou: Boolean,
        val IFollowed: Boolean) {
    companion object {

        fun from(user: User, relation: UserRelation): FeedUser {
            val feedUser = FeedUser(
                    id = user.userId,
                    followingYou = relation.authorFollowsUser,
                    username = user.userName,
                    IFollowed = relation.userFollowsAuthor,
                    isPending = relation.pending,
                    name = user.name,
                    reputationLevel = user.reputationLevel,
                    profileImage = user.profileImg,
                    permissions = user.permissions
            )
            return feedUser
        }
    }
}

data class FeedPost(
        val id: Long,
        val post: String,
        val kafeId: Int?,
        val mentions: String?,
        val repostId: Long,
        val replyId: Long,
        val location: String?,
        val isConfirmed: Boolean,
        val likeCount: Int,
        val dislikeCount: Int,
        val repostCount: Int,
        val replyCount: Int,
        val createdAt: String,
        val user: FeedUser,
        val repostUser: List<FeedUser>?,
        val kafeDesc: FeedKafeDesc?,

        @get:JsonProperty("isReported")
        val isReported: Boolean,

        @get:JsonProperty("isLiked")
        val isLiked: Boolean,

        @get:JsonProperty("isDisliked")
        val isDisliked: Boolean,

        @get:JsonProperty("isReposted")
        val isReposted: Boolean,

        @JsonSerialize(using = StringToJson::class)
        val postLink: String?,

        @JsonSerialize(using = StringListToJson::class)
        val fileEntries: List<String>?,
        val main_post_id: Long?,
        val isCan_reply: Boolean,
        val replies: String?,
        val beforeDate: String
) {
    companion object {
        fun createOriginalPost(post: Post, user: FeedUser, relation: PostRelation, formatter: DateTimeFormatter): FeedPost {
            val feedPost = FeedPost(
                    id = post.postId,
                    post = post.content,
                    kafeId = post.categoryId,
                    mentions = post.mentions,
                    kafeDesc = if (post.categoryId != null) FeedKafeDesc.forCategory(post.categoryId) else null,
                    repostId = post.repostId,
                    replyId = post.replyId,
                    location = post.location,
                    isConfirmed = true,
                    likeCount = post.likeCount,
                    dislikeCount = post.dislikeCount,
                    repostCount = post.repostCount,
                    replyCount = post.repostCount,
                    createdAt = formatter.format(post.createdAt),
                    user = user,
                    isReported = relation.reported,
                    isLiked = relation.liked,
                    isDisliked = relation.disliked,
                    isReposted = relation.reposted,
                    postLink = post.postLink,
                    isCan_reply = post.isCanReply,
                    replies = post.replies,
                    fileEntries = post.fileEntries,
                    beforeDate = formatter.format(post.createdAt),
                    main_post_id = post.mainPostId,
                    repostUser = null
            )
            return feedPost
        }

        fun createRepost(post: Post, reposter: FeedUser, op: FeedUser, relation: PostRelation, formatter: DateTimeFormatter): FeedPost {
            val feedPost = FeedPost(
                    id = post.postId,
                    post = post.content,
                    kafeId = post.categoryId,
                    mentions = post.mentions,
                    kafeDesc = if (post.categoryId != null) FeedKafeDesc.forCategory(post.categoryId) else null,
                    repostId = post.repostId,
                    replyId = post.replyId,
                    location = post.location,
                    isConfirmed = true,
                    likeCount = post.likeCount,
                    dislikeCount = post.dislikeCount,
                    repostCount = post.repostCount,
                    replyCount = post.repostCount,
                    createdAt = formatter.format(post.createdAt),
                    user = reposter,
                    isReported = relation.reported,
                    isLiked = relation.liked,
                    isDisliked = relation.disliked,
                    isReposted = relation.reposted,
                    postLink = post.postLink,
                    isCan_reply = post.isCanReply,
                    replies = post.replies,
                    fileEntries = post.fileEntries,
                    beforeDate = formatter.format(post.createdAt),
                    main_post_id = post.mainPostId,
                    repostUser = listOf(reposter)
            )
            return feedPost
        }
    }
}
