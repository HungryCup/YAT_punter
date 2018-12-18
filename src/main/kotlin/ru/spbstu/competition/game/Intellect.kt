
package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.River
import java.util.*

class Intellect(val graph: Graph, val protocol: Protocol) {

    var gameStage = 0
    var currentSetOfMines = -1
    var nextSetOfMines = -1
    val random = Random()

    fun init() {
        if (graph.getAllMines().size < 2) {//особый режим игры?
            return
        }
    }

    private fun getNextRiver(firstSet: Int, secondSet: Int): River {
        if (!graph.getAllSetsOfMines().contains(firstSet) || !graph.getAllSetsOfMines().contains(secondSet))
            throw IllegalArgumentException("set(s) doesn't exist")
        if (firstSet == secondSet) throw IllegalArgumentException("it is one set")
        val queue = mutableListOf<Int>()
        queue.addAll(graph.getSitesBySetId(firstSet))
        val visited = mutableSetOf<Int>()
        visited.addAll(graph.getSitesBySetId(firstSet))
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in graph.getNeighbors(current)) {
                if (siteState != -1 && siteState != graph.myId || neighbor in visited) continue
                if (graph.getSitesBySetId(secondSet).contains(neighbor)) return River(current, neighbor)
                val partOfGraph = graph.getPartOfGraph(neighbor)
                queue.addAll(partOfGraph)
                visited.addAll(partOfGraph)
            }
        }
        throw IllegalArgumentException("impossible to connect")
    }

    //Улучшить выбор множеств
    private fun updateCurrentAndNextSetsOfMines() {
        val compatibleSetsOfMines = mutableSetOf<Pair<Int, Int>>()
        for (i in 0..graph.getAllSetsOfMines().size - 2) {
            for (j in i + 1 until graph.getAllSetsOfMines().size) {
                if (!graph.getIncompatibleSetsBySetId(graph.getAllSetsOfMines().elementAt(i))
                        .contains(graph.getAllSetsOfMines().elementAt(j))) {
                    //currentSetOfMines = graph.getAllSetsOfMines().elementAt(i)
                    //nextSetOfMines = graph.getAllSetsOfMines().elementAt(j)
                    compatibleSetsOfMines.add(Pair(graph.getAllSetsOfMines().elementAt(i), graph.getAllSetsOfMines().elementAt(j)))
                    //return
                }
            }
        }
        if (compatibleSetsOfMines.isEmpty()) throw IllegalArgumentException()
        val id = random.nextInt(compatibleSetsOfMines.size)
        currentSetOfMines = compatibleSetsOfMines.elementAt(id).first
        nextSetOfMines = compatibleSetsOfMines.elementAt(id).second
    }

    //Улучшить выбор mine и конкретной реки (в зависимости от количества игроков)
    //Быть может не следует занимать все реки mines
    private fun try0(): River {
        var min = Int.MAX_VALUE
        var source = -1
        for (mineId in graph.getAllMines()) {
            val neutral = graph.getNeighbors(mineId).keys.count { key -> graph.getNeighbors(mineId)[key] == -1 }
            val our = graph.getNeighbors(mineId).keys.count { key -> graph.getNeighbors(mineId)[key] == graph.myId }
            //val enemy = graph.getNeighbors(mineId).keys.count { key -> graph.getNeighbors(mineId)[key] == SiteState.Enemy }
            if (neutral == 0) continue
            if (neutral == 1 && our == 0) {
                source = mineId
                break
            }
            val sum = neutral + 2 * our
            if (sum < min) {
                source = mineId
                min = sum
            }
        }
        if (source == -1) {//захват рек у mines закончен
            updateCurrentAndNextSetsOfMines()
            throw IllegalArgumentException()
        }
        var target = -1
        for ((neighbor, siteState) in graph.getNeighbors(source)) {
            if (siteState == -1) {
                target = neighbor
                if (graph.ourSites.contains(neighbor)) break
            }
        }
        return River (source, target)
    }

    //try 0.25 блочить лёгие mine противника
    //try 0.5: отойти от mines на более безопасное расстояние
    //try 0.75: занять мосты
    //try1: занимать реки наперёд, в тех местах, где меньше разветвлений
    //На протяжении всего try1, если нас пытаются заблочить, поддерживать свою свободу

    //Если одно из множеств соединилась с сторонним, то происходит перевыбор множеств
    private fun try1(): River {
        if (graph.getAllSetsOfMines().size < 2) {//or (... == 1)
            graph.setAreaWeights()
            throw IllegalArgumentException()
        }
        while (true) {
            try {
                val result = getNextRiver(currentSetOfMines, nextSetOfMines)
                val temp = currentSetOfMines
                currentSetOfMines = nextSetOfMines
                nextSetOfMines = temp
                return result
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                if (graph.getAllSetsOfMines().contains(currentSetOfMines) && graph.getAllSetsOfMines().contains(nextSetOfMines))
                    graph.setIncompatibleSetsBySetId(currentSetOfMines, nextSetOfMines)
                try {
                    updateCurrentAndNextSetsOfMines()
                } catch (e: IllegalArgumentException) {
                    graph.setAreaWeights()
                    throw e
                }
            }
        }
    }

    //try2 пускать щупальца в стороны (к site с большим весом)
    //Закрывать доступ к site с большим весом?
    private fun try2(): River {
        val setOfAllNeighbors = mutableSetOf<Int>()
        graph.getAllSetsOfMines().forEach { setOfMines -> setOfAllNeighbors.addAll(graph.getNeighborsBySetId(setOfMines)) }
        if (setOfAllNeighbors.isEmpty()) throw IllegalArgumentException()
        var max = -1
        var target = -1
        for (neighbor in setOfAllNeighbors) {
            if (graph.getWeight(neighbor) > max) {
                max = graph.getWeight(neighbor)
                target = neighbor
            }
        }
        //if (target == -1) throw Exception()
        println("Plus $max points")
        val source = graph.getNeighbors(target).keys
                .find { key -> graph.ourSites.contains(key) && graph.getNeighbors(target)[key] == -1 }!!
        return River(source, target)
    }

    //Закрывать доступ к site с большим весом?
    private fun try3(): River {
        for (site in graph.getAllSites()) {
            val neighbor = graph.getNeighbors(site).keys.find { key -> graph.getNeighbors(site)[key] == -1 }
            if (neighbor != null) {
                return River(site, neighbor)
            }
        }
        throw IllegalArgumentException()
    }

    fun makeMove() {
        try {
            val result = when (gameStage) {
                0 -> try0()
                1 -> try1()
                2 -> try2()
                3 -> try3()
                else -> return protocol.passMove()
            }
            println("Game stage: $gameStage")
            protocol.claimMove(result.source, result.target)
        } catch (e: IllegalArgumentException) {
            gameStage++
            makeMove()
        }
    }
}