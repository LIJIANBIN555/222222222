package com.example.guardiantime

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.time.LocalTime

class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private val pad = 20

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RuleScheduler.scheduleNextCheck(this)
        if (!RuleStore.hasPin(this)) {
            showPinSetup()
        } else {
            showGate()
        }
    }

    private fun base(title: String): LinearLayout {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
            gravity = Gravity.TOP
        }
        scroll.addView(root)
        setContentView(scroll)
        addText(title, 24f, true)
        return root
    }

    private fun addText(text: String, size: Float = 16f, bold: Boolean = false) {
        root.addView(TextView(this).apply {
            this.text = text
            textSize = size
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 10, 0, 10)
        })
    }

    private fun addButton(label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 6, 0, 6) })
    }

    private fun edit(hint: String, value: String = "", numeric: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint
            setText(value)
            if (numeric) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            else inputType = InputType.TYPE_CLASS_TEXT
        }

    private fun showPinSetup() {
        base("守护时段 · 首次设置")
        addText("请设置 6–12 位数字家长 PIN。请妥善保管；当前原型不提供云端找回。")
        val pin = edit("新 PIN（6–12 位）", numeric = true)
        val confirm = edit("再次输入 PIN", numeric = true)
        root.addView(pin)
        root.addView(confirm)
        addButton("保存 PIN") {
            val a = pin.text.toString()
            val b = confirm.text.toString()
            if (a.length !in 6..12 || !a.all(Char::isDigit)) {
                message("PIN 必须是 6–12 位数字")
            } else if (a != b) {
                message("两次输入不一致")
            } else {
                RuleStore.setPin(this, a)
                showGate()
            }
        }
    }

    private fun showGate() {
        base("守护时段")
        addText("家长管理入口", 18f, true)
        addText("输入家长 PIN 进入设置。备用恢复入口也需要验证 PIN，避免孩子直接解除限制。")
        val pin = edit("家长 PIN", numeric = true)
        root.addView(pin)
        addButton("验证并进入管理") {
            if (RuleStore.verifyPin(this, pin.text.toString())) {
                RuleStore.log(this, "家长验证成功")
                showDashboard()
            } else {
                RuleStore.log(this, "家长 PIN 验证失败")
                message("PIN 错误")
            }
        }
        addButton("备用恢复入口") {
            if (RuleStore.verifyPin(this, pin.text.toString())) {
                AlertDialog.Builder(this)
                    .setTitle("备用恢复")
                    .setMessage("验证成功。你可以停用时间规则并返回管理界面。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("停用规则") { _, _ ->
                        RuleStore.setRule(this, false, RuleStore.start(this), RuleStore.end(this))
                        RuleStore.log(this, "家长通过备用入口停用规则")
                        showDashboard()
                    }.show()
            } else message("请先输入正确的家长 PIN")
        }
        addText("当前原型仅限制本 App 内的页面，不会锁定整个手机。", 13f)
    }

    private fun showDashboard() {
        base("家长控制台")
        val restricted = RuleStore.isRestrictedNow(this)
        addText("当前状态：${if (restricted) "受限时段（应用内状态）" else "允许使用"}", 18f, true)
        addText("已配置规则：${if (RuleStore.isRuleEnabled(this)) "启用" else "停用"}")
        addText("限制开始：${RuleStore.start(this)}")
        addText("限制结束：${RuleStore.end(this)}")
        addButton("设置时间规则") { showRuleEditor() }
        addButton("立即重新检查状态") {
            RuleStore.log(this, "家长手动检查状态")
            showDashboard()
        }
        addButton("修改家长 PIN") { changePin() }
        addButton("查看操作日志") { showLogs() }
        addButton("返回 PIN 入口") { showGate() }
        addText("注意：此页面不是系统级锁屏；孩子可能通过系统设置停止或卸载原型。", 13f)
    }

    private fun showRuleEditor() {
        base("时间规则")
        addText("输入 24 小时制时间，例如 22:00 和 07:00。支持跨午夜。起止时间相同表示全天限制。")
        val start = edit("开始时间 HH:mm", RuleStore.start(this))
        val end = edit("结束时间 HH:mm", RuleStore.end(this))
        root.addView(start)
        root.addView(end)
        addButton(if (RuleStore.isRuleEnabled(this)) "保存并启用规则" else "保存并启用规则") {
            val s = start.text.toString().trim()
            val e = end.text.toString().trim()
            val valid = runCatching { LocalTime.parse(s); LocalTime.parse(e) }.isSuccess
            if (!valid) {
                message("时间格式错误，请使用 HH:mm，例如 22:00")
            } else {
                RuleStore.setRule(this, true, s, e)
                RuleScheduler.scheduleNextCheck(this)
                message("规则已保存。此原型不会强制锁定整台手机。")
                showDashboard()
            }
        }
        addButton("停用规则") {
            RuleStore.setRule(this, false, RuleStore.start(this), RuleStore.end(this))
            showDashboard()
        }
        addButton("返回") { showDashboard() }
    }

    private fun changePin() {
        val oldPin = edit("当前 PIN", numeric = true)
        val newPin = edit("新 PIN（6–12 位）", numeric = true)
        val confirm = edit("再次输入新 PIN", numeric = true)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
            addView(oldPin); addView(newPin); addView(confirm)
        }
        AlertDialog.Builder(this)
            .setTitle("修改家长 PIN")
            .setView(box)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val n = newPin.text.toString()
                if (!RuleStore.verifyPin(this, oldPin.text.toString())) {
                    message("当前 PIN 错误")
                } else if (n.length !in 6..12 || !n.all(Char::isDigit) || n != confirm.text.toString()) {
                    message("新 PIN 无效或两次输入不一致")
                } else {
                    RuleStore.setPin(this, n)
                    message("PIN 已修改")
                    showDashboard()
                }
            }.show()
    }

    private fun showLogs() {
        base("操作日志")
        addText(RuleStore.logs(this), 13f)
        addButton("返回") { showDashboard() }
    }

    private fun message(text: String) {
        AlertDialog.Builder(this).setMessage(text).setPositiveButton("确定", null).show()
    }
}
