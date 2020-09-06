package org.hydev

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import okhttp3.Call
import okhttp3.Response
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import org.hydev.devices.Device
import org.hydev.users.User
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.IOException

object Utils {
    fun generateAppToken(): String {
        @Suppress("SpellCheckingInspection")
        val partOneBase = "token://com.coolapk.market/c67ef5943784d09750dcfbb31020f0ab?"

        val currentTime = System.currentTimeMillis() / 1000
        val md5TimeStamp = DigestUtils.md5Hex(currentTime.toString())

        val deviceId = "00000000-0000-0000-0000-000000000000"
        val base64PartOne = Base64.encodeBase64("$partOneBase$md5TimeStamp$$deviceId&com.coolapk.market".toByteArray())

        val partOne = DigestUtils.md5Hex(base64PartOne)
        return "$partOne${deviceId}0x${currentTime.toString(16)}"
    }

    fun initDatabase() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://localhost:3306/coolapk_data"
            driverClassName = "com.mysql.cj.jdbc.Driver"
            username = "root"
            password = "Vanilla1225-"
        }

        val hikariDataSource = HikariDataSource(hikariConfig)
        Database.connect(hikariDataSource)
    }

    fun Call.execRetry(times: Int): Response {
        repeat(times) {
            try {
                return clone().execute()
            } catch (ioe: IOException) {
                if (it == times - 1) throw ioe
            }
        }

        throw AssertionError()
    }
}
