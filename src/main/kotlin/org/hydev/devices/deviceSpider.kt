package org.hydev.devices

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
    transaction { SchemaUtils.create(Device) }

    val fixedThreadPool = Executors.newFixedThreadPool(1024)
    val okHttpClient = OkHttpClient.Builder()
        // Default read time out is 10s.
        .readTimeout(Duration.ofSeconds(130))
        .build()

    // Current Unix Timestamp is 1598843361.
    for (deviceId in 1001..2138) run {
        fixedThreadPool.execute {
            val request = Request.Builder()
                .url("https://api.coolapk.com/v6/product/detail?id=$deviceId")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("X-App-Id", "com.coolapk.market")
                .addHeader("X-App-Token", Utils.generateAppToken())
                .build()

            val response = okHttpClient.newCall(request).execRetry(13)
            val responseData = response.body?.string()

            val jsonObject = JsonParser.parseString(responseData).asJsonObject
            if (jsonObject.has("status")) {
                println("$deviceId cannot fetch: ${StringEscapeUtils.unescapeJava(responseData)}")
                return@execute
            }

            val jsonObjectData = jsonObject["data"].asJsonObject
            transaction {
                Device.insert {
                    it[Device.deviceId] = deviceId
                    it[deviceName] = jsonObjectData["title"].asString

                    it[followNum] = jsonObjectData["follow_num"].asInt
                    it[feedCommentNum] = jsonObjectData["feed_comment_num"].asInt
                    it[hotNum] = jsonObjectData["hot_num"].asInt

                    val oneStartCount = jsonObjectData["star_1_count"].asInt
                    val twoStartCount = jsonObjectData["star_2_count"].asInt
                    val threeStartCount = jsonObjectData["star_3_count"].asInt
                    val fourStartCount = jsonObjectData["star_4_count"].asInt
                    val fiveStartCount = jsonObjectData["star_5_count"].asInt

                    it[star_1_count] = oneStartCount
                    it[star_2_count] = twoStartCount
                    it[star_3_count] = threeStartCount
                    it[star_4_count] = fourStartCount
                    it[star_5_count] = fiveStartCount

                    val sum = oneStartCount + twoStartCount + threeStartCount + fourStartCount + fiveStartCount
                    val score = if (sum == 0) 0.0 else
                        ((oneStartCount + 1) * 1 +
                                twoStartCount * 2 +
                                threeStartCount * 3 +
                                fourStartCount * 4 +
                                (fiveStartCount + 1) * 5).toDouble() / (sum + 2)
                    it[Device.score] = String.format("%.3f", score).toDouble()
                }
            }
        }
    }
}
