package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlayerRole {
    BATSMAN,
    BOWLER,
    ALL_ROUNDER,
    WICKET_KEEPER
}

enum class PlayingStyle {
    AGGRESSIVE, // High risk, many boundaries
    DEFENSIVE,  // Low risk, many dot balls and singles
    BALANCED,   // Balanced risk
    BLITZKRIEG  // Highly Aggressive / Blitzkrieg
}

enum class DeliveryStyle {
    FAST,   // High wicket potential, higher run rate potential
    SPIN,   // Lower run rate potential, effective on DUSTY pitches
    MEDIUM  // Balanced pace and control
}

enum class MatchFormat(val displayName: String, val maxOvers: Int, val maxOversPerBowler: Int) {
    T20("T20 League", 20, 4),
    ODI("One-Day Cup", 50, 10),
    FIRST_CLASS("First-Class Shield", 90, 25)
}

data class Player(
    val id: String,
    val name: String,
    val role: PlayerRole,
    val battingSkill: Int, // 1 to 100
    val bowlingSkill: Int, // 1 to 100
    val playStyle: PlayingStyle = PlayingStyle.BALANCED,
    val deliveryStyle: DeliveryStyle = DeliveryStyle.MEDIUM,
    val nationality: String = "Pakistan",
    val county: String = "Lahore",
    val isForeign: Boolean = false,
    var marketPriceCr: Double = 1.0,
    var teamId: String? = null,
    // Season accumulators
    var seasonMatches: Int = 0,
    var seasonRuns: Int = 0,
    var seasonWickets: Int = 0,
    var seasonBallsFaced: Int = 0,
    var seasonBallsBowled: Int = 0,
    var seasonRunsConceded: Int = 0,
    var bowlingType: String = "m",
    var weakness: String = "",
    var isOpener: Boolean = false,
    var isTopOrder: Boolean = false,
    var isFinisher: Boolean = false,
    var isPowerHitter: Boolean = false,
    // Career Accumulators
    var careerMatches: Int = 0,
    var careerInningsBatting: Int = 0,
    var careerInningsBowling: Int = 0,
    var careerRuns: Int = 0,
    var highestScore: Int = 0,
    var battingAverage: Double = 0.0,
    var battingStrikeRate: Double = 0.0,
    var numFifties: Int = 0,
    var numCenturies: Int = 0,
    var careerWickets: Int = 0,
    var bestBowlingFigures: String = "0/0",
    var bowlingEconomy: Double = 0.0,
    var numThreeWickets: Int = 0,
    var numFiveWickets: Int = 0
) {
    init {
        // Algorithmic Weakness Generation Rule:
        // If weakness isn't explicitly defined and Batting Skill > 70,
        // assign them an absolute vulnerability trait based on their lower skill subsets.
        if (weakness.isEmpty() && battingSkill > 70) {
            val hash = name.hashCode() + id.hashCode() + battingSkill
            weakness = when (Math.abs(hash) % 4) {
                0 -> "ls"    // Facing Leg Spin
                1 -> "lals"  // Facing Left-Arm Spin
                2 -> "fbs"   // Facing Fast Bowler with More Control
                else -> "laos" // Facing Left Arm Off-Spin
            }
        }

        // Initialize Sensible Default Starting Career Stats Based on Roles & Skills
        if (careerMatches == 0) {
            val hash = Math.abs(name.hashCode() + id.hashCode())
            val baseM = 15 + (hash % 80) // 15 to 95 matches
            careerMatches = baseM
            
            if (role == PlayerRole.BATSMAN || role == PlayerRole.ALL_ROUNDER || role == PlayerRole.WICKET_KEEPER) {
                careerInningsBatting = (baseM * 0.9).toInt().coerceAtLeast(1)
                val baseRunsPerInn = (battingSkill * 0.35) + (hash % 10)
                careerRuns = (careerInningsBatting * baseRunsPerInn).toInt()
                highestScore = (battingSkill * 1.1 + (hash % 40)).toInt().coerceIn(30, 185)
                battingAverage = (careerRuns.toDouble() / careerInningsBatting).coerceIn(10.0, 62.0)
                battingStrikeRate = 110.0 + (battingSkill * 0.6) + (hash % 15)
                numFifties = (careerInningsBatting / 8).coerceAtLeast(0)
                numCenturies = (careerInningsBatting / 40).coerceAtLeast(0)
            }
            if (role == PlayerRole.BOWLER || role == PlayerRole.ALL_ROUNDER) {
                careerInningsBowling = (baseM * 0.85).toInt().coerceAtLeast(1)
                val wktsPerInn = (bowlingSkill / 35.0) + ((hash % 50) / 100.0)
                careerWickets = (careerInningsBowling * wktsPerInn).toInt().coerceAtLeast(1)
                val bestW = (3 + (hash % 4)).coerceIn(2, 7)
                val bestR = (10 + (hash % 35)).coerceIn(5, 50)
                bestBowlingFigures = "$bestW/$bestR"
                bowlingEconomy = (10.0 - (bowlingSkill * 0.04) + ((hash % 100) / 100.0)).coerceIn(4.5, 9.8)
                numThreeWickets = (careerInningsBowling / 12).coerceAtLeast(0)
                numFiveWickets = (careerInningsBowling / 45).coerceAtLeast(0)
            }
        }
    }
}

data class Team(
    val id: String,
    val name: String,
    val abbreviation: String,
    val flagEmoji: String,
    val players: MutableList<Player> = mutableListOf(),
    var colorHex: String = "#3B82F6",
    var purseCr: Double = 50.0
)

@Entity(tableName = "match_history")
data class MatchHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateTime: Long = System.currentTimeMillis(),
    val homeTeamName: String,
    val awayTeamName: String,
    val winnerTeamName: String,
    val homeRuns: Int,
    val homeWickets: Int,
    val homeBallsBowled: Int,
    val awayRuns: Int,
    val awayWickets: Int,
    val awayBallsBowled: Int,
    val oversLimit: Int,
    val margin: String,
    val manOfTheMatch: String,
    val matchSummary: String // Summary of scorecards
)

enum class PitchType(val displayName: String, val description: String) {
    BALANCED("Balanced", "An even contest between bat and ball."),
    FLAT("Flat Track", "Batter's paradise! High scoring, easier boundaries."),
    GREEN("Green Top", "Favors fast bowlers. Extra bounce and swing."),
    DUSTY("Dusty Turner", "Favors spinners. Sharp turn and uneven bounce.")
}

enum class MatchStage {
    TOSS,
    PLAYING,
    INNINGS_BREAK,
    FINISHED
}

data class PlayerMatchStats(
    val name: String,
    var runsScored: Int = 0,
    var ballsFaced: Int = 0,
    var fours: Int = 0,
    var sixes: Int = 0,
    var isOut: Boolean = false,
    var dismissalInfo: String = "",
    var oversBowled: Double = 0.0,
    var maidenOvers: Int = 0,
    var runsConceded: Int = 0,
    var wicketsTaken: Int = 0,
    var ballsBowled: Int = 0
) {
    val strikeRate: Double
        get() = if (ballsFaced > 0) (runsScored.toDouble() / ballsFaced) * 100 else 0.0

    val economy: Double
        get() = if (ballsBowled > 0) (runsConceded.toDouble() / (ballsBowled.toDouble() / 6.0)) else 0.0
}
