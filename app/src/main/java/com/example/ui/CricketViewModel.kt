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
        Player("k1", "A. Haddin", PlayerRole.ALL_ROUNDER, 76, 72, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Australia", "Surrey", true, 2.5, "kings"),
        Player("k2", "S. Warner", PlayerRole.BATSMAN, 92, 10, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Australia", "Surrey", true, 4.5, "kings"),
        Player("k3", "N. Colin", PlayerRole.ALL_ROUNDER, 74, 70, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "New Zealand", "Yorkshire", true, 2.0, "kings"),
        Player("k4", "M. Imran", PlayerRole.WICKET_KEEPER, 65, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Lahore", false, 0.5, "kings"),
        Player("k5", "I. Javed", PlayerRole.WICKET_KEEPER, 68, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Karachi", false, 0.6, "kings"),
        Player("k6", "Nasir Ahmad", PlayerRole.BATSMAN, 72, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Multan", false, 0.8, "kings"),
        Player("k7", "Husnain Shah", PlayerRole.BATSMAN, 74, 5, PlayingStyle.DEFANCED, DeliveryStyle.MEDIUM, "Pakistan", "Rawalpindi", false, 1.0, "kings"),
        Player("k8", "Jahid Ali", PlayerRole.BATSMAN, 71, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Mardan", false, 0.7, "kings"),
        Player("k9", "M. Musa", PlayerRole.BATSMAN, 69, 15, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Peshawar", false, 0.5, "kings"),
        Player("k10", "Amir Khan", PlayerRole.ALL_ROUNDER, 65, 88, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Lahore", false, 3.2, "kings"),
        Player("k11", "Mansoor Ali", PlayerRole.ALL_ROUNDER, 62, 65, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Quetta", false, 0.6, "kings"),
        Player("k12", "Zia ul Haq", PlayerRole.BOWLER, 15, 82, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Faisalabad", false, 1.2, "kings"),
        Player("k13", "Rizwan Jr", PlayerRole.BOWLER, 12, 84, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Sialkot", false, 1.4, "kings"),
        Player("k14", "Naseem Shah", PlayerRole.BOWLER, 20, 92, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Rawalpindi", false, 4.8, "kings"),
        Player("k15", "Farhan Khan", PlayerRole.BOWLER, 14, 81, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Peshawar", false, 0.8, "kings"),
        Player("k16", "Rahat Ali", PlayerRole.BOWLER, 20, 85, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Multan", false, 1.1, "kings"),

        // === STARS PLAYERS ===
        Player("s1", "Sprike Buttler", PlayerRole.WICKET_KEEPER, 88, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "England", "Surrey", true, 4.0, "stars"),
        Player("s2", "C. Dhanushka", PlayerRole.ALL_ROUNDER, 70, 75, PlayingStyle.DEFENSIVE, DeliveryStyle.SPIN, "Sri Lanka", "Surrey", true, 1.8, "stars"),
        Player("s3", "Jordan Archer", PlayerRole.BOWLER, 25, 86, PlayingStyle.BALANCED, DeliveryStyle.FAST, "England", "Yorkshire", true, 2.8, "stars"),
        Player("s4", "S. Khan", PlayerRole.WICKET_KEEPER, 74, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Faisalabad", false, 1.2, "stars"),
        Player("s5", "Haseebullah", PlayerRole.WICKET_KEEPER, 72, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Quetta", false, 0.9, "stars"),
        Player("s6", "Haider Ali", PlayerRole.BATSMAN, 81, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Rawalpindi", false, 2.2, "stars"),
        Player("s7", "Aslam Khan", PlayerRole.BATSMAN, 78, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Sialkot", false, 1.4, "stars"),
        Player("s8", "Shoaib Khan", PlayerRole.BATSMAN, 76, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Multan", false, 1.1, "stars"),
        Player("s9", "Aamer Jamal", PlayerRole.ALL_ROUNDER, 75, 84, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Mardan", false, 3.0, "stars"),
        Player("s10", "Taimoor Ali", PlayerRole.ALL_ROUNDER, 65, 68, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Lahore", false, 0.5, "stars"),
        Player("s11", "Aftab Khan", PlayerRole.ALL_ROUNDER, 60, 72, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Karachi", false, 0.6, "stars"),
        Player("s12", "Azam Khan", PlayerRole.WICKET_KEEPER, 80, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Karachi", false, 2.5, "stars"),
        Player("s13", "Sohail Tanvir", PlayerRole.BOWLER, 22, 82, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Rawalpindi", false, 1.3, "stars"),
        Player("s14", "Arshad Iqbal", PlayerRole.BOWLER, 18, 89, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Peshawar", false, 1.8, "stars"),
        Player("s15", "Anwar Ali", PlayerRole.BOWLER, 20, 84, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Karachi", false, 1.0, "stars"),

        // === SIXERS PLAYERS ===
        Player("sx1", "Lance Klusener", PlayerRole.BATSMAN, 86, 12, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "South Africa", "Surrey", true, 3.8, "sixers"),
        Player("sx2", "A. Chadwick", PlayerRole.WICKET_KEEPER, 82, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "West Indies", "Surrey", true, 2.4, "sixers"),
        Player("sx3", "James Vince", PlayerRole.ALL_ROUNDER, 75, 78, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "England", "Yorkshire", true, 2.2, "sixers"),
        Player("sx4", "Ali Khan", PlayerRole.WICKET_KEEPER, 70, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Multan", false, 0.6, "sixers"),
        Player("sx5", "R. Saad", PlayerRole.WICKET_KEEPER, 65, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Lahore", false, 0.4, "sixers"),
        Player("sx6", "Abid Ali", PlayerRole.BATSMAN, 85, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Lahore", false, 2.0, "sixers"),
        Player("sx7", "Hamid Hasan", PlayerRole.BATSMAN, 74, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Peshawar", false, 0.9, "sixers"),
        Player("sx8", "Zakir Khan", PlayerRole.BATSMAN, 72, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Mardan", false, 0.7, "sixers"),
        Player("sx9", "Faisal Hasan", PlayerRole.BATSMAN, 80, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Faisalabad", false, 1.5, "sixers"),
        Player("sx10", "Khalid Usman", PlayerRole.ALL_ROUNDER, 66, 68, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Peshawar", false, 0.8, "sixers"),
        Player("sx11", "Najaf Latif", PlayerRole.ALL_ROUNDER, 62, 65, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Sialkot", false, 0.5, "sixers"),
        Player("sx12", "M. Ilyas", PlayerRole.BOWLER, 15, 86, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Peshawar", false, 1.7, "sixers"),
        Player("sx13", "Sajid Khan", PlayerRole.BOWLER, 18, 88, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Peshawar", false, 2.3, "sixers"),
        Player("sx14", "Dahani Shah", PlayerRole.BOWLER, 12, 85, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Larkana", false, 2.1, "sixers"),
        Player("sx15", "Usman Shinwari", PlayerRole.BOWLER, 14, 82, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Rawalpindi", false, 1.4, "sixers"),

        // === GLADIATORS PLAYERS ===
        Player("g1", "Babar Azam", PlayerRole.BATSMAN, 94, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Lahore", false, 5.5, "gladiators"),
        Player("g2", "Fakhar Zaman", PlayerRole.BATSMAN, 88, 10, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Mardan", false, 4.2, "gladiators"),
        Player("g3", "Mohammad Rizwan", PlayerRole.WICKET_KEEPER, 91, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Peshawar", false, 5.0, "gladiators"),
        Player("g4", "Shaheen Afridi", PlayerRole.BOWLER, 25, 93, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Peshawar", false, 5.2, "gladiators"),
        Player("g5", "Rashid Khan", PlayerRole.ALL_ROUNDER, 68, 94, PlayingStyle.AGGRESSIVE, DeliveryStyle.SPIN, "Afghanistan", "Surrey", true, 4.8, "gladiators"),
        Player("g6", "Steve Smith", PlayerRole.BATSMAN, 92, 20, PlayingStyle.DEFENSIVE, DeliveryStyle.SPIN, "Australia", "Surrey", true, 3.8, "gladiators"),
        Player("g7", "Wanindu Hasaranga", PlayerRole.ALL_ROUNDER, 72, 89, PlayingStyle.AGGRESSIVE, DeliveryStyle.SPIN, "Sri Lanka", "Yorkshire", true, 3.5, "gladiators"),
        Player("g8", "Iftikhar Ahmed", PlayerRole.ALL_ROUNDER, 82, 70, PlayingStyle.AGGRESSIVE, DeliveryStyle.SPIN, "Pakistan", "Peshawar", false, 2.6, "gladiators"),
        Player("g9", "Imad Wasim", PlayerRole.ALL_ROUNDER, 78, 82, PlayingStyle.DEFENSIVE, DeliveryStyle.SPIN, "Pakistan", "Karachi", false, 2.8, "gladiators"),
        Player("g10", "Zaman Khan", PlayerRole.BOWLER, 10, 86, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Mirpur", false, 2.0, "gladiators"),
        Player("g11", "Osama Mir", PlayerRole.BOWLER, 12, 82, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Sialkot", false, 1.8, "gladiators"),
        Player("g12", "Haris Sohail", PlayerRole.BATSMAN, 79, 15, PlayingStyle.DEFENSIVE, DeliveryStyle.SPIN, "Pakistan", "Sialkot", false, 1.2, "gladiators"),
        Player("g13", "Khushdil Shah", PlayerRole.BATSMAN, 75, 40, PlayingStyle.AGGRESSIVE, DeliveryStyle.SPIN, "Pakistan", "Multan", false, 1.0, "gladiators"),
        Player("g14", "Mir Hamza", PlayerRole.BOWLER, 15, 84, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Karachi", false, 1.5, "gladiators"),
        Player("g15", "Shahnawaz Dahani", PlayerRole.BOWLER, 12, 83, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Larkana", false, 1.6, "gladiators"),

        // === EAGLES PLAYERS ===
        Player("e1", "Travis Head", PlayerRole.BATSMAN, 90, 35, PlayingStyle.AGGRESSIVE, DeliveryStyle.SPIN, "Australia", "Surrey", true, 4.4, "eagles"),
        Player("e2", "Mitchell Starc", PlayerRole.BOWLER, 22, 91, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Australia", "Yorkshire", true, 4.6, "eagles"),
        Player("e3", "Sam Curran", PlayerRole.ALL_ROUNDER, 78, 83, PlayingStyle.BALANCED, DeliveryStyle.FAST, "England", "Yorkshire", true, 3.6, "eagles"),
        Player("e4", "Saud Shakeel", PlayerRole.BATSMAN, 84, 15, PlayingStyle.DEFENSIVE, DeliveryStyle.SPIN, "Pakistan", "Karachi", false, 2.4, "eagles"),
        Player("e5", "Sarfraz Ahmed", PlayerRole.WICKET_KEEPER, 82, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Karachi", false, 1.8, "eagles"),
        Player("e6", "Anwar Ali Jr", PlayerRole.ALL_ROUNDER, 70, 75, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Sialkot", false, 1.0, "eagles"),
        Player("e7", "Omair Yousuf", PlayerRole.BATSMAN, 75, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Karachi", false, 0.9, "eagles"),
        Player("e8", "Mohammad Hasnain", PlayerRole.BOWLER, 15, 85, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Hyderabad", false, 2.2, "eagles"),
        Player("e9", "Akif Javed", PlayerRole.BOWLER, 12, 80, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Mardan", false, 0.8, "eagles"),
        Player("e10", "Abrar Ahmed", PlayerRole.BOWLER, 15, 87, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Karachi", false, 2.7, "eagles"),
        Player("e11", "Khurram Manzoor", PlayerRole.BATSMAN, 77, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Karachi", false, 0.7, "eagles"),
        Player("e12", "Mohammad Nawaz", PlayerRole.ALL_ROUNDER, 75, 81, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Rawalpindi", false, 2.5, "eagles"),
        Player("e13", "Sohaib Maqsood", PlayerRole.BATSMAN, 78, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Multan", false, 1.1, "eagles"),
        Player("e14", "Rumman Raees", PlayerRole.BOWLER, 15, 81, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Karachi", false, 1.2, "eagles"),
        Player("e15", "Zahid Mahmood", PlayerRole.BOWLER, 10, 80, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Larkana", false, 0.9, "eagles"),

        // === HAWKS PLAYERS ===
        Player("h1", "Josh Hazlewood", PlayerRole.BOWLER, 12, 90, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Australia", "Surrey", true, 4.2, "hawks"),
        Player("h2", "Glenn Maxwell", PlayerRole.ALL_ROUNDER, 86, 78, PlayingStyle.AGGRESSIVE, DeliveryStyle.SPIN, "Australia", "Yorkshire", true, 3.9, "hawks"),
        Player("h3", "David Miller", PlayerRole.BATSMAN, 89, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "South Africa", "Surrey", true, 3.5, "hawks"),
        Player("h4", "Shan Masood", PlayerRole.BATSMAN, 82, 5, PlayingStyle.DEFENSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Multan", false, 2.1, "hawks"),
        Player("h5", "Kamran Akmal Jr", PlayerRole.WICKET_KEEPER, 75, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Lahore", false, 1.0, "hawks"),
        Player("h6", "Tayyab Tahir", PlayerRole.BATSMAN, 79, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Sialkot", false, 1.5, "hawks"),
        Player("h7", "Shadab Khan", PlayerRole.ALL_ROUNDER, 80, 86, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Rawalpindi", false, 3.8, "hawks"),
        Player("h8", "Ahsan Ali", PlayerRole.BATSMAN, 73, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Karachi", false, 0.8, "hawks"),
        Player("h9", "Usman Qadir", PlayerRole.BOWLER, 15, 80, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Lahore", false, 1.2, "hawks"),
        Player("h10", "Wahab Riaz Jr", PlayerRole.BOWLER, 18, 82, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Lahore", false, 1.3, "hawks"),
        Player("h11", "Imran Khan Sr", PlayerRole.BOWLER, 14, 81, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Peshawar", false, 0.9, "hawks"),
        Player("h12", "Sohail Khan", PlayerRole.BOWLER, 16, 83, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Karachi", false, 1.1, "hawks"),
        Player("h13", "Sahibzada Farhan", PlayerRole.BATSMAN, 80, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Peshawar", false, 1.7, "hawks"),
        Player("h14", "Hammad Azam", PlayerRole.ALL_ROUNDER, 74, 76, PlayingStyle.BALANCED, DeliveryStyle.FAST, "Pakistan", "Rawalpindi", false, 1.2, "hawks"),
        Player("h15", "Mohammad Irfan Jr", PlayerRole.BOWLER, 12, 80, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Faisalabad", false, 0.8, "hawks"),

        // === FREE AGENTS / MARKET / AUCTION PLAYERS ===
        Player("f1", "Kane Williamson", PlayerRole.BATSMAN, 91, 10, PlayingStyle.DEFENSIVE, DeliveryStyle.SPIN, "New Zealand", "Yorkshire", true, 3.2, null),
        Player("f2", "Nicholas Pooran", PlayerRole.WICKET_KEEPER, 89, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "West Indies", "Surrey", true, 4.0, null),
        Player("f3", "Andre Russell", PlayerRole.ALL_ROUNDER, 84, 85, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "West Indies", "Surrey", true, 4.2, null),
        Player("f4", "Kagiso Rabada", PlayerRole.BOWLER, 18, 91, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "South Africa", "Lancashire", true, 4.4, null),
        Player("f5", "Rashid Khan Jr", PlayerRole.BOWLER, 22, 92, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Afghanistan", "Cardiff", true, 4.5, null),
        Player("f6", "Rovman Powell", PlayerRole.BATSMAN, 83, 10, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "West Indies", "Surrey", true, 2.0, null),
        Player("f7", "Haris Rauf", PlayerRole.BOWLER, 15, 88, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Rawalpindi", false, 3.5, null),
        Player("f8", "Mohammad Ali", PlayerRole.BOWLER, 10, 83, PlayingStyle.DEFENSIVE, DeliveryStyle.FAST, "Pakistan", "Sialkot", false, 1.4, null),
        Player("f9", "Abbas Afridi", PlayerRole.BOWLER, 16, 82, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Pakistan", "Peshawar", false, 1.3, null),
        Player("f10", "Usman Khan", PlayerRole.BATSMAN, 82, 5, PlayingStyle.AGGRESSIVE, DeliveryStyle.MEDIUM, "Pakistan", "Multan", false, 1.9, null),
        Player("f11", "Irfan Khan", PlayerRole.ALL_ROUNDER, 74, 60, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Pakistan", "Faisalabad", false, 1.2, null),
        Player("f12", "Litton Das", PlayerRole.WICKET_KEEPER, 80, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Bangladesh", "Lancashire", true, 1.5, null),
        Player("f13", "Sikandar Raza", PlayerRole.ALL_ROUNDER, 82, 83, PlayingStyle.BALANCED, DeliveryStyle.SPIN, "Zimbabwe", "Surrey", true, 2.8, null),
        Player("f14", "Pathum Nissanka", PlayerRole.BATSMAN, 84, 5, PlayingStyle.BALANCED, DeliveryStyle.MEDIUM, "Sri Lanka", "Kent", true, 2.0, null),
        Player("f15", "Hasan Ali", PlayerRole.BOWLER, 25, 84, PlayingStyle.AGGRESSIVE, DeliveryStyle.FAST, "Pakistan", "Gujranwala", false, 2.5, null)
    )

    // Setup active state list
    private val _playersStateFlow = MutableStateFlow<List<Player>>(masterPlayersList)
    val playersStateFlow: StateFlow<List<Player>> = _playersStateFlow.asStateFlow()

    // Franchise Teams list
    private val _teamsList = MutableStateFlow(listOf(
        Team("kings", "Kings", "KNG", "👑", mutableListOf(), "#FBC02D", 50.0),
        Team("stars", "Stars", "STR", "🌟", mutableListOf(), "#10B981", 50.0),
        Team("sixers", "Sixers", "SXR", "💖", mutableListOf(), "#EC4899", 50.0),
        Team("gladiators", "Gladiators", "GLD", "⚔️", mutableListOf(), "#8B5CF6", 50.0),
        Team("eagles", "Eagles", "EGL", "🦅", mutableListOf(), "#3B82F6", 50.0),
        Team("hawks", "Hawks", "HWK", "🔥", mutableListOf(), "#F97316", 50.0)
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
    private val _selectedTVChannel = MutableStateFlow("Roar Sports Live")
    val selectedTVChannel: StateFlow<String> = _selectedTVChannel.asStateFlow()

    private val _selectedSponsor = MutableStateFlow("Malik Sports")
    val selectedSponsor: StateFlow<String> = _selectedSponsor.asStateFlow()

    // Current matches state
    private val _scheduleMatches = MutableStateFlow<List<ScheduledMatch>>(emptyList())
    val scheduleMatches: StateFlow<List<ScheduledMatch>> = _scheduleMatches.asStateFlow()

    // Live standing details
    private val _standings = MutableStateFlow<List<LeagueStanding>>(emptyList())
    val standings: StateFlow<List<LeagueStanding>> = _standings.asStateFlow()

    init {
        registerInjectedPlayers()
        // Distribute starting roster players to their specified teamIds
        masterPlayersList.forEach { p ->
            if (p.teamId != null) {
                val team = _teamsList.value.firstOrNull { t -> t.id == p.teamId }
                team?.players?.add(p)
            }
        }
        recalculateBudgets()
        generateLeagueSchedule()
        recalculateStandings()
    }

    private fun addInjectedPlayer(
        id: String,
        name: String,
        bat: Int,
        bowl: Int,
        roleStr: String,
        aggStr: String,
        bowlType: String = "",
        extraFlags: String = "",
        nationality: String = "Pakistan",
        weaknessStr: String = "",
        assignedTeamId: String? = null
    ) {
        val role = when (roleStr) {
            "AR" -> PlayerRole.ALL_ROUNDER
            "WK" -> PlayerRole.WICKET_KEEPER
            "BL", "SB" -> PlayerRole.BOWLER
            else -> PlayerRole.BATSMAN
        }
        val playStyle = when (aggStr) {
            "A" -> PlayingStyle.AGGRESSIVE
            "D" -> PlayingStyle.DEFENSIVE
            "NA" -> PlayingStyle.BLITZKRIEG
            else -> PlayingStyle.BALANCED
        }
        
        val bType = bowlType.trim().lowercase()
        val deliveryStyle = when (bType) {
            "ls", "os", "lals", "laos", "lac" -> DeliveryStyle.SPIN
            "fb", "fbs" -> DeliveryStyle.FAST
            else -> DeliveryStyle.MEDIUM
        }

        val flags = extraFlags.lowercase()
        val isOpener = flags.contains("op")
        val isTopOrder = flags.contains("top") || flags.contains("4")
        val isFinisher = flags.contains("finisher")
        val isPowerHitter = flags.contains("power")

        // Clean up duplicates
        masterPlayersList.removeAll { it.name.trim().lowercase() == name.trim().lowercase() }

        val p = Player(
            id = id,
            name = name,
            role = role,
            battingSkill = bat,
            bowlingSkill = bowl,
            playStyle = playStyle,
            deliveryStyle = deliveryStyle,
            nationality = nationality,
            county = "Karachi",
            isForeign = (nationality != "Pakistan"),
            marketPriceCr = 1.0 + (bat + bowl) / 50.0,
            teamId = assignedTeamId,
            bowlingType = if (bType.isNotEmpty()) bType else "m",
            weakness = weaknessStr,
            isOpener = isOpener,
            isTopOrder = isTopOrder,
            isFinisher = isFinisher,
            isPowerHitter = isPowerHitter
        )
        masterPlayersList.add(p)
    }

    private fun registerInjectedPlayers() {
        // - International (Australia)
        addInjectedPlayer("inj_au1", "A. Haddin", 65, 56, "AR", "A", "m", "Finisher", "Australia", "ls,fbs")
        addInjectedPlayer("inj_au2", "Langer", 22, 72, "BL", "D", "fbs", "", "Australia")
        addInjectedPlayer("inj_au3", "Parsh", 76, 0, "WK", "N", "", "OP", "Australia")
        addInjectedPlayer("inj_au4", "Mausechate", 69, 23, "BT", "A", "", "Finisher", "Australia")
        addInjectedPlayer("inj_au5", "Wade", 25, 84, "BL", "D", "fbs", "", "Australia")
        addInjectedPlayer("inj_au6", "Lance", 76, 0, "BT", "N", "", "OP", "Australia")
        addInjectedPlayer("inj_au7", "M.G. Glaxen", 82, 65, "AR", "A", "ls", "OP,Power Hitter", "Australia")
        addInjectedPlayer("inj_au8", "J. Harris", 56, 45, "AR", "N", "m", "Finisher", "Australia")
        addInjectedPlayer("inj_au9", "Lin", 34, 68, "BL", "N", "fb", "", "Australia")
        addInjectedPlayer("inj_au10", "Wilton", 72, 64, "AR", "N", "mv", "", "Australia")

        // - International (New Zealand)
        addInjectedPlayer("inj_nz1", "Waller", 23, 67, "BL", "N", "mv", "", "New Zealand")
        addInjectedPlayer("inj_nz2", "B. Rington", 45, 56, "AR", "N", "fb", "Finisher", "New Zealand")
        addInjectedPlayer("inj_nz3", "Addams", 55, 45, "AR", "N", "fb", "Finisher", "New Zealand")
        addInjectedPlayer("inj_nz4", "S. Warner", 76, 33, "BT", "A", "", "OP", "New Zealand")
        addInjectedPlayer("inj_nz5", "Sprike", 80, 0, "WK", "A", "", "OP", "New Zealand")

        // - International (West Indies)
        addInjectedPlayer("inj_wi1", "Jordan", 23, 66, "BL", "N", "fb", "", "West Indies")
        addInjectedPlayer("inj_wi2", "N. Fill", 22, 68, "SB", "N", "lac", "", "West Indies")
        addInjectedPlayer("inj_wi3", "A. Chadwick", 73, 0, "WK", "A", "", "OP", "West Indies")

        // - International (Sri Lanka)
        addInjectedPlayer("inj_sl1", "Sriwardna", 67, 0, "BT", "D", "", "", "Sri Lanka")
        addInjectedPlayer("inj_sl2", "C. Dhanushka", 45, 66, "AR", "D", "ls", "", "Sri Lanka")

        // - International (South Africa)
        addInjectedPlayer("inj_sa1", "James", 63, 66, "AR", "A", "lals", "", "South Africa")
        addInjectedPlayer("inj_sa2", "Aram", 45, 66, "AR", "D", "os", "", "South Africa")

        // - International (England)
        addInjectedPlayer("inj_eng1", "N. Colin", 70, 61, "AR", "N", "laos", "", "England")
        addInjectedPlayer("inj_eng2", "D. Quentin", 56, 23, "BT", "N", "", "", "England")

        // - Spin Bowlers (SB)
        addInjectedPlayer("sb1", "Rahat", 12, 59, "SB", "N", "ls")
        addInjectedPlayer("sb2", "Abrar", 22, 62, "SB", "D", "os")
        addInjectedPlayer("sb3", "Anwar", 28, 81, "SB", "N", "ls")
        addInjectedPlayer("sb4", "Arshad", 22, 56, "SB", "D", "ls")
        addInjectedPlayer("sb5", "Mehrab", 16, 62, "SB", "D", "lals")
        addInjectedPlayer("sb6", "Bilal", 40, 78, "SB", "N", "os")
        addInjectedPlayer("sb7", "Adnan", 12, 56, "SB", "D", "laos")
        addInjectedPlayer("sb8", "Riaz", 11, 55, "SB", "N", "lac")
        addInjectedPlayer("sb9", "Amjad", 30, 69, "SB", "D", "os")
        addInjectedPlayer("sb10", "Rehan", 12, 61, "SB", "N", "ls")
        addInjectedPlayer("sb11", "N. Samad", 23, 55, "SB", "D", "lac")
        addInjectedPlayer("sb12", "M. Amjad", 45, 68, "SB", "N", "ls")
        addInjectedPlayer("sb13", "Asim", 23, 71, "SB", "D", "os")

        // - All-Rounders (AR)
        addInjectedPlayer("ar1", "Khalid", 54, 45, "AR", "N", "lals")
        addInjectedPlayer("ar2", "Taimoor", 56, 51, "AR", "N", "os")
        addInjectedPlayer("ar3", "Saeed", 60, 58, "AR", "N", "os")
        addInjectedPlayer("ar4", "Najaf", 41, 63, "AR", "D", "ls")
        addInjectedPlayer("ar5", "Jahangir", 60, 58, "AR", "D", "os", "Finisher")
        addInjectedPlayer("ar6", "M. Asghar", 56, 55, "AR", "N", "m", "Finisher")
        addInjectedPlayer("ar7", "Amir", 81, 85, "AR", "NA", "ls")
        addInjectedPlayer("ar8", "Mansoor", 55, 65, "AR", "N", "ls")
        addInjectedPlayer("ar9", "Aftab", 70, 61, "AR", "NA", "os", "OP")
        addInjectedPlayer("ar10", "Wahab", 50, 51, "AR", "N", "os")
        addInjectedPlayer("ar11", "Aaqib Raza", 78, 70, "AR", "A", "fb")
        addInjectedPlayer("ar12", "Sike", 87, 85, "AR", "NA", "fbs", "OP")
        addInjectedPlayer("ar13", "Nawaz", 57, 67, "AR", "A", "mv")
        addInjectedPlayer("ar14", "Muhammad Tahir", 60, 56, "AR", "A", "m")
        addInjectedPlayer("ar15", "Irfaan Ali", 70, 56, "AR", "N", "os")

        // - Wicket Keepers (WK)
        addInjectedPlayer("wk1", "M. Imran", 68, 60, "WK", "A")
        addInjectedPlayer("wk2", "S. Khan", 75, 87, "WK", "D", "", "OP")
        addInjectedPlayer("wk3", "Ali", 60, 67, "WK", "D")
        addInjectedPlayer("wk4", "A. Sajjad", 55, 69, "WK", "N")
        addInjectedPlayer("wk5", "Zulqarnain", 70, 78, "WK", "N", "", "OP")
        addInjectedPlayer("wk6", "Haseebullah", 72, 78, "WK", "NA", "", "OP")
        addInjectedPlayer("wk7", "Shahid Latif", 59, 67, "WK", "N")
        addInjectedPlayer("wk8", "Yaqoob", 63, 68, "WK", "D")
        addInjectedPlayer("wk9", "I. Javed", 84, 85, "WK", "NA", "", "OP")
        addInjectedPlayer("wk10", "M. Amin", 79, 80, "WK", "NA", "", "OP")
        addInjectedPlayer("wk11", "Aslam Sattar", 55, 60, "WK", "D")
        addInjectedPlayer("wk12", "Atiq Ali", 62, 72, "WK", "N", "", "OP")
        addInjectedPlayer("wk13", "Zahid", 77, 76, "WK", "N", "", "OP")
        addInjectedPlayer("wk14", "Uddin Ali", 55, 65, "WK", "N", "", "OP")
        addInjectedPlayer("wk15", "R. Saad", 60, 70, "WK", "N")

        // - Fast Bowlers (BL)
        addInjectedPlayer("bl1", "Ilyas", 11, 63, "BL", "D", "fb")
        addInjectedPlayer("bl2", "Waheed", 10, 55, "BL", "D", "mv")
        addInjectedPlayer("bl3", "M. Ali", 23, 67, "BL", "D", "mv")
        addInjectedPlayer("bl4", "Sohail", 24, 75, "BL", "D", "fbs")
        addInjectedPlayer("bl5", "Zia", 23, 72, "BL", "N", "fb")
        addInjectedPlayer("bl6", "Azam", 23, 70, "BL", "D", "fb")
        addInjectedPlayer("bl7", "Faraz Khan", 12, 56, "BL", "N", "mv")
        addInjectedPlayer("bl8", "Waleed", 23, 55, "BL", "D", "m")
        addInjectedPlayer("bl9", "Atif Maqbool", 12, 53, "BL", "N", "m")
        addInjectedPlayer("bl10", "Rizwan", 22, 70, "BL", "N", "fb")
        addInjectedPlayer("bl11", "Salman", 30, 73, "BL", "D", "fb")
        addInjectedPlayer("bl12", "Naseem", 22, 81, "BL", "D", "fb")
        addInjectedPlayer("bl13", "Aramzad", 25, 85, "BL", "N", "fbs")
        addInjectedPlayer("bl14", "M. Arif", 12, 55, "BL", "D", "m")
        addInjectedPlayer("bl15", "Waheed Ahmed", 16, 59, "BL", "D", "m")
        addInjectedPlayer("bl16", "Naeem", 22, 75, "BL", "N", "mv")
        addInjectedPlayer("bl17", "Akhlaq", 22, 69, "BL", "N", "fb")
        addInjectedPlayer("bl18", "Ahsan", 22, 78, "BL", "N", "fbs")
        addInjectedPlayer("bl19", "Farhan", 24, 80, "BL", "N", "fbs")
        addInjectedPlayer("bl20", "N. Javed", 22, 49, "BL", "D", "m")
        addInjectedPlayer("bl21", "Sohail Ahmed", 23, 46, "BL", "D", "m")
        addInjectedPlayer("bl22", "Muzafar", 22, 71, "BL", "N", "fb")
        addInjectedPlayer("bl23", "Sameen", 22, 72, "BL", "N", "fb")
        addInjectedPlayer("bl24", "Zohaib", 36, 85, "BL", "N", "fbs")
        addInjectedPlayer("bl25", "Iqrar", 19, 90, "BL", "D", "fbs", "Master")

        // - Batsmen (BT)
        addInjectedPlayer("bt1", "Jahid", 61, 22, "BT", "A", "", "Finisher")
        addInjectedPlayer("bt2", "Shahid", 68, 45, "BT", "N", "os")
        addInjectedPlayer("bt3", "Altaf", 55, 10, "BT", "N", "", "OP")
        addInjectedPlayer("bt4", "Yasir", 67, 12, "BT", "N")
        addInjectedPlayer("bt5", "Nauman", 72, 12, "BT", "N", "", "OP")
        addInjectedPlayer("bt6", "Nasir", 81, 48, "BT", "NA", "", "OP")
        addInjectedPlayer("bt7", "Haider", 62, 25, "BT", "N", "", "OP,Top Order")
        addInjectedPlayer("bt8", "Asad", 60, 11, "BT", "N", "", "OP")
        addInjectedPlayer("bt9", "Siraj", 63, 22, "BT", "D", "", "OP")
        addInjectedPlayer("bt10", "Aziz", 53, 22, "BT", "A")
        addInjectedPlayer("bt11", "Aslam", 71, 12, "BT", "D")
        addInjectedPlayer("bt12", "Abid", 79, 45, "BT", "NA", "", "OP,Top Order")
        addInjectedPlayer("bt13", "Husnain", 72, 22, "BT", "A", "", "Finisher")
        addInjectedPlayer("bt14", "Qasim", 45, 12, "BT", "N")
        addInjectedPlayer("bt15", "K. Navid", 72, 45, "BT", "N")
        addInjectedPlayer("bt16", "Shoaib Khan", 56, 25, "BT", "D", "", "Finisher")
        addInjectedPlayer("bt17", "A. Usman", 53, 22, "BT", "N", "", "OP")
        addInjectedPlayer("bt18", "Aafaq", 50, 10, "BT", "D")
        addInjectedPlayer("bt19", "Fakhrudin", 70, 23, "BT", "D")
        addInjectedPlayer("bt20", "A. Hafeez", 68, 11, "BT", "N", "", "OP")
        addInjectedPlayer("bt21", "Hamid Hasan", 70, 10, "BT", "A", "", "Finisher")
        addInjectedPlayer("bt22", "S. Hasan", 65, 10, "BT", "D")
        addInjectedPlayer("bt23", "Zakir", 59, 11, "BT", "D", "", "Finisher")
        addInjectedPlayer("bt24", "Sadiq", 46, 10, "BT", "D")
        addInjectedPlayer("bt25", "A. Jamal", 59, 0, "BT", "A", "", "Finisher")
        addInjectedPlayer("bt26", "Ashfaq", 55, 10, "BT", "D")
        addInjectedPlayer("bt27", "Farhan", 78, 10, "BT", "N", "", "OP")
        addInjectedPlayer("bt28", "M. Musa", 72, 8, "BT", "A", "", "Top Order,Finisher")
        addInjectedPlayer("bt29", "Abass", 72, 0, "BT", "A", "", "Finisher")
        addInjectedPlayer("bt30", "Faisal Hasan", 83, 60, "BT", "NA", "", "OP,Top Order,Finisher")
        addInjectedPlayer("bt31", "Muhammad Shahzain", 70, 34, "BT", "N", "", "Finisher")
        addInjectedPlayer("bt32", "Azhar", 75, 45, "BT", "A")
    }

    private fun recalculateBudgets() {
        _teamsList.value.forEach { team ->
            val totalSpent = team.players.sumOf { it.marketPriceCr }
            team.purseCr = (50.0 - totalSpent).coerceAtLeast(0.0)
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
        val list = userTeam.players
        if (direction == "up" && index > 0) {
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
        } else if (direction == "down" && index < list.size - 1) {
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
        }
        _teamsList.value = ArrayList(_teamsList.value)
    }

    fun swapPlayers(index1: Int, index2: Int) {
        val list = userTeam.players
        if (index1 in list.indices && index2 in list.indices) {
            val temp = list[index1]
            list[index1] = list[index2]
            list[index2] = temp
        }
        _teamsList.value = ArrayList(_teamsList.value)
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
        val currentRoster = userTeam.players
        val foreignCount = currentRoster.count { it.isForeign } + if (player.isForeign) 1 else 0
        val localCount = currentRoster.count { !it.isForeign } + if (!player.isForeign) 1 else 0

        if (currentRoster.size >= 21) return false
        if (userTeam.purseCr < player.marketPriceCr) return false

        // Check rules
        if (player.isForeign && foreignCount > 8) return false
        if (!player.isForeign && localCount < 12 && currentRoster.size + 1 >= 21) {
            // warning
        }

        // Deduct money and add to players list
        userTeam.players.add(player)
        player.teamId = userTeam.id
        
        // Remove from market pool/free agents
        masterPlayersList.firstOrNull { it.id == player.id }?.teamId = userTeam.id

        recalculateBudgets()
        _playersStateFlow.value = ArrayList(masterPlayersList)
        return true
    }

    fun releaseSquadPlayer(player: Player): Boolean {
        if (userTeam.players.size <= 16) return false // can't violate squad min size limit of 16 players

        val isRemoved = userTeam.players.removeIf { it.id == player.id }
        if (isRemoved) {
            player.teamId = null
            masterPlayersList.firstOrNull { it.id == player.id }?.teamId = null
            recalculateBudgets()
            _playersStateFlow.value = ArrayList(masterPlayersList)
            return true
        }
        return false
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

    private val _isExploitWeaknessActive = MutableStateFlow(false)
    val isExploitWeaknessActive = _isExploitWeaknessActive.asStateFlow()

    fun setExploitWeaknessActive(active: Boolean) {
        _isExploitWeaknessActive.value = active
    }

    private val _activeMilestoneText = MutableStateFlow<String?>(null)
    val activeMilestoneText = _activeMilestoneText.asStateFlow()

    private val _activeMilestoneSubtext = MutableStateFlow<String?>(null)
    val activeMilestoneSubtext = _activeMilestoneSubtext.asStateFlow()

    fun dismissMilestone() {
        _activeMilestoneText.value = null
        _activeMilestoneSubtext.value = null
    }

    private fun triggerMilestoneCelebration(title: String, subtitle: String) {
        _activeMilestoneText.value = title
        _activeMilestoneSubtext.value = subtitle
        stopAutoSimulate()
    }

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
        _matchStage.value = MatchStage.PLAYING

        addCommentary("INNINGS ${if (innings == 1) "1" else "2"}: ${_battingTeamNow.value.flagEmoji} ${_battingTeamNow.value.name} are coming in to bat! Primary Color accent overlay loaded.")
        addCommentary("Pitch conditions: ${_pitchType.value.displayName} - ${_pitchType.value.description}")
        addCommentary("Striker batsman: ${_strikerName.value}, Non-Striker: ${_nonStrikerName.value}. Bowler: ${_currentBowlerName.value} with the leather.")
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

        val outcome = SimulationEngine.simulateBall(
            batsman = striker,
            bowler = bowler,
            pitch = _pitchType.value,
            targetRuns = _target.value,
            currentRuns = _runs.value,
            wicketsDown = _wickets.value,
            ballsBowled = _balls.value,
            oversLimit = _oversLimit.value,
            exploitWeakness = _isExploitWeaknessActive.value
        )

        // Reset the captaincy action flag back to false for the next ball
        _isExploitWeaknessActive.value = false

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

        val oldRuns = strikerStats.runsScored
        val oldCareerRuns = striker.careerRuns

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

            // Persistency updates
            if (strikerStats.ballsFaced == 1) {
                striker.careerInningsBatting++
                striker.seasonMatches++
                striker.careerMatches++
            }
            striker.seasonRuns += outcome.runs
            striker.careerRuns += outcome.runs
            striker.seasonBallsFaced++
            if (strikerStats.runsScored > striker.highestScore) {
                striker.highestScore = strikerStats.runsScored
            }
            if (striker.careerInningsBatting > 0) {
                striker.battingAverage = striker.careerRuns.toDouble() / striker.careerInningsBatting
            }
            if (striker.seasonBallsFaced > 0) {
                striker.battingStrikeRate = (striker.careerRuns.toDouble() * 100.0) / (striker.seasonBallsFaced + (striker.careerMatches * 15))
            }

            if (bowlerStats.ballsBowled == 1) {
                bowler.careerInningsBowling++
                bowler.careerMatches = (bowler.careerMatches + 1).coerceAtLeast(1)
            }
            bowler.seasonBallsBowled++
            bowler.seasonRunsConceded += outcome.runs
        } else {
            _runs.value += outcome.totalRunsAwarded
            bowlerStats.runsConceded += outcome.totalRunsAwarded

            // Persistency updates for bowler on extras
            bowler.seasonRunsConceded += outcome.totalRunsAwarded

            if (outcome.extraType == "nb") {
                strikerStats.ballsFaced++
                strikerStats.runsScored += outcome.runs
                if (outcome.runs == 4) strikerStats.fours++
                if (outcome.runs == 6) strikerStats.sixes++
                bowlerStats.ballsBowled++

                // Persistency updates for front foot no ball batting increments
                if (strikerStats.ballsFaced == 1) {
                    striker.careerInningsBatting++
                    striker.seasonMatches++
                    striker.careerMatches++
                }
                striker.seasonRuns += outcome.runs
                striker.careerRuns += outcome.runs
                striker.seasonBallsFaced++
                if (strikerStats.runsScored > striker.highestScore) {
                    striker.highestScore = strikerStats.runsScored
                }

                if (bowlerStats.ballsBowled == 1) {
                    bowler.careerInningsBowling++
                }
                bowler.seasonBallsBowled++
            }
            updateBallLog(outcome.extraType + (if (outcome.runs > 0) "+${outcome.runs}" else ""))
        }

        // Live Milestones Celebrations Check
        val newRuns = strikerStats.runsScored
        if (oldRuns < 50 && newRuns >= 50) {
            striker.numFifties++
            triggerMilestoneCelebration(
                title = "FIFTY! 🌟",
                subtitle = "${striker.name} reaches 50! (Innings: $newRuns runs)"
            )
        } else if (oldRuns < 100 && newRuns >= 100) {
            striker.numCenturies++
            triggerMilestoneCelebration(
                title = "CENTURY! 🏏✨",
                subtitle = "CENTURY! ${striker.name} scores a magnificent 100!"
            )
        }

        // Form Streak Celebrations
        if (oldRuns < 30 && newRuns >= 30) {
            val hash = Math.abs(striker.name.hashCode()) % 5
            if (hash == 0) {
                triggerMilestoneCelebration(
                    title = "HOT FORM! 🔥",
                    subtitle = "${striker.name} records his 5th consecutive match with 30+ runs!"
                )
            } else if (hash == 1) {
                triggerMilestoneCelebration(
                    title = "HOT FORM! 🔥",
                    subtitle = "${striker.name} records his 3rd fifty in recent games!"
                )
            }
        }

        // Career Milestones Check
        val newCareerRuns = striker.careerRuns
        listOf(1000, 2000, 5000).forEach { m ->
            if (oldCareerRuns < m && newCareerRuns >= m) {
                triggerMilestoneCelebration(
                    title = "CAREER MILESTONE! 🎓🎒",
                    subtitle = "CAREER MILESTONE! ${striker.name} completes $m career runs!"
                )
            }
        }

        if (outcome.isWicket) {
            _wickets.value++
            strikerStats.isOut = true
            strikerStats.dismissalInfo = outcome.wicketType
            bowlerStats.wicketsTaken++

            bowler.seasonWickets++
            bowler.careerWickets++

            // Hauls celebrations
            if (bowlerStats.wicketsTaken == 3) {
                bowler.numThreeWickets++
                triggerMilestoneCelebration(
                    title = "3-WICKET HAUL! 🍒",
                    subtitle = "3-WICKET HAUL! ${bowler.name} takes 3 wickets in this match!"
                )
            } else if (bowlerStats.wicketsTaken == 5) {
                bowler.numFiveWickets++
                triggerMilestoneCelebration(
                    title = "FIVE-FOR! 🎭🔥",
                    subtitle = "FIVE-FOR! ${bowler.name} takes 5 wickets. Outstanding spell!"
                )
            }

            // Career Wickets check
            val oldWkts = bowler.careerWickets - 1
            listOf(50, 100).forEach { w ->
                if (oldWkts < w && bowler.careerWickets >= w) {
                    triggerMilestoneCelebration(
                        title = "CAREER WICKETS MILESTONE! 👑🦹",
                        subtitle = "CAREER MILESTONE! ${bowler.name} completes $w career wickets."
                    )
                }
            }

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
