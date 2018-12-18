
package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.River
import java.util.*

class Intellect(val graph: Graph, val protocol: Protocol) {

    val smallDepth = 5//try 1,2
    val shallowDepth = 17//
    var gameStage = 0
    var currentSetOfMines = -1
    var nextSetOfMines = -1
    var maxSite = -1
    var setContainsMaxSiteInArea = -1
    var currentEnemy = -1

    //что если врагов нет
    fun init() {
        if (graph.getAllMines().size < 2) {//особый режим игры?
            return
        }
        if (graph.punters > 1) currentEnemy = (graph.myId + 1) % graph.punters
    }

    private fun updateCurrentEnemy() {
        currentEnemy = (currentEnemy + 1) % graph.punters
        if (currentEnemy == graph.myId) updateCurrentEnemy()//если врагов нет и я играю один, то вечная рекурсия
    }

    private fun getWayForTry1(firstSet: Int, secondSet: Int): LinkedList<River> {
        if (!graph.getAllSetsOfMines().contains(firstSet) || !graph.getAllSetsOfMines().contains(secondSet))
            throw IllegalArgumentException("set(s) doesn't exist")
        if (firstSet == secondSet) throw IllegalArgumentException("it is one set")
        val way = getWay(graph.myId, graph.getSitesBySetId(firstSet), graph.getSitesBySetId(secondSet))
        if (way.isEmpty()) throw IllegalArgumentException("impossible to connect")
        return way
    }

    private fun getWayForTry2(setOfMines: Int, maxSite: Int): LinkedList<River> {
        if (!graph.getAllSetsOfMines().contains(setOfMines) || !graph.getAllSites().contains(maxSite))
            throw IllegalArgumentException("set or site doesn't exist")
        if (graph.getSitesBySetId(setOfMines).contains(maxSite)) throw IllegalArgumentException("already connected")
        val way = getWay(graph.myId, graph.getSitesBySetId(setOfMines), graph.getPartOfGraph(graph.myId, maxSite))
        if (way.isEmpty()) throw IllegalArgumentException("impossible to connect")
        return way
    }

    private fun updateCurrentAndNextSetsOfMines() {
        val ways = getWays()
        if (ways.isEmpty()) throw IllegalArgumentException()
        var minWay = Int.MAX_VALUE
        for ((pairOfSets, way) in ways) {
            if (way.size < minWay) {
                minWay = way.size
                currentSetOfMines = pairOfSets.first
                nextSetOfMines = pairOfSets.second
            }
        }
    }

    private fun updateMaxSiteAndSetContainsItInArea() {
        val areas = mutableSetOf<Int>()
        graph.getAllSetsOfMines().forEach { setOfMines ->
            graph.setAreaBySetId(setOfMines, graph.findArea(graph.myId, graph.getSitesBySetId(setOfMines)).toMutableSet())
            areas.addAll(graph.getAreaBySetId(setOfMines))
        }
        if (areas.isEmpty()) throw IllegalArgumentException()
        var max = -1
        for (site in areas) {
            if (graph.getWeight(site) > max) {
                max = graph.getWeight(site)
                maxSite = site
            }
        }
        for (setOfMines in  graph.getAllSetsOfMines()) {
            if (graph.getAreaBySetId(setOfMines).contains(maxSite)) {
                setContainsMaxSiteInArea = setOfMines
                break
            }
        }
        //println("$max points has $maxSite")
    }

