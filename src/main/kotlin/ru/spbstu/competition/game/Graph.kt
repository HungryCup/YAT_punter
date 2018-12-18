package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.data.Claim
import ru.spbstu.competition.protocol.data.River
import ru.spbstu.competition.protocol.data.Setup

class Graph {
    var myId = -1
    var punters = -1
    private val sites = mutableMapOf<Int, Site>()
    private val mines = mutableSetOf<Int>()
    private val rivers = mutableSetOf<River>()
    private val setsOfMines = mutableMapOf<Int, SetOfMines>()

    private class Site {
        val neighbors = mutableMapOf<Int, Int>()
        val distance = mutableMapOf<Int, Int>()
        var weight = 0
    }

    private class SetOfMines(id: Int) {
        var mines = mutableSetOf(id)
        var sites = mutableSetOf(id)
        var area = mutableSetOf<Int>()
    }

    fun init(setup: Setup) {
        myId = setup.punter
        punters = setup.punters
        for ((id) in setup.map.sites) {
            addSite(id)
        }
        for (river in setup.map.rivers) {
            connect(river.source, river.target)
            setNeighborsState(river.source, river.target, -1)
            rivers.add(river)
        }
        for (mineId in setup.map.mines) {
            mines.add(mineId)
            addSetOfMines(mineId)
            sites.values.forEach { site -> site.distance.put(mineId, 0) }
            findSitesDistances(mineId)
        }
        sites.values.forEach { site -> site.distance.keys
                .forEach { key -> site.distance[key] = site.distance[key]!! * site.distance[key]!!}}
    }

    fun getAllMines() = mines

    fun getAllSetsOfMines() = setsOfMines.keys

    fun getAllSites() = sites.keys

    fun getAllRivers() = rivers

    fun getNeighbors(id: Int) = sites[id]!!.neighbors

    fun getDistance(id: Int) = sites[id]!!.distance

    fun getSitesBySetId(setId: Int) = setsOfMines[setId]!!.sites

    fun getMinesBySetId(setId: Int) = setsOfMines[setId]!!.mines

    fun getAreaBySetId(setId: Int) = setsOfMines[setId]!!.area

    fun setAreaBySetId(setId: Int, area: MutableSet<Int>) {
        setsOfMines[setId]!!.area = area
    }

    fun getWeight(id: Int) = sites[id]!!.weight

    fun setWeight(id: Int, value: Int) {
        sites[id]!!.weight = value
    }

    private fun addSite(id: Int) {
        sites[id] = Site()
    }

    private fun connect(first: Int, second: Int) {
        sites[first]!!.neighbors.put(second, -1)
        sites[second]!!.neighbors.put(first, -1)
    }

    private fun addSetOfMines(id: Int) {
        setsOfMines[id] = SetOfMines(id)
    }

    private fun setNeighborsState(first: Int, second: Int, siteState: Int) {
        sites[first]!!.neighbors[second] = siteState
        sites[second]!!.neighbors[first] = siteState
    }

    fun isBridgeInDepth(punter: Int, depth: Int, source: Int, target: Int)
            = depthOfBridge(punter, depth, source, target) == depth

