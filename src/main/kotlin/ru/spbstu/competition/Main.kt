package ru.spbstu.competition

import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import ru.spbstu.competition.game.Graph
import ru.spbstu.competition.game.Intellect
import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.*

private var moveNumber = 1

object Arguments {
    @Option(name = "-u", usage = "Specify server url")
    var url: String = ""

    @Option(name = "-p", usage = "Specify server port")
    var port: Int = -1

    fun use(args: Array<String>): Arguments =
            CmdLineParser(this).parseArgument(*args).let{ this }
}

fun main(args: Array<String>) {
    Arguments.use(args)
    println("Hi, I am YAT")
    val protocol = Protocol(Arguments.url, Arguments.port)
    val graph = Graph()
    val intellect = Intellect(graph, protocol)
    protocol.handShake("YAT")
    val setupData = protocol.setup()
    graph.init(setupData)
    println("Sites: ${graph.getAllSites().size}")
    println("Rivers: ${graph.getAllRivers().size}")
    intellect.init()
    println("Received id = ${setupData.punter}")
    protocol.ready()
    gameloop@ while(true) {
        val message = protocol.serverMessage()
        when(message) {
            is GameResult -> {
                println("The game is over!")
                val myScore = message.stop.scores[protocol.myId]
                println("YAT scored ${myScore.score} points!")
                break@gameloop
            }
            is Timeout -> {
                println("YAT too slow =(")
            }
            is GameTurnMessage -> {
                for(move in message.move.moves) {
                    when(move) {
                        is PassMove -> {}
                        is ClaimMove -> {
                            graph.update(move.claim)
                        }
                    }
                    moveNumber++
                }
            }
        }
        println("Moves left: ${graph.getAllRivers().size - moveNumber}")
        println("YAT thin'")
        intellect.makeMove()
        println("YAT genius!")
    }
}