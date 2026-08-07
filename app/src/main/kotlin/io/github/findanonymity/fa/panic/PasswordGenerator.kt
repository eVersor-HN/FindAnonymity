package io.github.findanonymity.fa.panic

import java.security.SecureRandom

object PasswordGenerator {
    private const val CHARSET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
            "!@#\$%^&*()-_=+[]{};:,.<>/?~`'\"\\|"
    private val secureRandom = SecureRandom()

    fun generate(length: Int): String {
        val sb = StringBuilder(length)
        repeat(length) { sb.append(CHARSET[secureRandom.nextInt(CHARSET.length)]) }
        return sb.toString()
    }
}
