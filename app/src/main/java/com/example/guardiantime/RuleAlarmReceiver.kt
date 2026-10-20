package com.example.guardiantime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RuleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val restricted = RuleStore.isRestrictedNow(context)
        RuleStore.log(context, "定时检查：${if (restricted) "当前处于受限时段" else "当前允许使用"}")
        // MVP 只记录规则状态，不会锁定整个手机或切断系统网络。
        RuleScheduler.scheduleNextCheck(context)
    }
}
