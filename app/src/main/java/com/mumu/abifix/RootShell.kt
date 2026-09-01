package com.mumu.abifix

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.Exception

/**
 * 通过 su 实现 root 的薄封装。依次尝试常见 su 路径（含 KernelSU / Magisk），
 * 哪个能用就用哪个。不依赖第三方库。
 */
object RootShell {

    // 常见 su 路径，优先用普通 su；找不到再尝试 KernelSU 等
    private val suList = arrayOf(
        "su",
        "/data/adb/ksu/bin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su"
    )

    /** 探测 root 是否可用（会触发 root 管理器授权）。@return true 表示拿到 uid=0。 */
    fun requestRoot(): Boolean {
        val (out, _) = run("id")
        return out.contains("uid=0")
    }

    /** 以 root 执行 shell 命令字符串，返回（结果文本, 退出码）。 */
    fun run(command: String): Pair<String, Int> {
        for (s in suList) {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(arrayOf(s, "-c", command))
                val out = read(process.inputStream)
                val err = read(process.errorStream)
                val code = process.waitFor()
                return ("$out\n$err").trim() to code
            } catch (e: Exception) {
                // 此路径下找不到 su，尝试下一个
                process?.destroy()
            }
        }
        return "no su binary found" to -1
    }

    private fun read(stream: InputStream): String {
        val baos = ByteArrayOutputStream()
        val buf = ByteArray(4096)
        while (true) {
            val n = stream.read(buf)
            if (n == -1) break
            baos.write(buf, 0, n)
        }
        stream.close()
        return baos.toString("UTF-8")
    }
}
