package org.hydev.users

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.text.StringEscapeUtils
import org.hydev.Utils
import org.hydev.Utils.execRetry
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException
import java.time.Duration
import java.util.concurrent.Executors

// Current Unix Timestamp is 1598684249.
const val USER_MAX = 3924898
const val USER_SPACE = "https://api.coolapk.com/v6/user/space?uid="

fun main() {
    Utils.initDatabase()
    transaction { SchemaUtils.create(User) }

    val fixedThreadPool = Executors.newFixedThreadPool(2048)
    val okHttpClient = OkHttpClient.Builder()
        // Default read time out is 10s.
        .readTimeout(Duration.ofSeconds(130))
        .build()

    // 10002 is first user, in my run, (646212..648022) fail.
    for (userId in 10002..USER_MAX) run {
        fixedThreadPool.execute {
            val request = Request.Builder()
                .url(USER_SPACE + userId)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("X-App-Id", "com.coolapk.market")
                .addHeader("X-App-Token", Utils.generateAppToken())
                .build()

            lateinit var response: Response
            try {
                response = okHttpClient.newCall(request).execRetry(13)
            } catch (ioe: IOException) {
                println(" * Error in get $userId, message: ${ioe.message}")
                return@execute
            }

            val responseData = response.body?.string()

            // TODO: 2020/8/30  java.lang.IllegalStateException: Not a JSON Object: null
            val rootJsonObject = JsonParser.parseString(responseData).asJsonObject ?: return@execute

            // json has status means user is not exist.
            if (rootJsonObject.has("status")) {
                if (rootJsonObject["message"].asString != "用户不存在")
                    println("$userId cannot fetch: ${StringEscapeUtils.unescapeJava(responseData)}")
                return@execute
            }

            val jsonData = rootJsonObject["data"].asJsonObject
            transaction {
                User.insert {
                    it[User.userId] = userId

                    it[level] = jsonData["level"].asInt
                    it[experience] = jsonData["experience"].asInt
                    it[displayUsername] = StringEscapeUtils
                        .unescapeJava(jsonData["displayUsername"].asString)

                    it[feedNum] = jsonData["feed"].asInt
                    it[followNum] = jsonData["follow"].asInt
                    it[fansNum] = jsonData["fans"].asInt
                    it[beLikeNum] = jsonData["be_like_num"].asInt
                }
            }
        }
    }
}
