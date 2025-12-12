@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package uz.alien

import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import kotlinx.cinterop.convert
import platform.posix.getenv
import platform.posix.pclose
import platform.posix.popen
import platform.posix.fgets
import platform.posix.usleep
import kotlinx.cinterop.ByteVar

private fun runCommand(cmd: String): String {
    val fp = popen(cmd, "r") ?: return ""
    val out = StringBuilder()
    memScoped {
        val bufSize = 4096
        val buf = allocArray<ByteVar>(bufSize)
        while (fgets(buf, bufSize.convert(), fp) != null) {
            out.append(buf.toKString())
        }
    }
    pclose(fp)
    return out.toString()
}

private fun escapeForDoubleQuotes(s: String): String {
    // Escape double quotes and backslashes for inclusion inside double-quoted shell arg
    return s.replace("\\", "\\\\").replace("\"", "\\\"")
}

fun main() {
    val token = getenv("TELEGRAM_BOT_TOKEN")?.toKString()
    if (token.isNullOrBlank()) {
        println("TELEGRAM_BOT_TOKEN not set")
        return
    }

    var offset = 0L
    val getUpdatesBase = "https://api.telegram.org/bot$token/getUpdates"
    val sendMessageBase = "https://api.telegram.org/bot$token/sendMessage"

    val updateIdRegex = Regex("""\"update_id\"\s*:\s*(\d+)""")
    val chatIdRegex = Regex("""\"chat\"\s*:\s*\{[^}]*\"id\"\s*:\s*(\d+)""")
    val firstNameRegex = Regex("""\"first_name\"\s*:\s*\"([^\"]+)\"""")
    val textRegex = Regex("""\"text\"\s*:\s*\"([^\"]*)\"""")

    while (true) {
        val url = "$getUpdatesBase?timeout=10&offset=${'$'}{offset + 1}"
        val resp = runCommand("curl -s \"$url\"")
        if (resp.isNotBlank()) {
            // find all update ids
            val ids = updateIdRegex.findAll(resp).mapNotNull { it.groupValues.getOrNull(1)?.toLongOrNull() }.toList()
            var maxId = offset
            if (ids.isNotEmpty()) maxId = ids.maxOrNull() ?: offset

            // find chat blocks by locating "chat" objects and nearby fields
            val chatMatches = Regex("""\{[^}]*\"chat\"[^}]*\}""").findAll(resp)
            for (cm in chatMatches) {
                val chatBlock = cm.value
                val chatId = chatIdRegex.find(chatBlock)?.groupValues?.get(1) ?: continue
                val name = firstNameRegex.find(chatBlock)?.groupValues?.get(1) ?: "do'st"

                // For simplicity, attempt to find the text near this chat block
                val after = resp.substring(cm.range.last + 1).take(300)
                val text = textRegex.find(after)?.groupValues?.get(1) ?: ""

                if (text.trim() == "/start") {
                    val message = "Assalomu aleykum, $name!"
                    val esc = escapeForDoubleQuotes(message)
                    val sendCmd = "curl -s -X POST \"$sendMessageBase\" -d chat_id=$chatId --data-urlencode \"text=$esc\""
                    runCommand(sendCmd)
                }
            }

            if (maxId > offset) offset = maxId
        }

        // Sleep a bit to avoid tight loop
        usleep(500_000u)
    }
}