    fun depthOfBridge(punter: Int, depth: Int, source: Int, target: Int): Int {//is bridge when return = depth
        val realSiteState = getNeighbors(source)[target]!!//or -1
        setNeighborsState(source, target, punter + 1)
        var result = depth
        val setContainsTarget = getPartOfGraph(punter, target)
        val setContainsSource = getPartOfGraph(punter, source)
        val queue = mutableListOf<Int>()
        queue.addAll(setContainsTarget)
        val visited = mutableMapOf<Int, Int>()
        setContainsTarget.forEach { site -> visited[site] = 0}
        queueLoop@while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            val currentDepth = visited[current]!! + 1
            if (currentDepth > depth) break
            for ((neighbor, siteState) in getNeighbors(current)) {
                if (siteState != -1 && siteState != punter || neighbor in visited.keys) continue
                if (setContainsSource.contains(neighbor)) {
                    result = currentDepth
                    break@queueLoop
                }
                val partOfGraph = getPartOfGraph(punter, neighbor)
                queue.addAll(partOfGraph)
                partOfGraph.forEach { site -> visited[site] = currentDepth }
            }
        }
        setNeighborsState(source, target, realSiteState)//-1
        return result
    }

    fun isSingleRiverInLocalArea(depth: Int, mine: Int, target: Int): Boolean {
        val realSiteState = getNeighbors(mine)[target]!!//or -1
        setNeighborsState(mine, target, myId + 1)
        var result = true
        val setContainsTarget = getPartOfGraph(myId, target)
        val setContainsSource = getPartOfGraph(myId, mine)
        val queue = mutableListOf<Int>()
        queue.addAll(setContainsTarget)
        val visited = mutableMapOf<Int, Int>()
        setContainsTarget.forEach { site -> visited[site] = 0}
        queueLoop@while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            val currentDepth = visited[current]!! + 1
            if (currentDepth > depth) break//is bridge
            for ((neighbor, siteState) in getNeighbors(current)) {
                if (siteState != -1 && siteState != myId || neighbor in visited.keys) continue
                if (setContainsSource.contains(neighbor)) {
                    if (getAllMines().contains(neighbor)) {
                        result = true
                        break
                    } else {
                        result = false
                        break@queueLoop
                    }
                }
                val partOfGraph = getPartOfGraph(myId, neighbor)
                queue.addAll(partOfGraph)
                partOfGraph.forEach { site -> visited[site] = currentDepth}
            }
        }
        setNeighborsState(mine, target, realSiteState)//-1
        return result
    }

    fun update(claim: Claim) {
        setNeighborsState(claim.source, claim.target, claim.punter)
        if (claim.punter != myId) return
        var setContainsFirst = -1
        var setContainsSecond = -1
        for ((id, setOfMines) in setsOfMines) {
            if (setOfMines.sites.contains(claim.source)) setContainsFirst = id
            if (setOfMines.sites.contains(claim.target)) setContainsSecond = id
        }
        println("$setContainsFirst,    $setContainsSecond")
        println("${setsOfMines.keys}")
        if (setContainsFirst == -1 && setContainsSecond == -1) return
        if (setContainsFirst == -1) {
            setsOfMines[setContainsSecond]!!.sites.addAll(getPartOfGraph(myId, claim.source))
            return
        }
        if (setContainsSecond == -1) {
            setsOfMines[setContainsFirst]!!.sites.addAll(getPartOfGraph(myId, claim.target))
            return
        }
        if (setContainsFirst != setContainsSecond) {
            val newSetOfMines = (setsOfMines[setContainsFirst]!!.mines + setsOfMines[setContainsSecond]!!.mines).toMutableSet()
            val newSetOfSites = (setsOfMines[setContainsFirst]!!.sites + setsOfMines[setContainsSecond]!!.sites).toMutableSet()
            setsOfMines.remove(setContainsSecond)
            setsOfMines[setContainsFirst]!!.mines = newSetOfMines
            setsOfMines[setContainsFirst]!!.sites = newSetOfSites
        }
    }

    fun getPartOfGraph(punter: Int, begin: Int): Set<Int> {
        val queue = mutableListOf(begin)
        val visited = mutableSetOf(begin)
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in sites[current]!!.neighbors) {
                if (neighbor !in visited && siteState == punter) {
                    queue.add(neighbor)
                    visited.add(neighbor)
                }
            }
        }
        return visited
    }

    fun findArea(punter: Int, set: Set<Int>): Set<Int> {
        val queue = mutableListOf<Int>()
        queue.addAll(set)
        val visited = mutableSetOf<Int>()
        visited.addAll(set)
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in getNeighbors(current)) {
                if (siteState != -1 && siteState != punter || neighbor in visited) continue
                queue.add(neighbor)
                visited.add(neighbor)
            }
        }
        visited.removeAll(set)
        return visited
    }

    fun setAreaWeights() = setsOfMines.keys.forEach { setId -> setAreaWeightsBySetId(setId) }

    private fun setAreaWeightsBySetId(setId: Int) {
        setAreaBySetId(setId, findArea(myId, getSitesBySetId(setId)).toMutableSet())
        getAreaBySetId(setId).forEach { site ->
            setWeight(site, getMinesBySetId(setId).map { getDistance(site)[it]!! }.sum()) }
    }

    private fun findSitesDistances(mine: Int) {
        val queue = mutableListOf(mine)
        val visited = mutableSetOf(mine)
        sites[mine]!!.distance.put(mine, 0)
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for (neighbor in getNeighbors(current).keys) {
                if (neighbor in visited) continue
                sites[neighbor]!!.distance.put(mine, sites[current]!!.distance[mine]!! + 1)
                queue.add(neighbor)
                visited.add(neighbor)
            }
        }
    }
}