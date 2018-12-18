
package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.Protocol
import ru.spbstu.competition.protocol.data.River
import java.util.*

class Intellect(val graph: Graph, val protocol: Protocol) {

    val smallDepth = 3//try 1,2
    val shallowDepth = 15//try 15 for triangle and icfp
    var gameStage = 0
    var currentSetOfMines = -1
    var nextSetOfMines = -1
    var maxSite = -1
    var setContainsMaxSiteInArea = -1

    fun init() {
        if (graph.getAllMines().size < 2) {//особый режим игры?
            return
        }
    }

    private fun getWayForTry1(firstSet: Int, secondSet: Int): LinkedList<River> {
        if (!graph.getAllSetsOfMines().contains(firstSet) || !graph.getAllSetsOfMines().contains(secondSet))
            throw IllegalArgumentException("set(s) doesn't exist")
        if (firstSet == secondSet) throw IllegalArgumentException("it is one set")
        val way = getWay(graph.getSitesBySetId(firstSet), graph.getSitesBySetId(secondSet))
        if (way.isEmpty()) throw IllegalArgumentException("impossible to connect")
        return way
    }

    private fun getWayForTry2(setOfMines: Int, maxSite: Int): LinkedList<River> {
        if (!graph.getAllSetsOfMines().contains(setOfMines) || !graph.getAllSites().contains(maxSite))
            throw IllegalArgumentException("set or site doesn't exist")
        if (graph.getSitesBySetId(setOfMines).contains(maxSite)) throw IllegalArgumentException("already connected")
        val way = getWay(graph.getSitesBySetId(setOfMines), graph.getPartOfGraph(maxSite))
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
            graph.findArea(setOfMines)
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
        println("$max points has $maxSite")
    }

