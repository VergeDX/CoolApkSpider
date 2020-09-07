package org.hydev.dyh

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.text.StringEscapeUtils
import org.hydev.Utils
import org.hydev.Utils.execRetry
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.concurrent.Executors

fun main() {
    Utils.initDatabase()
    transaction { SchemaUtils.create(Dyh) }

    val fixedThreadPool = Executors.newFixedThreadPool(1024)
    val okHttpClient = OkHttpClient.Builder()
        // Default read time out is 10s.
        .readTimeout(Duration.ofSeconds(130))
        .build()

    // Current Unix Timestamp is 1598863205.
    for (dyhId in 1008..4715) run {
        fixedThreadPool.execute {
            val request = Request.Builder()
                .url("https://api.coolapk.com/v6/dyh/detail?dyhId=$dyhId")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("X-App-Id", "com.coolapk.market")
                .addHeader("X-App-Token", Utils.generateAppToken())
                .build()

            val response = okHttpClient.newCall(request).execRetry(13)
            val responseData = response.body?.string()

            val jsonObject = JsonParser.parseString(responseData).asJsonObject
            if (jsonObject.has("status")) {
                val message = jsonObject["message"].asString
                if (message == "该看看号不存在") return@execute
                println("$dyhId cannot fetch: ${StringEscapeUtils.unescapeJava(responseData)}")
                return@execute
            }

            val jsonObjectData = jsonObject["data"].asJsonObject
            transaction {
                Dyh.insert {
                    it[Dyh.dyhId] = dyhId
                    it[dyhTitle] = jsonObjectData["title"].asString

                    it[creatorUid] = jsonObjectData["uid"].asInt
                    it[creatorUsername] = jsonObjectData["username"].asString
                    it[followNum] = jsonObjectData["follownum"].asInt
                }
            }
        }
    }

    fixedThreadPool.shutdown()
}