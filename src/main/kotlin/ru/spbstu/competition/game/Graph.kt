package ru.spbstu.competition.game

import ru.spbstu.competition.protocol.data.Claim
import ru.spbstu.competition.protocol.data.River
import ru.spbstu.competition.protocol.data.Setup

class Graph {
    var myId = -1
    private val sites = mutableMapOf<Int, Site>()
    private val mines = mutableSetOf<Int>()
    private val rivers = mutableSetOf<River>()
    private val setsOfMines = mutableMapOf<Int, SetOfMines>()
    val ourSites = mutableSetOf<Int>()
    
    private class Site {
        val neighbors = mutableMapOf<Int, Int>()
        val distance = mutableMapOf<Int, Int>()
        var weight = 0
    }

    private class SetOfMines(id: Int) {
        var mines = mutableSetOf(id)
        var sites = mutableSetOf(id)
        var area = mutableSetOf<Int>()
        var incompatibleSets = mutableSetOf<Int>()
    }

    //Следить за врагами, вести счёт, записать количество игроков
    fun init(setup: Setup) {
        myId = setup.punter
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

    fun getIncompatibleSetsBySetId(setId: Int): MutableSet<Int> = setsOfMines[setId]!!.incompatibleSets

    fun setIncompatibleSetsBySetId(firstSetId: Int, secondSetId: Int) {
        getIncompatibleSetsBySetId(firstSetId).add(secondSetId)
        getIncompatibleSetsBySetId(secondSetId).add(firstSetId)
    }

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
            //setsOfMines[setContainsSecond]!!.sites.add(first)
            ////setsOfMines[setContainsSecond]!!.neighbors.remove(first)
            //setsOfMines[setContainsSecond]!!.neighbors.addAll(getPartOfGraphNeighbors(first))
            ////setsOfMines[setContainsSecond]!!.neighbors.addAll(getNeighbors(first)
                    ////.filter { (id, siteState) -> siteState == SiteState.Neutral
                            ////&& !getSitesBySetId(setContainsSecond)!!.contains(id) }.keys)
            setsOfMines[setContainsSecond]!!.sites.addAll(getPartOfGraph(first))
            return
        }
        if (setContainsSecond == -1) {
            //setsOfMines[setContainsFirst]!!.sites.add(second)
            ////setsOfMines[setContainsFirst]!!.neighbors.remove(second)
            //setsOfMines[setContainsFirst]!!.neighbors.addAll(getPartOfGraphNeighbors(second))
            ////setsOfMines[setContainsFirst]!!.neighbors.addAll(getNeighbors(second)
                    ////.filter { (id, siteState) -> siteState == SiteState.Neutral
                            ////&& !getSitesBySetId(setContainsFirst)!!.contains(id) }.keys)
            setsOfMines[setContainsFirst]!!.sites.addAll(getPartOfGraph(second))
            return
        }
        if (setContainsFirst != setContainsSecond) {
            //set1 + set2
            val newSetOfMines = (setsOfMines[setContainsFirst]!!.mines + setsOfMines[setContainsSecond]!!.mines).toMutableSet()
            val newSetOfSites = (setsOfMines[setContainsFirst]!!.sites + setsOfMines[setContainsSecond]!!.sites).toMutableSet()
            val newSetOfIncompatibleSets = (setsOfMines[setContainsFirst]!!.incompatibleSets
                    + setsOfMines[setContainsSecond]!!.incompatibleSets).toMutableSet()
            ////пересчёт neighbors
            //setsOfMines[setContainsFirst]!!.neighbors.remove(second)
            //setsOfMines[setContainsSecond]!!.neighbors.remove(first)
            ////remove more neighbors
            //val newSetOfNeighbors = setsOfMines[setContainsFirst]!!.sites + setsOfMines[setContainsSecond]!!.sites
            setsOfMines.remove(setContainsSecond)
            setsOfMines[setContainsFirst]!!.mines = newSetOfMines
            setsOfMines[setContainsFirst]!!.sites = newSetOfSites
            setsOfMines[setContainsFirst]!!.incompatibleSets = newSetOfIncompatibleSets
            for (incompatibleSet in newSetOfIncompatibleSets) {
                setsOfMines[incompatibleSet]!!.incompatibleSets.remove(setContainsSecond)
                setsOfMines[incompatibleSet]!!.incompatibleSets.add(setContainsFirst)
            }
        }
    }

    /*private fun updateAllBorders(setId: Int) {

    }

    private fun updateBordersOnOurClaim(setId: Int, first: Int, second: Int) {//first - our, second - partOfGraph
        getBordersBySetId(setId).remove(first)

        getPartOfGraph(second).forEach { site -> getBordersBySetId(setId) }



        getPartOfGraph(second)
        if (getNeighbors(second).keys.find { neighbor -> getNeighbors(second)[neighbor] == SiteState.Neutral
                && !getSitesBySetId(setId).contains(neighbor) } != null) getBordersBySetId(setId).add(second)
    }

    private fun updateBordersOnEnemyClaim(first: Int, second: Int) {
        getBordersBySetId(setId).remove(first)
        getBordersBySetId(setId).remove(second)
        if (ourSites.contains(first) && getNeighbors(first).keys.find { neighbor -> getNeighbors(first)[neighbor] == SiteState.Neutral
                && !getSitesBySetId(setId).contains(neighbor) } != null) getBordersBySetId(setId).add(first)
        if (ourSites.contains(second) && getNeighbors(second).keys.find { neighbor -> getNeighbors(second)[neighbor] == SiteState.Neutral
                && !getSitesBySetId(setId).contains(neighbor) } != null) getBordersBySetId(setId).add(second)
    }

    private fun updateBorders() {//не оптимально проверять всё во всех ситуациях
        setsOfMines.values.forEach { setOfMines ->
            setOfMines.borders.clear()
            setOfMines.borders.addAll(setOfMines.sites.filter { site -> getNeighbors(site).keys.find { key ->
                getNeighbors(site)[key] == SiteState.Neutral && !setOfMines.sites.contains(key) } != null }) }
    }*/

    fun getPartOfGraph(begin: Int): Set<Int> {
        val queue = mutableListOf<Int>()
        queue.add(begin)
        val visited = mutableSetOf(begin)
        while (queue.isNotEmpty()) {
            val next = queue[0]
            queue.removeAt(0)
            for ((id, neighbor) in sites[next]!!.neighbors) {
                if (id !in visited && neighbor == myId) {
                    queue.add(id)
                    visited.add(id)
                }
            }
        }
        return visited
    }

    private fun findArea(setId: Int): MutableSet<Int> {
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
        return visited
    }

    fun setAreaWeights() = setsOfMines.keys.forEach { setId -> setAreaWeightsBySetId(setId) }

    private fun setAreaWeightsBySetId(setId: Int) {
        val area = findArea(setId)
        area.forEach { site -> setWeight(site, getMinesBySetId(setId).map { getDistance(site)[it]!! }.sum()) }
        println("AREA SIZE = ${area.size}      MAX WEIGHT = ${area.map { site -> getWeight(site) }.max()}")
        getAreaBySetId(setId).addAll(area + getSitesBySetId(setId))
    }

    private fun findSitesDistances(mine: Int) {
        val queue = mutableListOf<Int>()
        queue.add(mine)
        sites[mine]!!.distance.put(mine, 0)
        val visited = mutableSetOf(mine)
        while (queue.isNotEmpty()) {
            val next = queue[0]
            queue.removeAt(0)
            for (neighbor in sites[next]!!.neighbors.keys) {
                if (neighbor !in visited) {
                    sites[neighbor]!!.distance.put(mine, sites[next]!!.distance[mine]!! + 1)
                    queue.add(neighbor)
                    visited.add(neighbor)
                }
            }
        }
    }
}