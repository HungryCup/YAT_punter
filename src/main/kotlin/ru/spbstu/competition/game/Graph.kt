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
    private val ourSites = mutableSetOf<Int>()
    
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

    //Следить за врагами, вести счёт
    fun init(setup: Setup) {
        myId = setup.punter
        punters = setup.punters
        println("Num of punters: ${setup.punters}")
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

    fun getNeighborsBySetId(setOfMines: Int): MutableSet<Int> {
        val neighbors = mutableSetOf<Int>()
        getSitesBySetId(setOfMines).forEach { site -> neighbors.addAll(getNeighbors(site).filter { (neighbor, siteState) ->
            siteState == -1 && !getSitesBySetId(setOfMines).contains(neighbor) }.keys) }
        return neighbors
    }

    fun getWeight(id: Int) = sites[id]!!.weight//0, если нет или -1?

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

    fun isBridgeInDepth(depth: Int, source: Int, target: Int): Boolean {
        val realSiteState = getNeighbors(source)[target]!!//не обязвтельно, можно -1
        setNeighborsState(source, target, myId + 1)
        var result = true
        val setContainsTarget = getPartOfGraph(target)
        val setContainsSource = getPartOfGraph(source)
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
                if (siteState != -1 && siteState != myId || neighbor in visited.keys) continue
                if (setContainsSource.contains(neighbor)) {
                    result = false
                    break@queueLoop
                }
                val partOfGraph = getPartOfGraph(neighbor)
                queue.addAll(partOfGraph)
                partOfGraph.forEach { site -> visited[site] = currentDepth}
            }
        }
        setNeighborsState(source, target, realSiteState)//-1
        return result
    }

    fun depthOfBridge(depth: Int, source: Int, target: Int): Int {
        val realSiteState = getNeighbors(source)[target]!!//не обязвтельно, можно -1
        setNeighborsState(source, target, myId + 1)
        var result = depth
        val setContainsTarget = getPartOfGraph(target)
        val setContainsSource = getPartOfGraph(source)
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
                if (siteState != -1 && siteState != myId || neighbor in visited.keys) continue
                if (setContainsSource.contains(neighbor)) {
                    result = currentDepth
                    break@queueLoop
                }
                val partOfGraph = getPartOfGraph(neighbor)
                queue.addAll(partOfGraph)
                partOfGraph.forEach { site -> visited[site] = currentDepth }
            }
        }
        setNeighborsState(source, target, realSiteState)//-1
        return result
    }

    fun isSingleRiverInLocalArea(depth: Int, mine: Int, target: Int): Boolean {
        val realSiteState = getNeighbors(mine)[target]!!//не обязвтельно, можно -1
        setNeighborsState(mine, target, myId + 1)
        var result = true
        val setContainsTarget = getPartOfGraph(target)
        val setContainsSource = getPartOfGraph(mine)
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
                    if (getAllMines().contains(neighbor)) {//like neighbor == mine in special case
                        result = true
                        break
                    } else {
                        result = false
                        break@queueLoop
                    }
                }
                val partOfGraph = getPartOfGraph(neighbor)
                queue.addAll(partOfGraph)
                partOfGraph.forEach { site -> visited[site] = currentDepth}
            }
        }
        setNeighborsState(mine, target, realSiteState)//-1
        return result
    }

    fun update(claim: Claim) {
        when (claim.punter) {
            myId -> {
                setNeighborsState(claim.source, claim.target, myId)
                ourSites.add(claim.source)
                ourSites.add(claim.target)
                updateGraph(claim.source, claim.target)
                println("${claim.source}   ${claim.target} \n")
            }
            else -> {
                setNeighborsState(claim.source, claim.target, claim.punter)
            }
        }
    }

    private fun updateGraph(punter: Int, first: Int, second: Int) {}

    private fun updateGraph(first: Int, second: Int) {
        var setContainsFirst = -1
        var setContainsSecond = -1
        for ((id, setOfMines) in setsOfMines) {
            if (setOfMines.sites.contains(first)) setContainsFirst = id
            if (setOfMines.sites.contains(second)) setContainsSecond = id
        }
        println("$setContainsFirst,    $setContainsSecond")
        println("${setsOfMines.keys}")
        if (setContainsFirst == -1 && setContainsSecond == -1) return
        if (setContainsFirst == -1) {
            setsOfMines[setContainsSecond]!!.sites.addAll(getPartOfGraph(first))
            return
        }
        if (setContainsSecond == -1) {
            setsOfMines[setContainsFirst]!!.sites.addAll(getPartOfGraph(second))
            return
        }
        if (setContainsFirst != setContainsSecond) {
            //set1 + set2
            val newSetOfMines = (setsOfMines[setContainsFirst]!!.mines + setsOfMines[setContainsSecond]!!.mines).toMutableSet()
            val newSetOfSites = (setsOfMines[setContainsFirst]!!.sites + setsOfMines[setContainsSecond]!!.sites).toMutableSet()
            setsOfMines.remove(setContainsSecond)
            setsOfMines[setContainsFirst]!!.mines = newSetOfMines
            setsOfMines[setContainsFirst]!!.sites = newSetOfSites
        }
    }

    fun getPartOfGraph(begin: Int): Set<Int> {
        val queue = mutableListOf(begin)
        val visited = mutableSetOf(begin)
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in sites[current]!!.neighbors) {
                if (neighbor !in visited && siteState == myId) {
                    queue.add(neighbor)
                    visited.add(neighbor)
                }
            }
        }
        return visited
    }

    fun findArea(setId: Int) {
        val queue = mutableListOf<Int>()
        queue.addAll(getSitesBySetId(setId))
        val visited = mutableSetOf<Int>()
        visited.addAll(getSitesBySetId(setId))
        while (queue.isNotEmpty()) {
            val current = queue[0]
            queue.removeAt(0)
            for ((neighbor, siteState) in getNeighbors(current)) {
                if (siteState != -1 && siteState != myId || neighbor in visited) continue
                queue.add(neighbor)
                visited.add(neighbor)
            }
        }
        visited.removeAll(getSitesBySetId(setId))
        setsOfMines[setId]!!.area = visited
    }

    fun setAreaWeights() = setsOfMines.keys.forEach { setId -> setAreaWeightsBySetId(setId) }

    private fun setAreaWeightsBySetId(setId: Int) {
        findArea(setId)
        getAreaBySetId(setId).forEach { site -> setWeight(site, getMinesBySetId(setId).map { getDistance(site)[it]!! }.sum()) }
        println("AREA SIZE = ${getAreaBySetId(setId).size}      MAX WEIGHT = ${getAreaBySetId(setId).map { site -> getWeight(site) }.max()}")
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