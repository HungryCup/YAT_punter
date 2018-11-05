package ru.spbstu.competition

import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import ru.spbstu.competition.game.Graph
import ru.spbstu.competition.game.Intellect
import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.*

object Arguments {
    @Option(name = "-u", usage = "Specify server url")
    var url: String = ""

    @Option(name = "-p", usage = "Specify server port")
    var port: Int = -1

    fun use(args: Array<String>): Arguments =
            CmdLineParser(this).parseArgument(*args).let{ this }
}

fun main(args: Array<String>) {
    val args1: Array<String> = arrayOf("-u", "kotoed.icc.spbstu.ru", "-p", "50004")
    Arguments.use(args1)
    //Arguments.use(args)

    println("Hi, I am Joe")

    // Протокол обмена с сервером
    val protocol = Protocol(Arguments.url, Arguments.port)
    // Состояние игрового поля
    val graph = Graph()
    // Джо очень умный чувак, вот его ум
    val intellect = Intellect(graph, protocol)

    protocol.handShake("Joe")



    val setupData = protocol.setup()
    graph.init(setupData)
    intellect.init()


    println("Received id = ${setupData.punter}")

    protocol.ready()

    gameloop@ while(true) {
        val message = protocol.serverMessage()
        when(message) {
            is GameResult -> {
                println("The game is over!")
                val myScore = message.stop.scores[protocol.myId]
                println("Joe scored ${myScore.score} points!")
                break@gameloop
            }
            is Timeout -> {
                println("Joe too slow =(")
            }
            is GameTurnMessage -> {
                for(move in message.move.moves) {
                    when(move) {
                        is PassMove -> {}
                        is ClaimMove -> {
                            graph.update(move.claim)
                        }
                    }
                }
            }
        }

        println("Joe thin'")
        intellect.makeMove()
        println("Joe genius!")
    }
}