    private fun getWay(punter: Int, firstSet: Set<Int>, secondSet: Set<Int>): LinkedList<River> {
        val queue = mutableListOf<Int>()
        queue.addAll(firstSet)
        val visited = mutableMapOf<Int, Int>()
        firstSet.forEach { site -> visited[site] = -1 }
        var to = -1
        var from = -1
        queueLoop@while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in graph.getNeighbors(current)) {
                if (siteState != -1 && siteState != punter || neighbor in visited) continue
                if (secondSet.contains(neighbor)) {
                    visited[neighbor] = current
                    to = neighbor
                    from = current
                    break@queueLoop
                }
                val partOfGraph = graph.getPartOfGraph(punter, neighbor)
                queue.addAll(partOfGraph)
                visited[neighbor] = current
                if (partOfGraph.size != 1) {
                    val tempQueue = mutableListOf(neighbor)
                    while (tempQueue.isNotEmpty()) {
                        val tempCurrent = tempQueue[0]
                        tempQueue.removeAt(0)
                        for (tempNeighbor in graph.getNeighbors(tempCurrent).keys) {
                            if (tempNeighbor !in partOfGraph || tempNeighbor in visited) continue
                            tempQueue.add(tempNeighbor)
                            visited[tempNeighbor] = tempCurrent
                        }
                    }
                }
            }
        }
        val way = LinkedList<River>()
        while (from != -1) {
            if (graph.getNeighbors(from)[to] == -1) way.addFirst(River(from, to))
            to = from
            from = visited[to]!!
        }
        return way
    }

    private fun getWays(): MutableMap<Pair<Int, Int>, LinkedList<River>> {
        val ways = mutableMapOf<Pair<Int, Int>, LinkedList<River>>()
        for (firstSetOfMines in graph.getAllSetsOfMines()) {
            val directions = findDirections(graph.myId, graph.getSitesBySetId(firstSetOfMines))
            for (secondSetOfMines in graph.getAllSetsOfMines()) {
                val way = LinkedList<River>()
                var to = graph.getMinesBySetId(secondSetOfMines).elementAt(0)
                var from = directions[to] ?: continue
                while (from != -1) {
                    if (graph.getNeighbors(from)[to] == -1) way.addFirst(River(from, to))
                    to = from
                    from = directions[to]!!
                }
                if (way.isNotEmpty()) ways[Pair(firstSetOfMines, secondSetOfMines)] = way
            }
        }
        return ways
    }

    private fun getEnemySets(punter: Int): Map<Int, Set<Int>> {
        val sets = mutableMapOf<Int, Set<Int>>()
        graph.getAllMines().forEach { mine ->
            if (sets.values.find { setOfSites -> setOfSites.contains(mine) } == null)
                sets[mine] = graph.getPartOfGraph(punter, mine)
        }
        return sets
    }

    private fun getEnemyWays(punter: Int): MutableMap<Pair<Int, Int>, LinkedList<River>> {
        val ways = mutableMapOf<Pair<Int, Int>, LinkedList<River>>()
        val enemySets = getEnemySets(punter)
        for ((firstMine, firstSetOfMines) in enemySets) {
            val directions = findDirections(punter, firstSetOfMines)
            for (secondMine in enemySets.keys) {
                val way = LinkedList<River>()
                var to = secondMine
                var from = directions[to] ?: continue
                while (from != -1) {
                    if (graph.getNeighbors(from)[to] == -1) way.addFirst(River(from, to))
                    to = from
                    from = directions[to]!!
                }
                if (way.isNotEmpty()) ways[Pair(firstMine, secondMine)] = way
            }
        }
        return ways
    }

    private fun findDirections(punter: Int, set: Set<Int>): MutableMap<Int, Int> {
        val queue = mutableListOf<Int>()
        queue.addAll(set)
        val visited = mutableMapOf<Int, Int>()
        set.forEach { site -> visited[site] = -1 }
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in graph.getNeighbors(current)) {
                if (siteState != -1 && siteState != punter || neighbor in visited) continue
                val partOfGraph = graph.getPartOfGraph(punter, neighbor)
                queue.addAll(partOfGraph)
                visited[neighbor] = current
                if (partOfGraph.size != 1) {
                    val tempQueue = mutableListOf(neighbor)
                    while (tempQueue.isNotEmpty()) {
                        val tempCurrent = tempQueue[0]
                        tempQueue.removeAt(0)
                        for (tempNeighbor in graph.getNeighbors(tempCurrent).keys) {
                            if (tempNeighbor !in partOfGraph || tempNeighbor in visited) continue
                            tempQueue.add(tempNeighbor)
                            visited[tempNeighbor] = tempCurrent
                        }
                    }
                }
            }
        }
        return visited
    }

    private fun findImportantSites(): MutableSet<Int> {//вершины рядом с mines на путях соединения
        val importantSites = mutableSetOf<Int>()
        graph.getAllMines().forEach { mine ->
            val queue = mutableListOf(mine)
            val visited = mutableSetOf(mine)
            while (queue.isNotEmpty()) {
                val current = queue[0]
                queue.removeAt(0)
                for ((neighbor, siteState) in graph.getNeighbors(current)) {
                    if (siteState != -1 && siteState != graph.myId || neighbor in visited) continue
                    if (graph.getAllMines().contains(neighbor)) importantSites.add(current)
                    queue.add(neighbor)
                    visited.add(neighbor)
                }
            }
        }
        return importantSites
    }

    private fun getIncidenceRivers(punter: Int): Map<River, Int> {
        val ways = if (punter == graph.myId) getWays() else getEnemyWays(punter)
        val waysIncidence = mutableMapOf<River, Int>()
        val rivers = mutableSetOf<River>()
        for (way in ways.values) {
            way.forEach { river ->
                rivers.add(river)
                waysIncidence[river] = 0
            }
        }
        ways.values.forEach { way -> way.forEach { river -> waysIncidence[river] = waysIncidence[river]!! + 1 } }
        return waysIncidence
    }

    private fun getGlobalBridge(punter: Int): River {
        val waysIncidence = getIncidenceRivers(punter)
        var result: River? = null
        var maxIncidence = 0
        for ((river, incidence) in waysIncidence.filter { (river, _) ->
            graph.isBridgeInDepth(punter, Int.MAX_VALUE, river.source, river.target) }) {
            if (incidence > maxIncidence) {
                maxIncidence = incidence
                result = river
            }
        }
        if (result != null) return result
        throw IllegalArgumentException()
    }

    private fun getGlobalRiver(punter: Int): River {
        val waysIncidence = getIncidenceRivers(punter)
        var result: River? = null
        var maxIncidence = 3
        for ((river, incidence) in waysIncidence) {
            if (incidence >= maxIncidence) {
                maxIncidence = incidence
                result = river
            }
        }
        if (result != null) return result
        throw IllegalArgumentException()
    }

    private fun try00(): River {
        var source = -1
        var min = Int.MAX_VALUE
        graph.getAllMines().filter { mine -> graph.getNeighbors(mine).keys.count { neighbor ->
            graph.getNeighbors(mine)[neighbor] == graph.myId } == 0 && graph.getNeighbors(mine).keys.count { neighbor ->
            graph.getNeighbors(mine)[neighbor] == -1 } in 1 until graph.punters }.forEach { mine ->
            val sum = graph.getNeighbors(mine).keys.count { neighbor -> graph.getNeighbors(mine)[neighbor] == -1 }
            if (sum < min) {
                source = mine
                min = sum
            }
        }
        val importantSites = findImportantSites()
        if (source != -1) {//захват рек у mines не имеющих our, имеющих от 1 до punters нейтральных
            println("@@@@@@@@@@@@@____0")
            val neutralNeighbors = graph.getNeighbors(source).keys.filter { neighbor -> graph.getNeighbors(source)[neighbor] == -1 }
            val target = neutralNeighbors.find { neighbor ->
                importantSites.contains(neighbor) && graph.isBridgeInDepth(graph.myId, shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor ->
                importantSites.contains(neighbor) && graph.isSingleRiverInLocalArea(shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor ->
                graph.isBridgeInDepth(graph.myId, shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor ->
                graph.isSingleRiverInLocalArea(shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor -> importantSites.contains(neighbor) }
                    ?: neutralNeighbors[0]//unreal??
            return River(source, target)
        }
        graph.getAllMines().forEach { mine ->//захват рек на соединении mines и одновременно мостов
            val target = graph.getNeighbors(mine).keys.find { neighbor ->
                        graph.getNeighbors(mine)[neighbor] == -1
                        && graph.isBridgeInDepth(graph.myId, shallowDepth, mine, neighbor)
                        && importantSites.contains(neighbor) }
            if (target != null) {
                println("@@@@@@@@@@@@@____1")
                return River(mine, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват рек на соединении mines и одновременно одиночных в локальной области рек
            val target = graph.getNeighbors(mine).keys.find { neighbor ->
                        graph.getNeighbors(mine)[neighbor] == -1
                        && graph.isSingleRiverInLocalArea(shallowDepth, mine, neighbor)
                        && importantSites.contains(neighbor) }
            if (target != null) {
                println("@@@@@@@@@@@@@____2")
                return River(mine, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват мостов
            val target = graph.getNeighbors(mine).keys.find { neighbor ->
                        graph.getNeighbors(mine)[neighbor] == -1
                        && graph.isBridgeInDepth(graph.myId, shallowDepth, mine, neighbor) }
            if (target != null) {
                println("@@@@@@@@@@@@@____3")
                return River(mine, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват одиночных в локальной области рек
            val target = graph.getNeighbors(mine).keys.find { neighbor ->
                        graph.getNeighbors(mine)[neighbor] == -1
                        && graph.isSingleRiverInLocalArea(shallowDepth, mine, neighbor) }
            if (target != null) {
                println("@@@@@@@@@@@@@____4")
                return River(mine, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват рек на соединении mines
            val target = graph.getNeighbors(mine).keys.find { neighbor ->
                graph.getNeighbors(mine)[neighbor] == -1  && importantSites.contains(neighbor) }
            if (target != null) {
                println("@@@@@@@@@@@@@____5")
                return River(mine, target)
            }
        }
        //захват рек у mines закончен
        if (graph.getAllSetsOfMines().size < 2) {
            gameStage++
            graph.setAreaWeights()
            updateMaxSiteAndSetContainsItInArea()
        } else updateCurrentAndNextSetsOfMines()
        throw IllegalArgumentException()
    }

    //Если одно из множеств соединилась с сторонним, то происходит перевыбор множеств
    private fun try11(): River {
        while (true) {
            try {
                try {
                    return getGlobalBridge(graph.myId)//захват глобальных мостов
                } catch (e: IllegalArgumentException) {}
                if (graph.punters > 1) try {//противник(и) есть! захват глобальных мостов противника
                    return getGlobalBridge(currentEnemy)
                } catch (e: IllegalArgumentException) {} finally { updateCurrentEnemy() }
                if (graph.punters > 1) try {//противник(и) есть! захват наиболее часто встречающихся рек противника
                    return getGlobalRiver(currentEnemy)
                } catch (e: IllegalArgumentException) {} finally { updateCurrentEnemy() }
                try {
                    return getGlobalRiver(graph.myId)//захват наиболее часто встречающихся рек
                } catch (e: IllegalArgumentException) {}
                val way = getWayForTry1(currentSetOfMines, nextSetOfMines)
                val temp = currentSetOfMines
                currentSetOfMines = nextSetOfMines
                nextSetOfMines = temp
                val riversDepths = mutableMapOf<River, Int>()
                way.forEach { river ->
                    riversDepths[river] = graph.depthOfBridge(graph.myId, Int.MAX_VALUE, river.source, river.target)
                }
                var maxDepth = -1
                var result = River(-1, -1)
                riversDepths.forEach { river, depth ->
                    if (depth > maxDepth) {
                        maxDepth = depth
                        result = river
                    }
                }
                if (result.source == -1 || result.target == -1) throw Exception()
                return result
            } catch (e: IllegalArgumentException) {
                try {
                    updateCurrentAndNextSetsOfMines()
                } catch (e: IllegalArgumentException) {
                    graph.setAreaWeights()
                    updateMaxSiteAndSetContainsItInArea()
                    throw e
                }
            }
        }
    }

    private fun try22(): River {
        while (true) {
            try {
                val way = getWayForTry2(setContainsMaxSiteInArea, maxSite)
                val riversDepths = mutableMapOf<River, Int>()
                way.forEach { river ->
                    riversDepths[river] = graph.depthOfBridge(graph.myId, Int.MAX_VALUE, river.source, river.target)
                }
                var maxDepth = -1
                var result: River? = null
                riversDepths.forEach { river, depth ->
                    if (depth > maxDepth) {
                        maxDepth = depth
                        result = river
                    }
                }
                return result!!
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                updateMaxSiteAndSetContainsItInArea()
            }
        }
    }

    /*private fun getMaxPointsEnemy(): Int {//если игроков больше одного
        graph.findPoints()
        var maxPoints = -1L
        var maxPunter = -1
        for (punter in 0 until graph.punters) {
            if (punter == graph.myId) continue
            if (graph.points[punter]!! > maxPoints) {
                maxPoints = graph.points[punter]!!
                maxPunter = punter
            }
        }
        return maxPunter
    }*/

    //Закрывать доступ к site с большим весом?
    private fun try25(): River {
        //val maxEnemy = getMaxPointsEnemy()
        val enemySets = getEnemySets(currentEnemy)
        val areas = mutableSetOf<Int>()
        enemySets.values.forEach { set ->
            areas.addAll(graph.findArea(currentEnemy, set))
        }
        for (source in areas) {
            val target = graph.getNeighbors(source).keys.find { neighbor -> graph.getNeighbors(source)[neighbor] == -1 }
            if (target != null) return River(source, target)
        }
        throw IllegalArgumentException()
    }

    //захват оставшихся
    private fun try33(): River {
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
                0 -> try00()
                1 -> try11()
                2 -> try22()
                3 -> try25()
                4 -> try33()
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