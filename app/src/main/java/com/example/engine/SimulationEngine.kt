package com.example.engine

import com.example.data.models.*
import kotlin.random.Random

data class BallOutcome(
    val runs: Int,
    val isWicket: Boolean,
    val wicketType: String = "",
    val isExtra: Boolean = false,
    val extraType: String = "", // "wd" (wide), "nb" (no ball), "b" (bye)
    val totalRunsAwarded: Int,
    val commentary: String,
    val batsmanStrikeRotates: Boolean
)

object SimulationEngine {
    
    private val random = Random(System.currentTimeMillis())

    fun simulateBall(
        batsman: Player,
        bowler: Player,
        pitch: PitchType,
        targetRuns: Int?, // if 2nd innings, target to win
        currentRuns: Int,
        wicketsDown: Int,
        ballsBowled: Int,
        oversLimit: Int,
        isPlaySafe: Boolean = false // New tactical parameter
    ): BallOutcome {
        // 1. Extras probability (Wide, No Ball)
        // Fast bowlers have slightly higher extra rate
        val extraChance = when (bowler.deliveryStyle) {
            DeliveryStyle.FAST -> 0.05
            DeliveryStyle.MEDIUM -> 0.03
            DeliveryStyle.SPIN -> 0.02
        }

        if (random.nextDouble() < extraChance) {
            val isWide = random.nextBoolean()
            if (isWide) {
                val extrasRuns = 1 + (if (random.nextDouble() < 0.05) 1 else 0) // sometimes wide goes past keeper for boundary/single
                val commentary = "Wide ball! Conceded by ${bowler.name}. High above the off-stump, a tough reach for the wicket-keeper."
                return BallOutcome(
                    runs = 0,
                    isWicket = false,
                    isExtra = true,
                    extraType = "wd",
                    totalRunsAwarded = extrasRuns,
                    commentary = commentary,
                    batsmanStrikeRotates = false
                )
            } else {
                val addedRuns = if (random.nextDouble() < 0.20) 4 else if (random.nextDouble() < 0.30) 1 else 0
                val commentary = "No Ball! Overstepped front foot by ${bowler.name}. Free Hit is declared!"
                return BallOutcome(
                    runs = addedRuns,
                    isWicket = false,
                    isExtra = true,
                    extraType = "nb",
                    totalRunsAwarded = 1 + addedRuns,
                    commentary = commentary,
                    batsmanStrikeRotates = (addedRuns % 2 != 0)
                )
            }
        }

        // 2. Wicket vs Run scenario
        // Calculate basic batting advantage and bowling advantage
        val batAdvLevel = (batsman.battingSkill - 50) / 100.0 // range -0.5 to 0.5
        val bowlAdvLevel = (bowler.bowlingSkill - 50) / 100.0 // range -0.5 to 0.5

        // WEAKNESS SYSTEM IMPACT
        var weaknessWktMult = 1.0
        var weaknessBatMult = 1.0
        if (batsman.weakness != null && batsman.weakness == bowler.bowlingType) {
            // Bowler matches batter weakness
            weaknessWktMult = 1.25  // increase wicket chance
            weaknessBatMult = 0.80  // reduce timing/skill ratio
        }

        // Pitch multipliers
        var pitchBatMult = 1.0
        var pitchWktMult = 1.0

        when (pitch) {
            PitchType.FLAT -> {
                pitchBatMult = 1.25
                pitchWktMult = 0.70
            }
            PitchType.GREEN -> {
                if (bowler.deliveryStyle == DeliveryStyle.FAST) {
                    pitchWktMult = 1.35
                    pitchBatMult = 0.80
                }
            }
            PitchType.DUSTY -> {
                if (bowler.deliveryStyle == DeliveryStyle.SPIN) {
                    pitchWktMult = 1.40
                    pitchBatMult = 0.75
                }
            }
            PitchType.BALANCED -> {}
        }

        // Style multipliers
        var styleBatMult = 1.0
        var styleWktMult = 1.0

        when (batsman.playStyle) {
            PlayingStyle.BLITZKRIEG -> {
                styleBatMult = 1.90
                styleWktMult = 2.40
            }
            PlayingStyle.AGGRESSIVE -> {
                styleBatMult = 1.45
                styleWktMult = 1.70
            }
            PlayingStyle.DEFENSIVE -> {
                styleBatMult = 0.40
                styleWktMult = 0.35
            }
            PlayingStyle.BALANCED -> {}
        }

        // TACTICAL "PLAY SAFE" OVERRIDE
        var playSafeWktMult = 1.0
        var playSafeBatMult = 1.0
        if (isPlaySafe) {
            playSafeWktMult = 0.40 // Reduce dismissal risk greatly
            playSafeBatMult = 0.60 // Reduce risky shots and boundary probability
        }

        // Base probabilities of a wicket
        // Average wicket probability per normal ball is around 4.5% - 5.5% in cricket
        val baseWicketChance = 0.048
        val adjustedWktChance = baseWicketChance * (1 + bowlAdvLevel - batAdvLevel) * pitchWktMult * styleWktMult * weaknessWktMult * playSafeWktMult
        val finalWktChance = adjustedWktChance.coerceIn(0.010, 0.22)

        if (random.nextDouble() < finalWktChance) {
            // It's a wicket!
            val wicketTypes = listOf("Bowled", "Caught", "LBW", "Run Out", "Stumped")
            val wktType = when (bowler.deliveryStyle) {
                DeliveryStyle.FAST -> {
                    // Fast bowlers get more Bowled/LBW/Caught behind
                    val weights = listOf(0.35, 0.45, 0.15, 0.03, 0.02)
                    getRandomWeightedItem(wicketTypes, weights)
                }
                DeliveryStyle.SPIN -> {
                    // Spin bowlers get more Stumped/Caught/LBW
                    val weights = listOf(0.15, 0.55, 0.15, 0.03, 0.12)
                    getRandomWeightedItem(wicketTypes, weights)
                }
                DeliveryStyle.MEDIUM -> {
                    val weights = listOf(0.25, 0.55, 0.15, 0.03, 0.02)
                    getRandomWeightedItem(wicketTypes, weights)
                }
            }

            val details = when (wktType) {
                "Bowled" -> "bowled ${bowler.name}"
                "Caught" -> {
                    val fielders = listOf("mid-on", "deep-wicket", "point", "long-off", "keeper", "slip")
                    "ct ${fielders.random()} b ${bowler.name}"
                }
                "LBW" -> "lbw b ${bowler.name}"
                "Run Out" -> "run out (fielder)"
                "Stumped" -> "stumped b ${bowler.name}"
                else -> "dismissed b ${bowler.name}"
            }

            val wicketComms = when (wktType) {
                "Bowled" -> "OUT! Clean bowled! ${batsman.name} is stunned. Perfect delivery from ${bowler.name} clips the top of off-stump. Absolute beauty!"
                "Caught" -> {
                    val isBigHits = batsman.playStyle == PlayingStyle.AGGRESSIVE || batsman.playStyle == PlayingStyle.BLITZKRIEG
                    if (isBigHits && random.nextDouble() < 0.7) {
                        "OUT! Caught in the deep! ${batsman.name} goes big but doesn't get the timing. Flies high and is safely taken near the boundary ropes."
                    } else {
                        "OUT! Edged and taken! Smart length from ${bowler.name}, ${batsman.name} edges it straight to the awaiting hands."
                    }
                }
                "LBW" -> "OUT! Huge appeal... and given! ${batsman.name} steps across the line. ${bowler.name} appeals loudly, and the umpire raises the finger. Dead plumb!"
                "Run Out" -> "OUT! Exceptional field work! Sharp throw from the circle, ${batsman.name} is caught short of the crease. Brilliant run out!"
                "Stumped" -> "OUT! Stumped! ${batsman.name} steps down the track, completely beaten by the spin of ${bowler.name}. The keeper whips the bails off in a flash!"
                else -> "OUT! ${batsman.name} has been dismissed here. ${bowler.name} is celebrating."
            }

            return BallOutcome(
                runs = 0,
                isWicket = true,
                wicketType = details,
                totalRunsAwarded = 0,
                commentary = wicketComms,
                batsmanStrikeRotates = false
            )
        }

        // 3. Runs calculation if not a wicket
        // Probability distribution of runs: 0, 1, 2, 3, 4, 6
        // Base weights: [Dot: 50%, Single: 32%, Two: 6%, Three: 1%, Four: 8%, Six: 3%]
        val runOptions = listOf(0, 1, 2, 3, 4, 6)
        
        // Base weights adjusted by skill and pitch and styles
        val userSkillRatio = (1.0 + batAdvLevel - bowlAdvLevel) * pitchBatMult * styleBatMult * weaknessBatMult * playSafeBatMult

        val dotWeight = (0.50 / (userSkillRatio.coerceAtLeast(0.3))).coerceIn(0.2, 0.8)
        val singleWeight = 0.32
        val twoWeight = 0.06
        val threeWeight = 0.01
        
        // Aggressive batters score way more boundaries
        var baseFourWeight = 0.08
        var baseSixWeight = 0.03

        when(batsman.playStyle) {
            PlayingStyle.BLITZKRIEG -> {
                baseFourWeight = 0.22
                baseSixWeight = 0.14
            }
            PlayingStyle.AGGRESSIVE -> {
                baseFourWeight = 0.16
                baseSixWeight = 0.08
            }
            else -> {}
        }

        val fourWeight = baseFourWeight * userSkillRatio
        val sixWeight = baseSixWeight * userSkillRatio

        val totalWeight = dotWeight + singleWeight + twoWeight + threeWeight + fourWeight + sixWeight
        
        // Normalize and pick run
        val normalizedWeights = listOf(
            dotWeight / totalWeight,
            singleWeight / totalWeight,
            twoWeight / totalWeight,
            threeWeight / totalWeight,
            fourWeight / totalWeight,
            sixWeight / totalWeight
        )

        val runsScored = getRandomWeightedItem(runOptions, normalizedWeights)

        val runComms = when (runsScored) {
            0 -> listOf(
                "No run. Played defensively back to ${bowler.name}.",
                "0 runs. Swing and a miss! Stiff pace beats ${batsman.name}.",
                "No run. Solid defensive block, tapped into the covers.",
                "Dot ball. Good tight line on the off-stump, cannot get away."
            ).random()
            1 -> listOf(
                "1 run. Pushed into the gap for a quick single.",
                "1 run. Tucked away onto the leg side, rotates the strike.",
                "Single! Guided down to third man, comfortable run.",
                "Fast run! Played with soft hands, batsman sprint across."
            ).random()
            2 -> listOf(
                "2 runs. Driven nicely through the covers, excellent running between wickets.",
                "2 runs. Punched into mid-wicket area, batsman hustle back for the second.",
                "Two runs! Clipped off the pads, plenty of space out deep."
            ).random()
            3 -> "3 runs! Excellent placement! Picked up the gaps, sprinted hard to secure three great runs."
            4 -> listOf(
                "FOUR! Exquisite shot! Slashed backward of point, racing away to the boundary fence.",
                "FOUR! Elegant drive! Crushed down the ground past the bowler, no chance for the fielders.",
                "FOUR runs! Pulled with authority! Cracking sound off the bat as it clears mid-wicket.",
                "FOUR! Edged, but runs anyway! Past the slip region and rolls over the rope."
            ).random()
            6 -> listOf(
                "SIX! Out of the park! ${batsman.name} connects beautifully, dispatching this deep into the grandstands!",
                "SIX runs! Absolute monster! Stand and deliver, clears the boundary ropes with epic height and distance.",
                "SIX! Hammered! Lofted over long-on, massive strike that sends the crowd wild!",
                "SIX! Unbelievable power! Smashed flat and hard over deep square leg, what a shot!"
            ).random()
            else -> "$runsScored runs. Runs collected safely."
        }

        return BallOutcome(
            runs = runsScored,
            isWicket = false,
            totalRunsAwarded = runsScored,
            commentary = runComms,
            batsmanStrikeRotates = (runsScored % 2 != 0)
        )
    }

    private fun <T> getRandomWeightedItem(items: List<T>, weights: List<Double>): T {
        val rand = random.nextDouble()
        var cumSum = 0.0
        for (i in items.indices) {
            cumSum += weights[i]
            if (rand <= cumSum) {
                return items[i]
            }
        }
        return items.last()
    }
}
