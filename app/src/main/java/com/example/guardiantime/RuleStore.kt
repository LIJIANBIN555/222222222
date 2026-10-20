package com.example.guardiantime

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalTime

object RuleStore {
    private const val PREFS = "guardian_prefs"
    private const val PIN_HASH = "pin_hash"
    private const val PIN_SALT = "pin_salt"
    private const val START = "start_time"
    private const val END = "end_time"
    private const val ENABLED = "rule_enabled"
    private const val LOG = "event_log"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun hasPin(context: Context) = prefs(context).contains(PIN_HASH)

    fun setPin(context: Context, pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = digest(salt, pin)
        prefs(context).edit()
            .putString(PIN_SALT, salt.joinToString("") { "%02x".format(it) })
            .putString(PIN_HASH, hash)
            .apply()
        log(context, "管理员 PIN 已设置或更新")
    }

    fun verifyPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val saltHex = p.getString(PIN_SALT, null) ?: return false
        val expected = p.getString(PIN_HASH, null) ?: return false
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val actual = digest(salt, pin)
        return MessageDigest.isEqual(
            expected.toByteArray(Charsets.UTF_8),
            actual.toByteArray(Charsets.UTF_8)
        )
    }

    private fun digest(salt: ByteArray, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(salt + pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun setRule(context: Context, enabled: Boolean, start: String, end: String) {
        require(runCatching { LocalTime.parse(start) }.isSuccess)
        require(runCatching { LocalTime.parse(end) }.isSuccess)
        prefs(context).edit()
            .putBoolean(ENABLED, enabled)
            .putString(START, start)
            .putString(END, end)
            .apply()
        log(context, "时间规则更新：${if (enabled) "$start 至 $end" else "已停用"}")
    }

    fun isRuleEnabled(context: Context) = prefs(context).getBoolean(ENABLED, false)
    fun start(context: Context) = prefs(context).getString(START, "22:00") ?: "22:00"
    fun end(context: Context) = prefs(context).getString(END, "07:00") ?: "07:00"

    fun isRestrictedNow(context: Context): Boolean {
        if (!isRuleEnabled(context)) return false
        val now = LocalTime.now()
        val start = LocalTime.parse(start(context))
        val end = LocalTime.parse(end(context))
        return if (start == end) {
            true // 相同起止时间表示全天限制
        } else if (start.isBefore(end)) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            !now.isBefore(start) || now.isBefore(end) // 跨午夜
        }
    }

    fun log(context: Context, message: String) {
        val p = prefs(context)
        val old = p.getString(LOG, "") ?: ""
        val line = "${java.time.LocalDateTime.now()}  $message"
        val entries = (listOf(line) + old.lines().filter { it.isNotBlank() }).take(80)
        p.edit().putString(LOG, entries.joinToString("\n")).apply()
    }

    fun logs(context: Context) = prefs(context).getString(LOG, "暂无记录") ?: "暂无记录"
}
