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
    BLITZKRIEG  // Maximum risk, maximum power
}

enum class DeliveryStyle {
    FAST,   // High wicket potential, higher run rate potential
    SPIN,   // Lower run rate potential, effective on DUSTY pitches
    MEDIUM  // Balanced pace and control
}

enum class BattingHand {
    RIGHT_HAND,
    LEFT_HAND
}

enum class BowlingType {
    LEG_SPIN,
    OFF_SPIN,
    LEFT_ARM_LEG_SPIN,
    LEFT_ARM_OFF_SPIN,
    CHINAMAN,
    MEDIUM_VARIATIONS,
    FAST_BOWLER,
    FAST_BOWLER_EXTRA_PACE
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
    val battingHand: BattingHand = BattingHand.RIGHT_HAND,
    val bowlingType: BowlingType = BowlingType.FAST_BOWLER,
    val weakness: BowlingType? = null,
    val strengths: String = "Reliable under pressure",
    val bestPosition: Int = 3,
    val isFinisher: Boolean = false,
    val isPowerHitter: Boolean = false,
    val bio: String = "A disciplined athlete with high regional potential.",
    val age: Int = 26,
    val yearsProfessional: Int = 4,
    val bowlingSpeed: Int = 135, // Average speed in km/h
    val swingAbility: Int = 40,   // 1 to 100
    val turnAbility: Int = 30,    // 1 to 100
    val nationality: String = "Pakistan",
    val county: String = "Lahore",
    val isForeign: Boolean = false,
    var marketPriceCr: Double = 1.0,
    var teamId: String? = null,
    val isCaptain: Boolean = false,
    // Season accumulators
    var seasonMatches: Int = 0,
    var seasonRuns: Int = 0,
    var seasonWickets: Int = 0,
    var seasonBallsFaced: Int = 0,
    var seasonBallsBowled: Int = 0,
    var seasonRunsConceded: Int = 0
)

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
