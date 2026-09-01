package com.mumu.abifix

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

/**
 * MuMu 公主连结 ABI 定点修复（不改整文件，只改公主连结条目）。
 * 以 root 读取 /data/system/etc/mumu-configs 下的两个配置文件，
 * 仅把 com.bilibili.priconne（含 .yofun.mumu 渠道版）这一条目的 ABI 改为 x86_64，
 * 其余所有行（含台服 tw.sonet.princessconnect、其它游戏）原样保留。
 * 修改前把原文件备份为 .bak；可一键还原。不主动申请 root。
 */
class MainActivity : AppCompatActivity() {

    private val targetDir = "/data/system/etc/mumu-configs"
    // 设备上已存在的同名配置文件
    private val targetFiles = arrayOf("abi-select-android12.config", "abi-select-v2.config")
    // 只匹配公主连结（国服/官服，含 .yofun.mumu 等渠道版）；不匹配台服 tw.sonet.princessconnect
    private val pkgPrefix = "com\\.bilibili\\.priconne"
    private val wantAbi = "x86_64"

    private lateinit var statusTv: TextView
    private lateinit var scroll: android.widget.ScrollView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusTv = findViewById(R.id.status)
        scroll = findViewById(R.id.scroll)

        findViewById<Button>(R.id.btn_apply).setOnClickListener { applyFix() }
        findViewById<Button>(R.id.btn_restore).setOnClickListener { restoreBackup() }
        findViewById<Button>(R.id.btn_reapply).setOnClickListener { applyFix() }
        findViewById<Button>(R.id.btn_clear).setOnClickListener { statusTv.text = "" }

        log("目标目录: " + targetDir)
        log("只修改公主连结(com.bilibili.priconne 及其 .yofun.mumu 版) -> " + wantAbi)
        log("台服 tw.sonet.princessconnect 及其它条目不受影响。")
        log("点「定点修复」执行；点「还原备份」恢复修改前文件。")
    }

    private fun q(s: String): String = "\"" + s + "\""

    private fun applyFix() {
        Thread {
            log("=== 开始定点修复（" + targetFiles.size + " 个文件）===")
            for (name in targetFiles) { modifyOne(name) }
            log("=== 全部完成，建议重启 MuMu 模拟器使配置生效 ===")
        }.start()
    }

    private fun modifyOne(name: String) {
        try {
            val target = targetDir + "/" + name
            val bak = target + ".bak"

            // 备份原件（仅当 .bak 还不存在时）
            RootShell.run("if [ ! -f " + q(bak) + " ]; then cp " + q(target) + " " + q(bak) + "; fi")

            // 读取当前内容
            val (content, _) = RootShell.run("cat " + q(target))
            if (content.isBlank() || content.startsWith("io-error")) {
                log("● 未能读取 " + name + "（目标文件可能不存在）：" + content.take(120))
                return
            }

            // 内存里定点改写：只改 com.bilibili.priconne 前缀行的 ABI
            val lines = content.split("\n").toMutableList()
            var changed = 0
            val re = Regex("^\\s*(\\S+)\\s+(\\S+)")
            for (i in lines.indices) {
                val m = re.find(lines[i])
                if (m != null) {
                    val pkg = m.groupValues[1]
                    if (pkg.startsWith(pkgPrefix)) {
                        val tail = lines[i].substring(m.range.last + 1)
                        lines[i] = pkg + " " + wantAbi + tail
                        changed++
                    }
                }
            }

            if (changed == 0) {
                log("○ " + name + "：未找到公主连结条目，未做改动（格式可能不同）。")
                return
            }

            // 写回：先把新内容写到缓存，再以 root 覆盖目标
            val cacheFile = File(cacheDir, "edit-" + name)
            cacheFile.writeText(lines.joinToString("\n"))
            val wres = RootShell.run("cp -f " + q(cacheFile.absolutePath) + " " + q(target) + " && chmod 644 " + q(target) + " && echo WRITE_OK")
            val ok = wres.first.contains("WRITE_OK")
            val st = if (ok) "  修改成功" else "  写回失败 " + wres.first.take(120)
            log("● " + name + "：已修改 " + changed + " 处公主连结条目 -> " + wantAbi + st)
        } catch (e: Exception) {
            log("修改 " + name + " 异常: " + e.message)
        }
    }

    private fun restoreBackup() {
        Thread {
            log("=== 开始还原备份 ===")
            for (name in targetFiles) {
                val target = targetDir + "/" + name
                val bak = target + ".bak"
                val res = RootShell.run("if [ -f " + q(bak) + " ]; then cp -f " + q(bak) + " " + q(target) + " && chmod 644 " + q(target) + " && echo RESTORE_OK; else echo NO_BAK; fi")
                val out = res.first
                when {
                    out.contains("RESTORE_OK") -> log("● 还原成功  " + name)
                    out.contains("NO_BAK") -> log("○ 未找到备份，跳过  " + name)
                    else -> log("● 还原失败  " + name + " : " + out.take(200))
                }
            }
            log("=== 还原完成 ===")
        }.start()
    }

    private fun log(msg: String) {
        handler.post {
            statusTv.append("\n" + msg)
            scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
        }
    }
}
