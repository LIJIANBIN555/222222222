package com.example.guardiantime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        RuleStore.log(context, "系统启动或应用更新后重新检查规则")
        // 这里不尝试从后台强行打开界面；现代 Android 会限制后台启动 Activity。
        RuleScheduler.scheduleNextCheck(context)
    }
}
