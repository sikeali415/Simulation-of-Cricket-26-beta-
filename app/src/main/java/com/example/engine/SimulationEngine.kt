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
        exploitWeakness: Boolean = false
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
        // Check if active bowler's bowling type matches current batsman's weakness
        val isWeaknessMatched = batsman.weakness.split(",").map { it.trim() }.contains(bowler.bowlingType)

        // Scale up bowler skill by 1.25x if weakness matches primary bowling type
        val activeBowlerSkill = if (isWeaknessMatched) {
            (bowler.bowlingSkill * 1.25).coerceIn(1.0, 100.0)
        } else {
            bowler.bowlingSkill.toDouble()
        }

        // Calculate basic batting advantage and bowling advantage
        val batAdvLevel = (batsman.battingSkill - 50) / 100.0 // range -0.5 to 0.5
        val bowlAdvLevel = (activeBowlerSkill - 50) / 100.0 // range -0.5 to 0.5

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

        // Base probabilities of a wicket
        // Average wicket probability per normal ball is around 4.5% - 5.5% in cricket
        val baseWicketChance = 0.048
        var adjustedWktChance = baseWicketChance * (1 + bowlAdvLevel - batAdvLevel) * pitchWktMult * styleWktMult
        if (exploitWeakness && isWeaknessMatched) {
            adjustedWktChance *= 1.10
        }
        val finalWktChance = adjustedWktChance.coerceIn(0.012, 0.25)

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
                    val isBigHits = batsman.playStyle == PlayingStyle.AGGRESSIVE
                    if (isBigHits && random.nextBoolean()) {
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
        var userSkillRatio = (1.0 + batAdvLevel - bowlAdvLevel) * pitchBatMult * styleBatMult
        if (isWeaknessMatched) {
            userSkillRatio *= 0.75 // sweet spot contracts dynamically
        }
        if (exploitWeakness && isWeaknessMatched) {
            userSkillRatio *= 1.05 // risk of conceding 5% more runs if wicket execution fails
        }

        val dotWeight = (0.50 / (userSkillRatio.coerceAtLeast(0.3))).coerceIn(0.2, 0.8)
        val singleWeight = 0.32
        val twoWeight = 0.06
        val threeWeight = 0.01
        
        // Aggressive batters score way more boundaries
        val baseFourWeight = if (batsman.playStyle == PlayingStyle.AGGRESSIVE) 0.16 else 0.08
        val baseSixWeight = if (batsman.playStyle == PlayingStyle.AGGRESSIVE) 0.08 else 0.03

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
