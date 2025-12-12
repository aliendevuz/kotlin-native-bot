package uz.alien

fun main() {
    val input = generateSequence { readLine() }?.joinToString("\n") ?: ""
    val bodyEscaped = input.replace("\"", "\\\"").replace("\n", "\\n")
    val response = "{\"statusCode\":200,\"body\":\"Received: $bodyEscaped\"}"
    println(response)
}