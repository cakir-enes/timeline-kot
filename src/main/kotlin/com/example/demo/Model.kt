package com.example.demo

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Instant

data class User(
        val userId: Long,
        val userName: String,
        val name: String,
        val profileImg: String?,
        val reputationLevel: Int,
        val blockedUsers: Set<Long> = emptySet(),
        val mutedUsers: Set<Long> = emptySet(),
        val unfollowedUsers: Set<Long> = emptySet(),
        val categories: Set<Int> = emptySet(),
        val permissions: Int = 5
)

data class Post(
        val postId: Long,
        val userId: Long,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
        val createdAt: Instant,
        val content: String,
        val categoryId: Int?,
        val mentions: String?,
        val repostId: Long,
        val replyId: Long,
        val location: String?,
        val likeCount: Int,
        val dislikeCount: Int,
        val repostCount: Int,
        val replyCount: Int,
        val mainPostId: Long,
        val isCanReply: Boolean,
        val replies: String?,
        val fileEntries: List<String>,
        val postLink: String?,
        val viewCount: Long,
        val postDetailsViewCount: Long,
        val postToProfileCount: Long
)

data class UserRelation(
        val userFollowsAuthor: Boolean = false,
        val authorFollowsUser: Boolean = false,
        val pending: Boolean = false
)


data class PostRelation(
        val userId: Long,
        val postId: Long,
        val reported: Boolean = false,
        val liked: Boolean = false,
        val disliked: Boolean = false,
        val reposted: Boolean = false
)

data class PostInfo(
        val id: Long,
        val authorId: Long,
        val postedAt: Instant,
        val categoryId: Int?,
        val origAuthorId: Long?
) {
        val isRepost: Boolean = origAuthorId == null || origAuthorId != 0L
}