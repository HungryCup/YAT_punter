package ru.spbstu.competition.protocol

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class ServerConnection(val url: String, val port: Int) {

    // Для связи с сервером для начала нужно создать сокет
    private val socket = Socket(url, port.toInt())

    // Потом из сокета можно получить потоки ввода и вывода, и работать с ним, как с файлом
    // В нашей задаче можно сделать BufferedReader/PrintWriter, потому что протокол текстовый
    val sin = BufferedReader(InputStreamReader(socket.getInputStream()))
    val sout = PrintWriter(socket.getOutputStream(), true)

    // Отправка сообщения на сервер
    fun <T> sendJson(json: T) {
        val jsonString = objectMapper.writeValueAsString(json)

        // См. описание протокола
        sout.println("${jsonString.length}:${jsonString}")
        // Буферизованные потоки в Java требуют вызова метода flush(), чтобы
        // записанное точно отправилось на сервер
        sout.flush()
    }

    // Получение сообщения с сервера
    // Здесь используется продвинутая фича Котлина - reified generics
    // Она требует, чтобы функция была inline и позволяет получить для T объект класса
    // (см. последнюю строку функции)
    inline fun <reified T> receiveJson(): T {
        val lengthChars = mutableListOf<Char>()
        var ch = '0'
        while (ch != ':') {
            lengthChars += ch
            ch = sin.read().toChar()
        }

        val length = lengthChars.joinToString("").trim().toInt()

        // Чтение из Reader нужно делать очень аккуратно
        val contentAsArray = CharArray(length)
        var start = 0
        // Операция read не гарантирует нам, что вернулось именно нужное количество символов
        // Поэтому её нужно делать в цикле
        while (start < length) {
            val read = sin.read(contentAsArray, start, length - start)
            start += read
        }

        return objectMapper.readValue(String(contentAsArray), T::class.java)
    }

}
