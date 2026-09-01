package com.mumu.abifix

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.lang.Exception

/**
 * 通过 su 实现 root 的薄封装。不依赖第三方库（无需 libsu/JitPack），
 * 在已 root 的设备上工作（Magisk / SuperSU / MuMu 内置 root 均支持 su -c）。
 */
object RootShell {

    /**
     * 探测 root 是否可用（会触发 root 管理器授权弹窗）。
     * @return true 表示拿到了 uid=0。
     */
    fun requestRoot(): Boolean {
        val (out, _) = exec(arrayOf("su", "-c", "id"))
        return out.contains("uid=0")
    }

    /**
     * 以 root 执行 shell 命令字符串，返回（结果文本, 退出码）。
     * 用 su -c 传入，多个命令可用 ; 或 && 拼接。
     */
    fun run(command: String): Pair<String, Int> {
        return exec(arrayOf("su", "-c", command))
    }

    private fun exec(cmd: Array<String>): Pair<String, Int> {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(cmd)
            val out = read(process.inputStream)
            val err = read(process.errorStream)
            val code = process.waitFor()
            ("$out\n$err").trim() to code
        } catch (e: Exception) {
            ("io-error: " + (e.message ?: e.toString())) to -1
        } finally {
            process?.destroy()
        }
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
