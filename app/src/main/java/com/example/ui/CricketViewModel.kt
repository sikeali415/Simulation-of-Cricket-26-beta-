package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CricketDatabase
import com.example.data.local.CricketRepository
import com.example.data.models.*
import com.example.engine.BallOutcome
import com.example.engine.SimulationEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

// Structure to hold standing row
data class LeagueStanding(
    val teamId: String,
    val teamName: String,
    val flagEmoji: String,
    var played: Int = 0,
    var won: Int = 0,
    var lost: Int = 0,
    var points: Int = 0,
    var netRunRate: Double = 0.0
)

data class ScheduledMatch(
    val id: Int,
    val round: Int,
    val homeTeamId: String,
    val awayTeamId: String,
    val format: MatchFormat,
    var isPlayed: Boolean = false,
    var resultText: String = "Scheduled",
    var homeRuns: Int = 0,
    var homeWickets: Int = 0,
    var homeOvers: Double = 0.0,
    var awayRuns: Int = 0,
    var awayWickets: Int = 0,
    var awayOvers: Double = 0.0,
    var winnerId: String? = null
)

class CricketViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CricketRepository
    val matchHistory: StateFlow<List<MatchHistory>>

    init {
        val database = CricketDatabase.getDatabase(application)
        repository = CricketRepository(database.matchHistoryDao())
        matchHistory = repository.allMatches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Comprehensive Players Master List representing the core database
    private val masterPlayersList = mutableListOf(
        // === KINGS PLAYERS ===
        Player("k1", "A. Haddin", PlayerRole.ALL_ROUNDER, 76, 72, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "Reliable under pressure", 6, true, false, "Australia", "Surrey", true, 2.5, "kings", false, 0, 0, 0, 0, 0, 0, "Brad Haddin clone with exceptional tactical reading of the game.", 34, 12),
        Player("k2", "S. Warner", PlayerRole.BATSMAN, 92, 10, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, BattingHand.LEFT_HAND, BowlingType.FAST_BOWLER, null, "Strong power-hitting ability", 1, false, true, "Australia", "Surrey", true, 4.5, "kings", false, 0, 0, 0, 0, 0, 0, "A fiery opener known for his explosive starts and aggressive fielding.", 35, 14),
        Player("k3", "N. Colin", PlayerRole.ALL_ROUNDER, 74, 70, PlayingStyle.BALANCED, DeliveryStyle.SPIN, BattingHand.RIGHT_HAND, BowlingType.OFF_SPIN, null, "Good tactical awareness", 5, false, false, "New Zealand", "Yorkshire", true, 2.0, "kings", false, 0, 0, 0, 0, 0, 0, "Consistent middle-order contributor and handy off-break bowler.", 28, 6),
        Player("k4", "M. Imran", PlayerRole.WICKET_KEEPER, 65, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "Solid glovework", 7, false, false, "Pakistan", "Lahore", false, 0.5, "kings", false, 0, 0, 0, 0, 0, 0, "Reliable wicket-keeper with strong fundamentals in red-ball cricket.", 24, 4),
        Player("k5", "I. Javed", PlayerRole.WICKET_KEEPER, 68, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "Quick between wickets", 7, false, false, "Pakistan", "Karachi", false, 0.6, "kings", false, 0, 0, 0, 0, 0, 0, "Athletic keeper who specializes in rotating strike under pressure.", 26, 5),
        Player("k6", "Nasir Ahmad", PlayerRole.BATSMAN, 72, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.FAST_BOWLER, null, "Technically sound", 3, false, false, "Pakistan", "Multan", false, 0.8, "kings", false, 0, 0, 0, 0, 0, 0, "Classic top-order batter with a penchant for playing the long innings.", 25, 4),
        Player("k14", "Naseem Shah", PlayerRole.BOWLER, 20, 92, PlayingStyle.BALANCED, DeliveryStyle.FAST, BattingHand.RIGHT_HAND, BowlingType.FAST_BOWLER_EXTRA_PACE, null, "Raw pace and bounce", 11, false, false, "Pakistan", "Rawalpindi", false, 4.8, "kings", false, 0, 0, 0, 0, 0, 0, "One of the fastest young bowlers in the world with natural swing.", 21, 5),

        // === STARS PLAYERS ===
        Player("s1", "Sprike Buttler", PlayerRole.WICKET_KEEPER, 88, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "360-degree stroke maker", 1, true, true, "England", "Surrey", true, 4.0, "stars", false, 0, 0, 0, 0, 0, 0, "Innovative batter who can exploit gaps in any set field.", 33, 11),
        Player("s12", "Azam Khan", PlayerRole.WICKET_KEEPER, 80, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "Exceptional power", 6, true, true, "Pakistan", "Karachi", false, 2.5, "stars", false, 0, 0, 0, 0, 0, 0, "Heavyweight hitter specialized in clearing the ropes with ease.", 25, 6),

        // === GLADIATORS PLAYERS ===
        Player("g1", "Babar Azam", PlayerRole.BATSMAN, 94, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "World No. 1 batter", 3, false, false, "Pakistan", "Lahore", false, 5.5, "gladiators", true, 0, 0, 0, 0, 0, 0, "Modern day great with impeccable cover drives and batting technique.", 29, 9),
        Player("g2", "Fakhar Zaman", PlayerRole.BATSMAN, 88, 10, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, BattingHand.LEFT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "Explosive opener", 1, false, true, "Pakistan", "Mardan", false, 4.2, "gladiators", false, 0, 0, 0, 0, 0, 0, "Fearless left-hander capable of taking any bowling attack apart.", 34, 10),
        Player("g3", "Mohammad Rizwan", PlayerRole.WICKET_KEEPER, 91, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, BattingHand.RIGHT_HAND, BowlingType.MEDIUM_VARIATIONS, null, "Consistently reliable", 2, false, false, "Pakistan", "Peshawar", false, 5.0, "gladiators", false, 0, 0, 0, 0, 0, 0, "Workhorse of the team with incredible stamina and keeping skills.", 31, 11),
        Player("g4", "Shaheen Afridi", PlayerRole.BOWLER, 25, 93, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, BattingHand.LEFT_HAND, BowlingType.FAST_BOWLER_EXTRA_PACE, null, "Deadly new ball spell", 11, false, false, "Pakistan", "Peshawar", false, 5.2, "gladiators", false, 0, 0, 0, 0, 0, 0, "Premier left-arm fast bowler known for his lethal first-over wickets.", 24, 7),
        Player("g5", "Rashid Khan", PlayerRole.ALL_ROUNDER, 68, 94, PlayingStyle.AGGRESSIVE, DeliveryStyle.SPIN, BattingHand.RIGHT_HAND, BowlingType.LEG_SPIN, null, "Best T20 spinner", 8, true, true, "Afghanistan", "Surrey", true, 4.8, "gladiators", false, 0, 0, 0, 0, 0, 0, "Global T20 sensation with quick arm action and tricky googlies.", 25, 8),

        // === FREE AGENTS ===
        Player("f1", "Kane Williamson", PlayerRole.BATSMAN, 91, 10, PlayingStyle.DEFENSIVE, DeliveryStyle.SPIN, BattingHand.RIGHT_HAND, BowlingType.OFF_SPIN, null, "World class anchor", 3, false, false, "New Zealand", "Yorkshire", true, 3.2, null, false, 0, 0, 0, 0, 0, 0, "Master tactician and top-tier technical batter for all conditions.", 33, 14)
    )

    // Setup active state list
    private val _playersStateFlow = MutableStateFlow<List<Player>>(masterPlayersList)
    val playersStateFlow: StateFlow<List<Player>> = _playersStateFlow.asStateFlow()

    // Franchise Teams list
    private val _teamsList = MutableStateFlow(listOf(
        Team("kings", "Kings", "KNG", "👑", emptyList(), "#FBC02D", 50.0),
        Team("stars", "Stars", "STR", "🌟", emptyList(), "#10B981", 50.0),
        Team("sixers", "Sixers", "SXR", "💖", emptyList(), "#EC4899", 50.0),
        Team("gladiators", "Gladiators", "GLD", "⚔️", emptyList(), "#8B5CF6", 50.0),
        Team("eagles", "Eagles", "EGL", "🦅", emptyList(), "#3B82F6", 50.0),
        Team("hawks", "Hawks", "HWK", "🔥", emptyList(), "#F97316", 50.0)
    ))
    val teamsList: StateFlow<List<Team>> = _teamsList.asStateFlow()

    // Active User Career Franchise Selection
    private val _userTeamId = MutableStateFlow("gladiators")
    val userTeamId: StateFlow<String> = _userTeamId.asStateFlow()

    val userTeam: Team
        get() = _teamsList.value.firstOrNull { it.id == _userTeamId.value } ?: _teamsList.value[3]

    // Active Match Format, Season, and Round indicators
    private val _activeFormat = MutableStateFlow(MatchFormat.T20)
    val activeFormat: StateFlow<MatchFormat> = _activeFormat.asStateFlow()

    private val _currentRound = MutableStateFlow(1)
    val currentRound: StateFlow<Int> = _currentRound.asStateFlow()

    // Selection custom states
    private val _selectedTVChannel = MutableStateFlow("Sike Sports+ 26")
    val selectedTVChannel: StateFlow<String> = _selectedTVChannel.asStateFlow()

    private val _selectedSponsor = MutableStateFlow("Sike's Cricket 26 Official")
    val selectedSponsor: StateFlow<String> = _selectedSponsor.asStateFlow()

    // Current matches state
    private val _scheduleMatches = MutableStateFlow<List<ScheduledMatch>>(emptyList())
    val scheduleMatches: StateFlow<List<ScheduledMatch>> = _scheduleMatches.asStateFlow()

    // Live standing details
    private val _standings = MutableStateFlow<List<LeagueStanding>>(emptyList())
    val standings: StateFlow<List<LeagueStanding>> = _standings.asStateFlow()

    init {
        // Apply weakness and scout report rules for missing data
        masterPlayersList.forEachIndexed { index, p ->
            if (p.weakness == null) {
                val generatedWeakness = generateWeaknessForPlayer(p)
                masterPlayersList[index] = p.copy(weakness = generatedWeakness)
            }
        }

        // Distribute starting roster players to their specified teamIds
        var currentTeams = _teamsList.value
        masterPlayersList.forEach { p ->
            if (p.teamId != null) {
                currentTeams = currentTeams.map { t ->
                    if (t.id == p.teamId) {
                        t.copy(players = t.players + p)
                    } else t
                }
            }
        }
        _teamsList.value = currentTeams
        
        // Fix Lineups and Captains
        _teamsList.value.forEach { team ->
            autoFixLineupAndCaptain(team.id)
        }

        recalculateBudgets()
        generateLeagueSchedule()
        recalculateStandings()
    }

    private fun generateWeaknessForPlayer(player: Player): BowlingType {
        if (player.battingSkill > 70) {
            // Vulnerable bowling category logic
            val types = BowlingType.values()
            // Weighted randomness for variety
            return types.random()
        } else {
            return BowlingType.values().random()
        }
    }

    private fun autoFixLineupAndCaptain(teamId: String) {
        val team = _teamsList.value.firstOrNull { it.id == teamId } ?: return
        val squad = masterPlayersList.filter { it.teamId == teamId }
        
        if (squad.isEmpty()) return

        // Assign captain if missing
        val hasCaptain = squad.any { it.isCaptain }
        if (!hasCaptain) {
            // Pick highest skilled player as captain
            val bestPlayer = squad.maxByOrNull { it.battingSkill + it.bowlingSkill }
            if (bestPlayer != null) {
                val idx = masterPlayersList.indexOfFirst { it.id == bestPlayer.id }
                if (idx != -1) {
                    masterPlayersList[idx] = masterPlayersList[idx].copy(isCaptain = true)
                }
            }
        }

        // Auto Generate Playing XI if needed
        var newPlayers = team.players
        if (newPlayers.size < 11 && squad.size >= 11) {
            newPlayers = pickBalancedXI(squad)
        } else if (newPlayers.isEmpty() && squad.size >= 11) {
            newPlayers = pickBalancedXI(squad)
        }
        
        if (newPlayers != team.players) {
            _teamsList.value = _teamsList.value.map {
                if (it.id == teamId) it.copy(players = newPlayers) else it
            }
        }
    }

    private fun pickBalancedXI(squad: List<Player>): List<Player> {
        val xi = mutableListOf<Player>()
        
        // 1. One Wicket Keeper
        val wk = squad.firstOrNull { it.role == PlayerRole.WICKET_KEEPER } ?: squad.first()
        xi.add(wk)
        
        // 2. Minimum 4 batters (including WK maybe, but let's be explicit)
        val batters = squad.filter { it.role == PlayerRole.BATSMAN && !xi.contains(it) }.take(4)
        xi.addAll(batters)
        
        // 3. Minimum 1 all-rounder
        val ar = squad.filter { it.role == PlayerRole.ALL_ROUNDER && !xi.contains(it) }.take(1)
        xi.addAll(ar)
        
        // 4. Minimum 3 bowlers
        val bowlers = squad.filter { it.role == PlayerRole.BOWLER && !xi.contains(it) }.take(3)
        xi.addAll(bowlers)
        
        // 5. Fill remaining to 11
        val remaining = squad.filter { !xi.contains(it) }.take(11 - xi.size)
        xi.addAll(remaining)
        
        return xi.take(11)
    }

    private fun recalculateBudgets() {
        _teamsList.value = _teamsList.value.map { team ->
            val totalSpent = team.players.sumOf { it.marketPriceCr }
            team.copy(purseCr = (50.0 - totalSpent).coerceAtLeast(0.0))
        }
    }

    // Set user managing franchise team
    fun selectUserTeam(id: String) {
        _userTeamId.value = id
        resetMatchSetup()
    }

    fun changeFormat(format: MatchFormat) {
        _activeFormat.value = format
        _currentRound.value = 1
        generateLeagueSchedule()
        recalculateStandings()
        resetMatchSetup()
    }

    // Generate Round Robin Fixtures for 5 rounds
    fun generateLeagueSchedule() {
        val matches = mutableListOf<ScheduledMatch>()
        val format = _activeFormat.value

        // Round 1
        matches.add(ScheduledMatch(1, 1, "kings", "stars", format))
        matches.add(ScheduledMatch(2, 1, "sixers", "gladiators", format))
        matches.add(ScheduledMatch(3, 1, "eagles", "hawks", format))

        // Round 2
        matches.add(ScheduledMatch(4, 2, "kings", "sixers", format))
        matches.add(ScheduledMatch(5, 2, "stars", "eagles", format))
        matches.add(ScheduledMatch(6, 2, "gladiators", "hawks", format))

        // Round 3
        matches.add(ScheduledMatch(7, 3, "kings", "gladiators", format))
        matches.add(ScheduledMatch(8, 3, "sixers", "eagles", format))
        matches.add(ScheduledMatch(9, 3, "stars", "hawks", format))

        // Round 4
        matches.add(ScheduledMatch(10, 4, "kings", "eagles", format))
        matches.add(ScheduledMatch(11, 4, "stars", "gladiators", format))
        matches.add(ScheduledMatch(12, 4, "sixers", "hawks", format))

        // Round 5
        matches.add(ScheduledMatch(13, 5, "kings", "hawks", format))
        matches.add(ScheduledMatch(14, 5, "stars", "sixers", format))
        matches.add(ScheduledMatch(15, 5, "gladiators", "eagles", format))

        _scheduleMatches.value = matches
    }

    // Quick Simulation of non-user matches
    fun simulateRemainingMatchesForRound() {
        val currentRnd = _currentRound.value
        val format = _activeFormat.value
        val updatedMatches = _scheduleMatches.value.map { match ->
            if (match.round == currentRnd && !match.isPlayed) {
                // Determine if user team is involved. We play user matches manually (or user can choose to quick sim user match too!)
                val isUserInvolved = (match.homeTeamId == _userTeamId.value || match.awayTeamId == _userTeamId.value)
                if (isUserInvolved) {
                    // Quick Sim user match too if they command it
                    quickSimulateMatch(match)
                } else {
                    quickSimulateMatch(match)
                }
            } else {
                match
            }
        }
        _scheduleMatches.value = updatedMatches
        recalculateStandings()

        // Advance round if all matches in round are played
        val matchesInRound = _scheduleMatches.value.filter { it.round == currentRnd }
        val allPlayed = matchesInRound.all { it.isPlayed }
        if (allPlayed && currentRnd < 5) {
            _currentRound.value = currentRnd + 1
        }
    }

    fun quickSimulateMatch(match: ScheduledMatch): ScheduledMatch {
        val homeTeam = _teamsList.value.firstOrNull { it.id == match.homeTeamId } ?: return match
        val awayTeam = _teamsList.value.firstOrNull { it.id == match.awayTeamId } ?: return match

        // Aggregate batting vs bowling skills to randomize realistically
        val homeBatSkill = homeTeam.players.sumOf { it.battingSkill } / 11.0
        val homeBowlSkill = homeTeam.players.sumOf { it.bowlingSkill } / 11.0
        val awayBatSkill = awayTeam.players.sumOf { it.battingSkill } / 11.0
        val awayBowlSkill = awayTeam.players.sumOf { it.bowlingSkill } / 11.0

        val random = Random(System.currentTimeMillis() + match.id * 100)

        val formatMultiplier = when (match.format) {
            MatchFormat.T20 -> 1.0
            MatchFormat.ODI -> 2.4
            MatchFormat.FIRST_CLASS -> 4.5
        }

        // Home scores
        val baseHomeRuns = (homeBatSkill * 2.1 - awayBowlSkill * 0.5) * formatMultiplier
        val homeRuns = (baseHomeRuns + random.nextInt(-30, 45)).toInt().coerceAtLeast(30)
        val homeWkts = random.nextInt(2, 11).coerceIn(0, 10)

        // Away scores
        val baseAwayRuns = (awayBatSkill * 2.1 - homeBowlSkill * 0.5) * formatMultiplier
        val awayRuns = (baseAwayRuns + random.nextInt(-35, 40)).toInt().coerceAtLeast(30)
        val awayWkts = random.nextInt(2, 11).coerceIn(0, 10)

        match.isPlayed = true
        match.homeRuns = homeRuns
        match.homeWickets = homeWkts
        match.homeOvers = if (match.format == MatchFormat.T20) 20.0 else if (match.format == MatchFormat.ODI) 50.0 else 90.0
        match.awayRuns = awayRuns
        match.awayWickets = awayWkts
        match.awayOvers = match.homeOvers

        // Set Winner and stats
        if (homeRuns > awayRuns) {
            match.winnerId = homeTeam.id
            val rMargin = homeRuns - awayRuns
            match.resultText = "${homeTeam.name} won by $rMargin runs"
        } else if (awayRuns > homeRuns) {
            match.winnerId = awayTeam.id
            val wMargin = 10 - awayWkts
            match.resultText = "${awayTeam.name} won by $wMargin wickets"
        } else {
            match.winnerId = null
            match.resultText = "Match Tied"
        }

        // Give dynamic stats to top performers under the hood
        homeTeam.players.take(5).forEach { p ->
            p.seasonMatches++
            p.seasonRuns += random.nextInt(10, (homeRuns / 3).coerceAtLeast(11))
        }
        homeTeam.players.takeLast(5).forEach { p ->
            if (p.role == PlayerRole.BOWLER) {
                p.seasonWickets += if (random.nextDouble() < 0.4) 1 else 0
            }
        }
        awayTeam.players.take(5).forEach { p ->
            p.seasonMatches++
            p.seasonRuns += random.nextInt(10, (awayRuns / 3).coerceAtLeast(11))
        }
        awayTeam.players.takeLast(5).forEach { p ->
            if (p.role == PlayerRole.BOWLER) {
                p.seasonWickets += if (random.nextDouble() < 0.4) 1 else 0
            }
        }

        return match
    }

    // Refresh stand table
    fun recalculateStandings() {
        val stands = _teamsList.value.map { team ->
            LeagueStanding(team.id, team.name, team.flagEmoji)
        }.toMutableList()

        _scheduleMatches.value.forEach { match ->
            if (match.isPlayed) {
                val hRow = stands.firstOrNull { it.teamId == match.homeTeamId }
                val aRow = stands.firstOrNull { it.teamId == match.awayTeamId }

                if (hRow != null && aRow != null) {
                    hRow.played++
                    aRow.played++
                    if (match.winnerId == match.homeTeamId) {
                        hRow.won++
                        hRow.points += 2
                    } else if (match.winnerId == match.awayTeamId) {
                        aRow.won++
                        aRow.points += 2
                    } else {
                        // Tie
                        hRow.points += 1
                        aRow.points += 1
                    }
                    // simple NRR variance delta mapping
                    hRow.lost = hRow.played - hRow.won
                    aRow.lost = aRow.played - aRow.won

                    val hruns = match.homeRuns.toDouble()
                    val aruns = match.awayRuns.toDouble()
                    hRow.netRunRate += (hruns - aruns) / 100.0
                    aRow.netRunRate += (aruns - hruns) / 100.0
                }
            }
        }
        // sort by Points, then NRR
        stands.sortWith(compareByDescending<LeagueStanding> { it.points }.thenByDescending { it.netRunRate })
        _standings.value = stands
    }

    // Interactive custom team presets & color edit mapping
    fun updateTeamCustomization(name: String, abbreviation: String, emoji: String, colorHex: String) {
        val list = _teamsList.value.map { team ->
            if (team.id == _userTeamId.value) {
                team.copy(name = name, abbreviation = abbreviation, flagEmoji = emoji, colorHex = colorHex)
            } else {
                team
            }
        }
        _teamsList.value = list
        recalculateStandings()
        autoFixLineupAndCaptain(_userTeamId.value) 
    }

    // TV Overlays presets
    fun setTVChannel(tvId: String) {
        _selectedTVChannel.value = tvId
    }

    fun setSponsorship(sponsor: String) {
        _selectedSponsor.value = sponsor
    }

    // Squad validations checking for minimum 12 local and max 8 foreign, etc.
    fun validateSquadRoster(squad: List<Player>): String? {
        val foreignCount = squad.count { it.isForeign }
        val localCount = squad.count { !it.isForeign }
        val total = squad.size

        if (total > 21) {
            return "Squad exceeds maximum cap limit of 21 players!"
        }
        if (total < 16) {
            return "Squad must consist of at least 16 players (Current: $total)!"
        }
        if (localCount < 12) {
            return "Roster must include at least 12 local Pakistani players (Current: $localCount)!"
        }
        if (foreignCount > 8) {
            return "Squad is limited to a maximum of 8 foreign players (Current: $foreignCount)!"
        }
        return null
    }

    // Playing XI limits check: max 4, min 2 foreign players
    fun validatePlayingXILineup(playingXI: List<Player>): String? {
        if (playingXI.size != 11) {
            return "Select exactly 11 active players for the lineup!"
        }
        val foreignCount = playingXI.count { it.isForeign }
        if (foreignCount > 4) {
            return "Too many foreign players! Maximum allowed is 4 in standard PSL playing XI squad."
        }
        if (foreignCount < 2) {
            return "Squad depth warning: A minimum of 2 foreign players must play in active lineup."
        }
        return null
    }

    fun movePlayer(index: Int, direction: String) {
        val currentTeam = userTeam
        val list = currentTeam.players.toMutableList()
        if (direction == "up" && index > 0) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
        } else if (direction == "down" && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
        }
        
        _teamsList.value = _teamsList.value.map {
            if (it.id == currentTeam.id) it.copy(players = list) else it
        }
    }

    fun swapPlayers(index1: Int, index2: Int) {
        val currentTeam = userTeam
        val list = currentTeam.players.toMutableList()
        if (index1 in list.indices && index2 in list.indices) {
            val temp = list[index1]
            list[index1] = list[index2]
            list[index2] = temp
        }
        _teamsList.value = _teamsList.value.map {
            if (it.id == currentTeam.id) it.copy(players = list) else it
        }
    }

    fun removePlayerFromLineup(player: Player) {
        val currentTeam = userTeam
        val newList = currentTeam.players.filter { it.id != player.id }
        _teamsList.value = _teamsList.value.map {
            if (it.id == currentTeam.id) it.copy(players = newList) else it
        }
    }

    fun addPlayerToLineup(player: Player) {
        val currentTeam = userTeam
        if (currentTeam.players.size < 21) {
            val newList = (currentTeam.players + player).distinctBy { it.id }
            _teamsList.value = _teamsList.value.map {
                if (it.id == currentTeam.id) it.copy(players = newList) else it
            }
        }
    }

    fun setPlayerRole(player: Player, newRole: com.example.data.models.PlayerRole) {
        val list = userTeam.players
        val updatedList = list.map { p ->
            if (p.id == player.id) {
                p.copy(role = newRole)
            } else {
                p
            }
        }
        list.clear()
        list.addAll(updatedList)
        _teamsList.value = ArrayList(_teamsList.value)
    }

    // Roster actions: Buying or releasing players
    fun purchaseMarketPlayer(player: Player): Boolean {
        val currentTeam = userTeam
        val currentRoster = currentTeam.players
        val foreignCount = currentRoster.count { it.isForeign } + if (player.isForeign) 1 else 0

        if (currentRoster.size >= 21) return false
        if (currentTeam.purseCr < player.marketPriceCr) return false

        // Check rules
        if (player.isForeign && foreignCount > 8) return false

        // Update player master record
        val pIdx = masterPlayersList.indexOfFirst { it.id == player.id }
        if (pIdx != -1) {
            masterPlayersList[pIdx] = masterPlayersList[pIdx].copy(teamId = currentTeam.id)
        }

        // Update teams list reactively
        _teamsList.value = _teamsList.value.map {
            if (it.id == currentTeam.id) {
                it.copy(players = it.players + player)
            } else it
        }

        recalculateBudgets()
        _playersStateFlow.value = ArrayList(masterPlayersList)
        return true
    }

    fun releaseSquadPlayer(player: Player): Boolean {
        val currentTeam = userTeam
        if (currentTeam.players.size <= 16) return false // can't violate squad min size limit of 16 players
        
        val pIdx = masterPlayersList.indexOfFirst { it.id == player.id }
        if (pIdx != -1) {
            masterPlayersList[pIdx] = masterPlayersList[pIdx].copy(teamId = null)
        }

        _teamsList.value = _teamsList.value.map {
            if (it.id == currentTeam.id) {
                val newList = it.players.filter { it.id != player.id }
                it.copy(players = newList)
            } else it
        }

        recalculateBudgets()
        _playersStateFlow.value = ArrayList(masterPlayersList)
        return true
    }

    // Player sorting, databases and search queries
    fun getPlayersFiltered(nationality: String, teamId: String, county: String, search: String): List<Player> {
        return masterPlayersList.filter { p ->
            val matchesNationality = when (nationality) {
                "Pakistan" -> !p.isForeign
                "Foreign" -> p.isForeign
                else -> true
            }
            val matchesTeam = when (teamId) {
                "All" -> true
                "Free Agents" -> p.teamId == null
                else -> p.teamId == teamId
            }
            val matchesCounty = when (county) {
                "All" -> true
                else -> p.county.equals(county, ignoreCase = true)
            }
            val matchesSearch = p.name.contains(search, ignoreCase = true)

            matchesNationality && matchesTeam && matchesCounty && matchesSearch
        }
    }

    // Live pre-match settings before commencing simulation
    private val _homeTeamName = MutableStateFlow("")
    private val _awayTeamName = MutableStateFlow("")

    private val _homeTeam = MutableStateFlow(Team("gladiators", "Gladiators", "GLD", "⚔️", mutableListOf(), "#8B5CF6", 50.0))
    val homeTeam: StateFlow<Team> = _homeTeam.asStateFlow()

    private val _awayTeam = MutableStateFlow(Team("kings", "Kings", "KNG", "👑", mutableListOf(), "#FBC02D", 50.0))
    val awayTeam: StateFlow<Team> = _awayTeam.asStateFlow()

    private val _oversLimit = MutableStateFlow(5) // play shorter simulation default
    val oversLimit: StateFlow<Int> = _oversLimit.asStateFlow()

    private val _pitchType = MutableStateFlow(PitchType.BALANCED)
    val pitchType: StateFlow<PitchType> = _pitchType.asStateFlow()

    private val _tossWinner = MutableStateFlow<String?>(null)
    val tossWinner = _tossWinner.asStateFlow()

    private val _tossChoice = MutableStateFlow<String?>(null) // "Batting" or "Bowling"
    val tossChoice = _tossChoice.asStateFlow()

    private val _userTossChoice = MutableStateFlow("Heads")
    val userTossChoice = _userTossChoice.asStateFlow()

    private val _matchStage = MutableStateFlow(MatchStage.TOSS)
    val matchStage: StateFlow<MatchStage> = _matchStage.asStateFlow()

    private val _currentInnings = MutableStateFlow(1)
    val currentInnings = _currentInnings.asStateFlow()

    private val _isUserBatting = MutableStateFlow(true)
    val isUserBatting = _isUserBatting.asStateFlow()

    private val _firstInningsScore = MutableStateFlow<Int?>(null)
    val firstInningsScore = _firstInningsScore.asStateFlow()

    private val _firstInningsWickets = MutableStateFlow<Int?>(null)
    val firstInningsWickets = _firstInningsWickets.asStateFlow()

    private val _battingTeamNow = MutableStateFlow(Team("gladiators", "Gladiators", "GLD", "⚔️", mutableListOf(), "#8B5CF6", 50.0))
    val battingTeamNow = _battingTeamNow.asStateFlow()

    private val _bowlingTeamNow = MutableStateFlow(Team("kings", "Kings", "KNG", "👑", mutableListOf(), "#FBC02D", 50.0))
    val bowlingTeamNow = _bowlingTeamNow.asStateFlow()

    private val _runs = MutableStateFlow(0)
    val runs = _runs.asStateFlow()

    private val _wickets = MutableStateFlow(0)
    val wickets = _wickets.asStateFlow()

    private val _balls = MutableStateFlow(0)
    val balls = _balls.asStateFlow()

    private val _target = MutableStateFlow<Int?>(null)
    val target = _target.asStateFlow()

    private val _batsmanStatsMap = MutableStateFlow<Map<String, PlayerMatchStats>>(emptyMap())
    val batsmanStatsMap = _batsmanStatsMap.asStateFlow()

    private val _bowlerStatsMap = MutableStateFlow<Map<String, PlayerMatchStats>>(emptyMap())
    val bowlerStatsMap = _bowlerStatsMap.asStateFlow()

    private val _strikerName = MutableStateFlow("")
    val strikerName = _strikerName.asStateFlow()

    private val _nonStrikerName = MutableStateFlow("")
    val nonStrikerName = _nonStrikerName.asStateFlow()

    private val _currentBowlerName = MutableStateFlow("")
    val currentBowlerName = _currentBowlerName.asStateFlow()

    private var nextBatsmanIndex = 2
    private var currentBowlerListIndex = 0

    private val _commentaryList = MutableStateFlow<List<String>>(emptyList())
    val commentaryList = _commentaryList.asStateFlow()

    // Independent Aggression & Tactics
    private val _strikerStyle = MutableStateFlow(PlayingStyle.BALANCED)
    val strikerStyle = _strikerStyle.asStateFlow()

    private val _nonStrikerStyle = MutableStateFlow(PlayingStyle.BALANCED)
    val nonStrikerStyle = _nonStrikerStyle.asStateFlow()

    // Map of (BatterId to BowlerId) -> PlaySafe Boolean
    private val _playSafeMatchups = MutableStateFlow<Map<Pair<String, String>, Boolean>>(emptyMap())
    val playSafeMatchups = _playSafeMatchups.asStateFlow()

    fun setStrikerStyle(style: PlayingStyle) { _strikerStyle.value = style }
    fun setNonStrikerStyle(style: PlayingStyle) { _nonStrikerStyle.value = style }
    fun togglePlaySafe(batterId: String, bowlerId: String) {
        val current = _playSafeMatchups.value.toMutableMap()
        val key = Pair(batterId, bowlerId)
        current[key] = !(current[key] ?: false)
        _playSafeMatchups.value = current
    }

    private val _recentBallLog = MutableStateFlow<List<String>>(emptyList())
    val recentBallLog = _recentBallLog.asStateFlow()

    private val _isAutoSimulating = MutableStateFlow(false)
    val isAutoSimulating = _isAutoSimulating.asStateFlow()

    private var autoSimJob: Job? = null

    private val _matchMargin = MutableStateFlow("")
    val matchMargin = _matchMargin.asStateFlow()

    private val _matchWinnerName = MutableStateFlow("")
    val matchWinnerName = _matchWinnerName.asStateFlow()

    private val _manOfTheMatch = MutableStateFlow("")
    val manOfTheMatch = _manOfTheMatch.asStateFlow()

    // Configure home/away prior to launching match simulate screen
    fun loadMatchup(opponentTeamId: String, limitOvers: Int) {
        autoFixLineupAndCaptain(_userTeamId.value)
        autoFixLineupAndCaptain(opponentTeamId)

        val userFranchise = userTeam
        val oppositionFranchise = _teamsList.value.firstOrNull { it.id == opponentTeamId } ?: _teamsList.value[0]

        _homeTeam.value = userFranchise
        _awayTeam.value = oppositionFranchise
        _oversLimit.value = limitOvers
        resetMatchSetup()
    }

    fun setOversLimit(limit: Int) {
        _oversLimit.value = limit
    }

    fun setPitchType(pitch: PitchType) {
        _pitchType.value = pitch
    }

    fun setUserTossChoice(choice: String) {
        _userTossChoice.value = choice
    }

    fun resetMatchSetup() {
        stopAutoSimulate()
        _matchStage.value = MatchStage.TOSS
        _tossWinner.value = null
        _tossChoice.value = null
        _currentInnings.value = 1
        _firstInningsScore.value = null
        _firstInningsWickets.value = null
        _runs.value = 0
        _wickets.value = 0
        _balls.value = 0
        _target.value = null
        _strikerName.value = ""
        _nonStrikerName.value = ""
        _currentBowlerName.value = ""
        _commentaryList.value = emptyList()
        _recentBallLog.value = emptyList()
        _matchMargin.value = ""
        _matchWinnerName.value = ""
        _manOfTheMatch.value = ""
        _isAutoSimulating.value = false
        _batsmanStatsMap.value = emptyMap()
        _bowlerStatsMap.value = emptyMap()
    }

    fun performToss() {
        val coinIsHeads = Math.random() < 0.5
        val userGuessedCorrectly = (coinIsHeads && _userTossChoice.value == "Heads") ||
                (!coinIsHeads && _userTossChoice.value == "Tails")

        val tossWinnerTeam = if (userGuessedCorrectly) _homeTeam.value else _awayTeam.value
        _tossWinner.value = tossWinnerTeam.name

        // Select randomly or let user bats/bowls
        val decision = if (userGuessedCorrectly) {
            if (Math.random() < 0.7) "Batting" else "Bowling"
        } else {
            if (Math.random() < 0.5) "Batting" else "Bowling"
        }
        _tossChoice.value = decision

        setupInningsTeams(1)
    }

    private fun setupInningsTeams(innings: Int) {
        val isHomeBattingFirst = (_tossWinner.value == _homeTeam.value.name && _tossChoice.value == "Batting") ||
                (_tossWinner.value == _awayTeam.value.name && _tossChoice.value == "Bowling")

        // Construct squad list
        if (innings == 1) {
            if (isHomeBattingFirst) {
                _battingTeamNow.value = _homeTeam.value
                _bowlingTeamNow.value = _awayTeam.value
                _isUserBatting.value = true
            } else {
                _battingTeamNow.value = _awayTeam.value
                _bowlingTeamNow.value = _homeTeam.value
                _isUserBatting.value = false
            }
            _runs.value = 0
            _wickets.value = 0
            _balls.value = 0
            _target.value = null
            _currentInnings.value = 1
        } else {
            val previousBatter = _battingTeamNow.value
            _battingTeamNow.value = _bowlingTeamNow.value
            _bowlingTeamNow.value = previousBatter
            _isUserBatting.value = !_isUserBatting.value
            _target.value = _runs.value + 1
            _runs.value = 0
            _wickets.value = 0
            _balls.value = 0
            _currentInnings.value = 2
        }

        // Generate scoring indices
        val baseBatsmen = _battingTeamNow.value.players
        val bStats = baseBatsmen.associate {
            it.name to PlayerMatchStats(name = it.name)
        }
        _batsmanStatsMap.value = bStats

        val baseBowlers = _bowlingTeamNow.value.players
        val boStats = baseBowlers.associate {
            it.name to PlayerMatchStats(name = it.name)
        }
        _bowlerStatsMap.value = boStats

        if (baseBatsmen.isNotEmpty()) {
            _strikerName.value = baseBatsmen[0].name
            _nonStrikerName.value = if (baseBatsmen.size > 1) baseBatsmen[1].name else baseBatsmen[0].name
        }
        nextBatsmanIndex = 2

        val bowlersList = _bowlingTeamNow.value.players.filter {
            it.role == PlayerRole.BOWLER || it.role == PlayerRole.ALL_ROUNDER
        }
        currentBowlerListIndex = 0
        if (bowlersList.isNotEmpty()) {
            _currentBowlerName.value = bowlersList[0].name
        } else if (_bowlingTeamNow.value.players.isNotEmpty()) {
            _currentBowlerName.value = _bowlingTeamNow.value.players.last().name
        }

        _recentBallLog.value = emptyList()

        addCommentary("INNINGS ${if (innings == 1) "1" else "2"}: ${_battingTeamNow.value.flagEmoji} ${_battingTeamNow.value.name} are coming in to bat! Primary Color accent overlay loaded.")
        addCommentary("Pitch conditions: ${_pitchType.value.displayName} - ${_pitchType.value.description}")
        addCommentary("Striker batsman: ${_strikerName.value}, Non-Striker: ${_nonStrikerName.value}. Bowler: ${_currentBowlerName.value} with the leather.")
    }

    fun startGameplay() {
        _matchStage.value = MatchStage.PLAYING
    }

    private fun addCommentary(line: String) {
        _commentaryList.value = listOf(line) + _commentaryList.value
    }

    fun playSingleBall(): Boolean {
        if (_matchStage.value != MatchStage.PLAYING) return false

        val bSquad = _battingTeamNow.value.players
        val boSquad = _bowlingTeamNow.value.players

        if (bSquad.isEmpty() || boSquad.isEmpty()) return false

        val striker = bSquad.firstOrNull { it.name == _strikerName.value } ?: bSquad[0]
        val bowler = boSquad.firstOrNull { it.name == _currentBowlerName.value } ?: boSquad.last()

        val isPlaySafe = _playSafeMatchups.value[Pair(striker.id, bowler.id)] ?: false
        
        // Override striker temporary playing style for simulation
        val effectiveStriker = striker.copy(playStyle = _strikerStyle.value)

        val outcome = SimulationEngine.simulateBall(
            batsman = effectiveStriker,
            bowler = bowler,
            pitch = _pitchType.value,
            targetRuns = _target.value,
            currentRuns = _runs.value,
            wicketsDown = _wickets.value,
            ballsBowled = _balls.value,
            oversLimit = _oversLimit.value,
            isPlaySafe = isPlaySafe
        )

        processBallOutcome(outcome, striker, bowler)

        val ends = checkInningsComplete()
        if (ends) {
            stopAutoSimulate()
            return true
        }
        return false
    }

    private fun processBallOutcome(outcome: BallOutcome, striker: Player, bowler: Player) {
        val batStats = _batsmanStatsMap.value.toMutableMap()
        val bowlStats = _bowlerStatsMap.value.toMutableMap()

        val strikerStats = batStats[striker.name] ?: PlayerMatchStats(striker.name)
        val bowlerStats = bowlStats[bowler.name] ?: PlayerMatchStats(bowler.name)

        if (!outcome.isExtra) {
            strikerStats.ballsFaced++
            strikerStats.runsScored += outcome.runs
            if (outcome.runs == 4) strikerStats.fours++
            if (outcome.runs == 6) strikerStats.sixes++

            bowlerStats.ballsBowled++
            bowlerStats.runsConceded += outcome.runs

            _runs.value += outcome.runs
            _balls.value++
            updateBallLog(outcome.runs.toString())
        } else {
            _runs.value += outcome.totalRunsAwarded
            bowlerStats.runsConceded += outcome.totalRunsAwarded

            if (outcome.extraType == "nb") {
                strikerStats.ballsFaced++
                strikerStats.runsScored += outcome.runs
                if (outcome.runs == 4) strikerStats.fours++
                if (outcome.runs == 6) strikerStats.sixes++
                bowlerStats.ballsBowled++
            }
            updateBallLog(outcome.extraType + (if (outcome.runs > 0) "+${outcome.runs}" else ""))
        }

        if (outcome.isWicket) {
            _wickets.value++
            strikerStats.isOut = true
            strikerStats.dismissalInfo = outcome.wicketType
            bowlerStats.wicketsTaken++

            updateBallLog("W")
            addCommentary("Ball ${_balls.value / 6}.${_balls.value % 6}: ${outcome.commentary}")
            addCommentary("WICKET! ${striker.name} departs scoring ${strikerStats.runsScored} (${strikerStats.ballsFaced}).")

            if (_wickets.value < 10) {
                val nextBatter = _battingTeamNow.value.players.getOrNull(nextBatsmanIndex)
                if (nextBatter != null) {
                    _strikerName.value = nextBatter.name
                    nextBatsmanIndex++
                    addCommentary("New batsman at striker's crease: ${_strikerName.value}")
                }
            } else {
                _strikerName.value = ""
            }
        } else {
            addCommentary("Ball ${_balls.value / 6}.${_balls.value % 6}: ${outcome.commentary}")
            if (outcome.batsmanStrikeRotates) {
                rotateStrike()
            }
        }

        batStats[striker.name] = strikerStats
        bowlStats[bowler.name] = bowlerStats
        _batsmanStatsMap.value = batStats
        _bowlerStatsMap.value = bowlStats

        val curBowlStats = _bowlerStatsMap.value[_currentBowlerName.value]
        if (curBowlStats != null) {
            curBowlStats.oversBowled = (curBowlStats.ballsBowled / 6) + ((curBowlStats.ballsBowled % 6) / 10.0)
        }

        if (_balls.value > 0 && _balls.value % 6 == 0 && !outcome.isExtra) {
            rotateStrike()
            changeBowler()
        }
    }

    private fun rotateStrike() {
        val tmp = _strikerName.value
        _strikerName.value = _nonStrikerName.value
        _nonStrikerName.value = tmp

        // Swap aggression state to follow player
        val tmpStyle = _strikerStyle.value
        _strikerStyle.value = _nonStrikerStyle.value
        _nonStrikerStyle.value = tmpStyle
    }

    private fun changeBowler() {
        val bowlersList = _bowlingTeamNow.value.players.filter {
            it.role == PlayerRole.BOWLER || it.role == PlayerRole.ALL_ROUNDER
        }
        if (bowlersList.isNotEmpty()) {
            currentBowlerListIndex = (currentBowlerListIndex + 1) % bowlersList.size
            val oldBowl = _currentBowlerName.value
            _currentBowlerName.value = bowlersList[currentBowlerListIndex].name
            addCommentary("End of over. ${_currentBowlerName.value} is handed the ball to replace $oldBowl.")
            _recentBallLog.value = emptyList()
        }
    }

    private fun updateBallLog(event: String) {
        val curr = _recentBallLog.value.toMutableList()
        curr.add(event)
        _recentBallLog.value = curr
    }

    private fun checkInningsComplete(): Boolean {
        val maxBalls = _oversLimit.value * 6
        if (_currentInnings.value == 1) {
            if (_wickets.value >= 10 || _balls.value >= maxBalls) {
                _firstInningsScore.value = _runs.value
                _firstInningsWickets.value = _wickets.value
                _matchStage.value = MatchStage.INNINGS_BREAK
                addCommentary("=== INNINGS 1 CONCLUDED ===")
                addCommentary("${_battingTeamNow.value.name} scored ${_runs.value}/${_wickets.value} on this ${_pitchType.value.displayName} pitch.")
                addCommentary("${_bowlingTeamNow.value.name} needs ${_runs.value + 1} runs in ${_oversLimit.value} overs.")
                return true
            }
        } else {
            val winsReq = _runs.value >= (_target.value ?: 0)
            val loses = _wickets.value >= 10 || _balls.value >= maxBalls
            if (winsReq || loses) {
                finishMatch()
                return true
            }
        }
        return false
    }

    fun startSecondInnings() {
        if (_matchStage.value == MatchStage.INNINGS_BREAK) {
            setupInningsTeams(2)
        }
    }

    private fun finishMatch() {
        _matchStage.value = MatchStage.FINISHED
        stopAutoSimulate()

        val finalRuns = _runs.value
        val reqTarget = _target.value ?: 0
        val isChased = finalRuns >= reqTarget

        val batName = _battingTeamNow.value.name
        val bowlName = _bowlingTeamNow.value.name

        if (isChased) {
            _matchWinnerName.value = batName
            val remainingWkts = 10 - _wickets.value
            _matchMargin.value = "$batName won by $remainingWkts wickets"
        } else {
            _matchWinnerName.value = bowlName
            if (finalRuns == reqTarget - 1) {
                _matchWinnerName.value = "Tie"
                _matchMargin.value = "Magnificent Tied match!"
            } else {
                val runMarg = (reqTarget - 1) - finalRuns
                _matchMargin.value = "$bowlName won by $runMarg runs"
            }
        }

        // POTM select
        val winningTeamName = _matchWinnerName.value
        val filterTeamObj = _teamsList.value.firstOrNull { it.name == winningTeamName } ?: _homeTeam.value
        var mvp = filterTeamObj.players.getOrNull(0)?.name ?: "Babar Azam"
        var highestPotRating = 0.0

        _batsmanStatsMap.value.forEach { (_, stats) ->
            val pts = stats.runsScored * 1.5 + (if (stats.runsScored >= 50) 25.0 else 0.0)
            if (pts > highestPotRating) {
                highestPotRating = pts
                mvp = stats.name
            }
        }
        _bowlerStatsMap.value.forEach { (_, stats) ->
            val pts = stats.wicketsTaken * 30.0 + stats.maidenOvers * 10 - stats.runsConceded * 0.4
            if (pts > highestPotRating) {
                highestPotRating = pts
                mvp = stats.name
            }
        }
        _manOfTheMatch.value = mvp

        addCommentary("=== GAME ENDED ===")
        addCommentary("Winner: ${_matchWinnerName.value} | Margin: ${_matchMargin.value}")
        addCommentary("🏆 Player of the Match: ${_manOfTheMatch.value}")

        // Save simulated result to schedules list if user team is active match
        updateSeasonScheduleResult()
        saveMatchToHistory()
    }

    private fun updateSeasonScheduleResult() {
        val roundMatch = _scheduleMatches.value.firstOrNull { match ->
            match.round == _currentRound.value && !match.isPlayed &&
                    (match.homeTeamId == _userTeamId.value || match.awayTeamId == _userTeamId.value)
        }
        if (roundMatch != null) {
            roundMatch.isPlayed = true
            roundMatch.homeRuns = if (roundMatch.homeTeamId == _battingTeamNow.value.id) _runs.value else (_firstInningsScore.value ?: 0)
            roundMatch.homeWickets = if (roundMatch.homeTeamId == _battingTeamNow.value.id) _wickets.value else (_firstInningsWickets.value ?: 0)
            roundMatch.homeOvers = _oversLimit.value.toDouble()

            roundMatch.awayRuns = if (roundMatch.awayTeamId == _battingTeamNow.value.id) _runs.value else (_firstInningsScore.value ?: 0)
            roundMatch.awayWickets = if (roundMatch.awayTeamId == _battingTeamNow.value.id) _wickets.value else (_firstInningsWickets.value ?: 0)
            roundMatch.awayOvers = _oversLimit.value.toDouble()

            roundMatch.winnerId = if (_matchWinnerName.value == _homeTeam.value.name) _homeTeam.value.id else if (_matchWinnerName.value == _awayTeam.value.name) _awayTeam.value.id else null
            roundMatch.resultText = _matchMargin.value

            recalculateStandings()
        }
    }

    private fun saveMatchToHistory() {
        viewModelScope.launch {
            val sumSummary = buildString {
                append("Scorecard - ${_homeTeam.value.name} vs ${_awayTeam.value.name}\n")
                append("Winner: ${_matchWinnerName.value} (${_matchMargin.value})\n")
                append("PotM: ${_manOfTheMatch.value}\n")
            }

            val homeScoreRuns = if (_homeTeam.value.name == _battingTeamNow.value.name) _runs.value else (_firstInningsScore.value ?: 0)
            val homeScoreWkts = if (_homeTeam.value.name == _battingTeamNow.value.name) _wickets.value else (_firstInningsWickets.value ?: 0)
            val awayScoreRuns = if (_awayTeam.value.name == _battingTeamNow.value.name) _runs.value else (_firstInningsScore.value ?: 0)
            val awayScoreWkts = if (_awayTeam.value.name == _battingTeamNow.value.name) _wickets.value else (_firstInningsWickets.value ?: 0)

            val record = MatchHistory(
                homeTeamName = _homeTeam.value.name,
                awayTeamName = _awayTeam.value.name,
                winnerTeamName = _matchWinnerName.value,
                homeRuns = homeScoreRuns,
                homeWickets = homeScoreWkts,
                homeBallsBowled = _balls.value,
                awayRuns = awayScoreRuns,
                awayWickets = awayScoreWkts,
                awayBallsBowled = _balls.value,
                oversLimit = _oversLimit.value,
                margin = _matchMargin.value,
                manOfTheMatch = _manOfTheMatch.value,
                matchSummary = sumSummary
            )
            repository.insertMatch(record)
        }
    }

    fun toggleAutoSimulate() {
        if (_isAutoSimulating.value) {
            stopAutoSimulate()
        } else {
            startAutoSimulate()
        }
    }

    private fun startAutoSimulate() {
        _isAutoSimulating.value = true
        autoSimJob = viewModelScope.launch {
            while (_isAutoSimulating.value && _matchStage.value == MatchStage.PLAYING) {
                val ends = playSingleBall()
                if (ends) break
                delay(1200)
            }
        }
    }

    fun playWholeOverDirectly() {
        if (_matchStage.value != MatchStage.PLAYING) return
        stopAutoSimulate()
        viewModelScope.launch {
            for (i in 0 until 6) {
                val done = playSingleBall()
                if (done) break
                delay(300)
            }
        }
    }

    fun stopAutoSimulate() {
        _isAutoSimulating.value = false
        autoSimJob?.cancel()
        autoSimJob = null
    }

    fun clearLocalMatchesHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoSimulate()
    }
}
