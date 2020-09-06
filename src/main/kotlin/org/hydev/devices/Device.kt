package org.hydev.devices

import org.jetbrains.exposed.sql.Table

object Device : Table() {
    val deviceId = integer("device_id")

    // VARCHAR default length is 80.
    val deviceName = varchar("device_name", 80)

    val followNum = integer("follow_num")
    val feedCommentNum = integer("feed_comment_num")
    val hotNum = integer("hot_num")

    val star_1_count = integer("star_1_count")
    val star_2_count = integer("star_2_count")
    val star_3_count = integer("star_3_count")
    val star_4_count = integer("star_4_count")
    val star_5_count = integer("star_5_count")

    val score = double("3b1b_score")
    override val primaryKey = PrimaryKey(deviceId)
}