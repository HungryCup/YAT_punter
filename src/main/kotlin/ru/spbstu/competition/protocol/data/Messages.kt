package ru.spbstu.competition.protocol.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import ru.spbstu.competition.protocol.objectMapper

// Все эти классы взяты напрямую из описания протокола
// Если правильно описать все поля, то магия библиотеки Jackson
// позволит очень легко реализовать весь протокол!
data class HandshakeRequest(val me: String)
data class HandshakeResponse(val you: String)

data class Site(val id: Int, val x: Double?, val y: Double?)
data class River(val source: Int, val target: Int) {
    // Злобные враги специально будут делать ходы, у которых концы реки перевёрнуты местами
    // Необходимо обеспечить, чтобы две такие реки считались одинаковыми
    override fun equals(other: Any?) =
            other is River &&
                    (source == other.source && target == other.target ||
                            source == other.target && target == other.source)

    // Xor это самый простой способ сделать зеркальный hash
    override fun hashCode() = source.hashCode() xor target.hashCode()
}

data class Map(val sites: List<Site>, val rivers: List<River>, val mines: List<Int>)
data class Setup(val punter: Int, val punters: Int, val map: Map, val settings: JsonNode?)

data class Ready(val ready: Int)

data class Pass(val punter: Int)
data class Claim(val punter: Int, val source: Int, val target: Int)

data class PassMove(val pass: Pass): Move()
data class ClaimMove(val claim: Claim): Move()

sealed class Move {

    // Библиотека Jackson написана на Java, поэтому требует, чтобы функции нестандартной конверсии из JSON
    // были статическими, в Kotlin этого можно добиться с помощью аннотации @JvmStatic внутри companion object
    companion object {
        @JvmStatic
        @JsonCreator
        fun factory(map: kotlin.collections.Map<String, Any>): Move {
            return when {
                "pass" in map -> objectMapper.convertValue(map, PassMove::class.java)
                "claim" in map -> objectMapper.convertValue(map, ClaimMove::class.java)
                else -> throw IllegalArgumentException()
            }
        }
    }
}

data class GameTurn(val moves: List<Move>)
data class GameTurnMessage(val move: GameTurn): ServerMessage()

data class Score(val punter: Int, val score: Int)
data class GameStop(val moves: List<Move>, val scores: List<Score>)

data class GameResult(val stop: GameStop): ServerMessage()
data class Timeout(val timeout: Double) : ServerMessage()

sealed class ServerMessage {

    // Библиотека Jackson написана на Java, поэтому требует, чтобы функции нестандартной конверсии из JSON
    // были статическими, в Kotlin этого можно добиться с помощью аннотации @JvmStatic внутри companion object
    companion object {
        @JvmStatic
        @JsonCreator
        fun factory(map: kotlin.collections.Map<String, Any>): ServerMessage {
            return when {
                "move" in map -> objectMapper.convertValue(map, GameTurnMessage::class.java)
                "stop" in map -> objectMapper.convertValue(map, GameResult::class.java)
                "timeout" in map -> objectMapper.convertValue(map, Timeout::class.java)
                else -> throw IllegalArgumentException()
            }
        }
    }
}
