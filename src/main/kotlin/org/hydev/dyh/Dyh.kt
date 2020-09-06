package org.hydev.dyh

import org.jetbrains.exposed.sql.Table

object Dyh : Table() {
    val dyhId = integer("dyh_id")

    // VARCHAR default length is 80.
    val dyhTitle = varchar("dyh_title", 80)

    val creatorUid = integer("creator_uid")
    val creatorUsername = varchar("creator_username", 80)

    val followNum = integer("follow_num")
    override val primaryKey = PrimaryKey(dyhId)
}