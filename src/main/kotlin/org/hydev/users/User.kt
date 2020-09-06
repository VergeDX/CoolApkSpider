package org.hydev.users

import org.jetbrains.exposed.sql.Table

object User : Table() {
    val userId = integer("user_id")

    val level = integer("level")
    val experience = integer("experience")

    // VARCHAR default length is 80.
    val displayUsername = varchar("display_username", 80)

    val feedNum = integer("feed_num")
    val followNum = integer("follow_num")
    val fansNum = integer("fans_num")
    val beLikeNum = integer("be_like_num")

    override val primaryKey = PrimaryKey(userId)
}
