package org.hydev.live

import org.jetbrains.exposed.sql.Table

object Live : Table() {
    val liveId = integer("live_id")

    // VARCHAR default length is 80.
    val title = varchar("live_title", 80)

    val discussNum = integer("discuss_num")
    val visitNum = integer("visit_num")

    override val primaryKey = PrimaryKey(liveId)
}