    private fun getWay(firstSet: Set<Int>, secondSet: Set<Int>): LinkedList<River> {
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
                if (siteState != -1 && siteState != graph.myId || neighbor in visited) continue
                if (secondSet.contains(neighbor)) {
                    visited[neighbor] = current
                    to = neighbor
                    from = current
                    break@queueLoop
                }
                val partOfGraph = graph.getPartOfGraph(neighbor)
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
            val directions = findDirections(firstSetOfMines)
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

    private fun findDirections(setOfMines: Int): MutableMap<Int, Int> {
        if (!graph.getAllSetsOfMines().contains(setOfMines)) throw IllegalArgumentException("set doesn't exist")
        val queue = mutableListOf<Int>()
        queue.addAll(graph.getSitesBySetId(setOfMines))
        val visited = mutableMapOf<Int, Int>()
        graph.getSitesBySetId(setOfMines).forEach { site -> visited[site] = -1 }
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in graph.getNeighbors(current)) {
                if (siteState != -1 && siteState != graph.myId || neighbor in visited) continue
                val partOfGraph = graph.getPartOfGraph(neighbor)
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

    private fun findAllBridges() {
        var i = 0
        for ((source, target) in graph.getAllRivers()) {
            if (graph.getNeighbors(source)[target] != -1) continue
            if (graph.isBridgeInDepth(shallowDepth, source, target)) {
                println(i++)
                allBridges.add(River(source, target))
            }
        }
    }

    private val allBridges = mutableSetOf<River>()

    private fun try15(): River {
        allBridges.forEach { river ->
            if (graph.getNeighbors(river.source)[river.target] == -1) {
                allBridges.remove(river)
                return river
            }
        }
        throw IllegalArgumentException()
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

    private fun checkFreedom(): River {
        val ways = getWays()
        val waysIncidence = mutableMapOf<River, Int>()
        val rivers = mutableSetOf<River>()
        for (way in ways.values) {
            way.forEach { river ->
                rivers.add(river)
                waysIncidence[river] = 0
            }
        }
        ways.values.forEach { way -> way.forEach { river -> waysIncidence[river] = waysIncidence[river]!! + 1 } }
        return River(-1, -1)
    }

    private fun getGlobalRiver(): River {
        val ways = getWays()
        val waysIncidence = mutableMapOf<River, Int>()
        val rivers = mutableSetOf<River>()
        for (way in ways.values) {
            way.forEach { river ->
                rivers.add(river)
                waysIncidence[river] = 0
            }
        }
        ways.values.forEach { way -> way.forEach { river -> waysIncidence[river] = waysIncidence[river]!! + 1 } }
        var maxIncidence = 0
        var result: River? = null
        for ((river, incidence) in waysIncidence.filter { (river, _) ->//in global bridges
            graph.depthOfBridge(Int.MAX_VALUE, river.source, river.target) == Int.MAX_VALUE }) {
            if (incidence > maxIncidence) {
                maxIncidence = incidence
                result = river
            }
        }
        if (result != null) return result
        maxIncidence = 1
        for ((river, incidence) in waysIncidence) {
            if (incidence > maxIncidence) {
                maxIncidence = incidence
                result = river
            }
        }
        if (result != null) return result
        throw IllegalArgumentException()
    }

    private fun try00(): River {
        var source = -1
        var target = -1
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
            target = neutralNeighbors.find { neighbor -> importantSites.contains(neighbor) && graph.isBridgeInDepth(shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor -> importantSites.contains(neighbor) && graph.isSingleRiverInLocalArea(shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor -> graph.isBridgeInDepth(shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor -> graph.isSingleRiverInLocalArea(shallowDepth, source, neighbor) }
                    ?: neutralNeighbors.find { neighbor -> importantSites.contains(neighbor) }
                    ?: neutralNeighbors[0]//unreal??
            return River(source, target)
        }
        graph.getAllMines().forEach { mine ->//захват рек на соединении mines и одновременно мостов
            val temp = graph.getNeighbors(mine).keys.find { neighbor ->
                graph.getNeighbors(mine)[neighbor] == -1  && graph.isBridgeInDepth(shallowDepth, mine, neighbor) && importantSites.contains(neighbor) }
            if (temp != null) {
                source = mine
                target = temp
                println("@@@@@@@@@@@@@____1")
                return River(source, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват рек на соединении mines и одновременно одиночных в локальной области рек
            val temp = graph.getNeighbors(mine).keys.find { neighbor ->
                graph.getNeighbors(mine)[neighbor] == -1  && graph.isSingleRiverInLocalArea(shallowDepth, mine, neighbor) && importantSites.contains(neighbor) }
            if (temp != null) {
                source = mine
                target = temp
                println("@@@@@@@@@@@@@____2")
                return River(source, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват мостов
            val temp = graph.getNeighbors(mine).keys.find { neighbor ->
                graph.getNeighbors(mine)[neighbor] == -1  && graph.isBridgeInDepth(shallowDepth, mine, neighbor) }
            if (temp != null) {
                source = mine
                target = temp
                println("@@@@@@@@@@@@@____3")
                return River(source, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват одиночных в локальной области рек
            val temp = graph.getNeighbors(mine).keys.find { neighbor ->
                graph.getNeighbors(mine)[neighbor] == -1  && graph.isSingleRiverInLocalArea(shallowDepth, mine, neighbor) }
            if (temp != null) {
                source = mine
                target = temp
                println("@@@@@@@@@@@@@____4")
                return River(source, target)
            }
        }
        graph.getAllMines().forEach { mine ->//захват рек на соединении mines
            val temp = graph.getNeighbors(mine).keys.find { neighbor ->
                graph.getNeighbors(mine)[neighbor] == -1  && importantSites.contains(neighbor) }
            if (temp != null) {
                source = mine
                target = temp
                println("@@@@@@@@@@@@@____5")
                return River(source, target)
            }
        }
        //захват рек у mines закончен
        updateCurrentAndNextSetsOfMines()
        //findAllBridges()
        throw IllegalArgumentException()
    }

    //try 0.25 блочить лёгие mine противника
    //try 0.5: отойти от mines на более безопасное расстояние
    //try 0.75: занять мосты
    //try1: занимать реки наперёд, в тех местах, где меньше разветвлений
    //На протяжении всего try1, если нас пытаются заблочить, поддерживать свою свободу

    //Если одно из множеств соединилась с сторонним, то происходит перевыбор множеств
    private fun try11(): River {
        if (graph.getAllSetsOfMines().size < 2) {//or (... == 1)
            graph.setAreaWeights()
            updateMaxSiteAndSetContainsItInArea()
            throw IllegalArgumentException()
        }
        while (true) {
            try {
                val way = getWayForTry1(currentSetOfMines, nextSetOfMines)
                val temp = currentSetOfMines
                currentSetOfMines = nextSetOfMines
                nextSetOfMines = temp
                val riversDepths = mutableMapOf<River, Int>()
                way.forEach { river ->
                    riversDepths[river] = graph.depthOfBridge(Int.MAX_VALUE, river.source, river.target)
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
                //way.forEach { river ->
                //    if (graph.isBridgeInDepth(Int.MAX_VALUE, river.source, river.target)) return river
                //}
                //way.forEach { river ->
                //    if (graph.isBridgeInDepth(shallowDepth, river.source, river.target)) return river
                //}
                //way.forEach { river ->
                //    if (graph.isBridgeInDepth(smallDepth, river.source, river.target)) return river
                //}
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

    //try2 пускать щупальца в стороны (к site с большим весом)
    //Закрывать доступ к site с большим весом?
    //занимать от наших к max (мосты в том числе)
    private fun try22(): River {
        while (true) {
            try {
                val way = getWayForTry2(setContainsMaxSiteInArea, maxSite)
                val riversDepths = mutableMapOf<River, Int>()
                way.forEach { river ->
                    riversDepths[river] = graph.depthOfBridge(Int.MAX_VALUE, river.source, river.target)
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
                e.printStackTrace()
                updateMaxSiteAndSetContainsItInArea()
            }
        }
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
                //0 -> try00()
                //1 -> getGlobalRiver()
                //2 -> try11()
                //3 -> try22()
                //4 -> try3()
                0 -> try00()
                1 -> try11()
                2 -> try22()
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