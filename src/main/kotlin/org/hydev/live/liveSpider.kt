package org.hydev.live

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.text.StringEscapeUtils
import org.hydev.Utils
import org.hydev.Utils.execRetry
import org.hydev.dyh.Dyh.creatorUid
import org.hydev.dyh.Dyh.creatorUsername
import org.hydev.dyh.Dyh.dyhTitle
import org.hydev.dyh.Dyh.followNum
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.concurrent.Executors

fun main() {
    Utils.initDatabase()
    transaction { SchemaUtils.create(Live) }

    val fixedThreadPool = Executors.newFixedThreadPool(1024)
    val okHttpClient = OkHttpClient.Builder()
        // Default read time out is 10s.
        .readTimeout(Duration.ofSeconds(130))
        .build()

    // Current Unix Timestamp is 1598863205.
    for (liveId in 1001..1204) run {
        fixedThreadPool.execute {
            val request = Request.Builder()
                .url("https://api.coolapk.com/v6/live/detail?id=$liveId")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("X-App-Id", "com.coolapk.market")
                .addHeader("X-App-Token", Utils.generateAppToken())
                .build()

            val response = okHttpClient.newCall(request).execRetry(13)
            val responseData = response.body?.string()

            val jsonObject = JsonParser.parseString(responseData).asJsonObject
            if (jsonObject.has("status")) {
                val message = jsonObject["message"].asString
                if (message == "直播间不存在") return@execute
                println("$liveId cannot fetch: ${StringEscapeUtils.unescapeJava(responseData)}")
                return@execute
            }

            val jsonObjectData = jsonObject["data"].asJsonObject
            transaction {
                Live.insert {
                    it[Live.liveId] = liveId
                    it[title] = jsonObjectData["title"].asString

                    it[discussNum] = jsonObjectData["discuss_num"].asInt
                    it[visitNum] = jsonObjectData["visit_num"].asInt
                }
            }
        }
    }
}