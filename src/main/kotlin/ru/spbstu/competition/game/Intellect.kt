
package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.River

class Intellect(val graph: Graph, val protocol: Protocol) {

    var gameStage = 0
    var currentMineId = -1
    var nextMineId = -1

    fun init() {
        if (graph.getAllMines().size < 2) {//особый режим игры???
            return
        }
    }

    private fun getNextRiver(first: Int, second: Int): River {//second is always "mine"
        if (!graph.getAllSites().contains(first) || !graph.getAllSites().contains(second)) throw IllegalArgumentException("site(s) doesn't exist")
        if (graph.getSites(second)!!.contains(first)) throw IllegalArgumentException("already connected")
        val queue = mutableListOf<Int>()
        queue.add(first)
        val visited = mutableSetOf(first)
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((id, vertexState) in graph.getNeighbors(current)) {
                if (vertexState == VertexState.Enemy || id in visited) continue
                if (graph.getSites(second).contains(id)) {
                    return River(current, id)
                }
                queue.add(id)
                visited.add(id)
            }
        }
        throw IllegalArgumentException("impossible to connect")
    }

    private fun updateCurrentAndNextMines() {
        findMines@for (i in 0..graph.getAllMines().size - 2) {
            for (j in i + 1 until graph.getAllMines().size) {
                if (graph.getSites(graph.getAllMines().elementAt(i))
                        !== graph.getSites(graph.getAllMines().elementAt(j))
                        && !graph.getIncompatibleSets(graph.getAllMines().elementAt(i))
                        .contains(graph.getAllMines().elementAt(j))) {
                    currentMineId = graph.getAllMines().elementAt(i)
                    nextMineId = graph.getAllMines().elementAt(j)
                    return
                }
            }
        }
        throw IllegalArgumentException()
    }

    private fun try0(): River {
        var min = Int.MAX_VALUE
        var source = -1
        for (mineId in graph.getAllMines()) {
            val neutral = graph.getNeighbors(mineId).keys.count { key -> graph.getNeighbors(mineId)[key] == VertexState.Neutral }
            if (neutral == 0) continue
            val our = graph.getNeighbors(mineId).keys.count { key -> graph.getNeighbors(mineId)[key] == VertexState.Our }
            val enemy = graph.getNeighbors(mineId).keys.count { key -> graph.getNeighbors(mineId)[key] == VertexState.Enemy }
            val sum = neutral + enemy + 2 * our
            if (sum < min) {
                source = mineId
                min = sum
            }
        }
        if (source == -1) {//нетральных соседей нет ни у одной mine
            if (graph.getAllMines().size > 1) {
                updateCurrentAndNextMines()
            } else {
                //
            }
            throw IllegalArgumentException()
        }
        //val target = graph.getNeighbors(source).keys.find { key -> graph.getNeighbors(source)[key] == VertexState.Neutral }!!
        var target = -1
        for ((key, _) in graph.getNeighbors(source)) {
            if (graph.getNeighbors(source)[key] == VertexState.Neutral) {
                target = key
                if (graph.getAllMines().contains(key)) break
            }
        }
        return River (source, target)
    }

    private fun try1(): River {
        while (true) {
            try {
                val result = getNextRiver(currentMineId, nextMineId)
                val temp = currentMineId
                currentMineId = nextMineId
                nextMineId = temp
                return result
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                graph.getAllMines()
                        .filter { graph.getSites(it) === graph.getSites(currentMineId) }
                        .forEach { mineId ->
                            graph.getAllMines()
                                    .filter { graph.getSites(it) === graph.getSites(nextMineId) }
                                    .forEach {
                                        graph.setIncompatibleSets(mineId, it)
                                        graph.setIncompatibleSets(it, mineId)
                                    }
                        }
                updateCurrentAndNextMines()
            }
        }
    }

    private fun try2(): River {
        val setNeighbors = mutableSetOf<Int>()
        for (ourSite in graph.ourSites) {
            setNeighbors.addAll(graph.getNeighbors(ourSite).keys
                    .filter { key -> graph.getNeighbors(ourSite)[key] == VertexState.Neutral && !graph.ourSites.contains(key)})
        }
        if (setNeighbors.isEmpty()) throw IllegalArgumentException()
        for (mine in graph.getAllMines()) {//не оптимально
            graph.setWeights(mine)
        }
        var max = -1L
        var target = -1
        for (neighbor in setNeighbors) {
            if (graph.getWeight(neighbor) > max) {
                max = graph.getWeight(neighbor)
                target = neighbor
            }
        }
        //if (target == -1) throw Exception()
        val source = graph.getNeighbors(target).keys
                .find { key -> graph.ourSites.contains(key) && graph.getNeighbors(target)[key] == VertexState.Neutral }!!
        return River(source, target)
    }

    private fun try3(): River {
        for (site in graph.getAllSites()) {
            val neighbor = graph.getNeighbors(site).keys.find { key -> graph.getNeighbors(site)[key] == VertexState.Neutral }
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