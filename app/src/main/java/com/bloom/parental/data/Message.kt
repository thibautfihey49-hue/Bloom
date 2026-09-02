package com.bloom.parental.data
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
data class Message(
    val id: String = System.currentTimeMillis().toString(),
    val from: String, val to: String, val content: String,
    val ts: Long = System.currentTimeMillis()
) {
    fun time(): String = SimpleDateFormat("HH:mm", Locale.FRANCE).format(Date(ts))
}
object MsgManager {
    private val messagesList = mutableListOf<Message>()
    fun sendMessage(from: String, to: String, content: String) {
        messagesList.add(0, Message(from = from, to = to, content = content))
    }
    fun all(): List<Message> = messagesList.toList()
}
