package ru.spbstu.competition.protocol

import ru.spbstu.competition.protocol.data.*

class Protocol(val connection: ServerConnection) {
    constructor(url: String, port: Int) : this(ServerConnection(url, port))

    // После прописывания всех наших типов данных в Messages.kt
    // и грамотной реализации операций в ServerConnection
    // сами команды становятся очень простой задачей

    // Храним id своей команды прямо тут, чтобы не таскать его туда-сюда
    var myId: Int = -1

    // Собственно, сами команды работы с сервером
    fun handShake(teamName: String) {
        connection.sendJson(HandshakeRequest(teamName))

        val reply: HandshakeResponse = connection.receiveJson()

        assert(reply.you == teamName)
    }

    fun setup(): Setup {
        val result: Setup = connection.receiveJson()
        myId = result.punter
        return result
    }

    fun serverMessage(): ServerMessage = connection.receiveJson()
    fun ready() = connection.sendJson(Ready(myId))
    fun passMove() = connection.sendJson(PassMove(Pass(myId)))
    fun claimMove(from: Int, to: Int) = connection.sendJson(ClaimMove(Claim(myId, from, to)))
}