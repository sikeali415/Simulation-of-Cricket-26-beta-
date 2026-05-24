import React, { useCallback } from 'react';
import { GameData, Format, PlayerRole, MatchResult, Inning, BattingPerformance, BowlingPerformance, Team, Match, Player } from '../types';
import { PITCH_MODIFIERS, formatOvers, getPlayerById, generateAutoXI, getBatterTier, BATTING_PROFILES, calculatePopularityPoints } from '../utils';
import { generateSingleFormatInitialStats } from '../data';

export const useSimulation = (gameData: GameData, setGameData: React.Dispatch<React.SetStateAction<GameData | null>>) => {
    const simulateInning = useCallback((battingTeam: Team, bowlingTeam: Team, format: Format, target: number | null, pitch: string, groundCode: string, inningNumber: number, allPlayers: Player[], playerForms: Record<string, number>): Inning => {
        const pitchMods = PITCH_MODIFIERS[pitch as keyof typeof PITCH_MODIFIERS] || PITCH_MODIFIERS["Balanced Sporting Pitch"];
        const formatMods = pitchMods[format];
        let score = 0, wickets = 0, balls = 0, extras = 0;
        
        // Determine max balls based on format - STRICTOR LIMITS
        const isT20 = format.includes('T20');
        const isODI = format.includes('One-Day') || format.includes('List-A');
        const maxBalls = (isT20 ? 20 : isODI ? 50 : 90) * 6;
        
        let limits: any = null;
        const groundLimits = gameData.scoreLimits?.[groundCode];
        if (groundLimits) {
            const formatLimits = groundLimits[format];
            if (formatLimits) {
                limits = formatLimits[inningNumber];
            }
        }
        const maxWicketsForInning = (limits?.maxWickets && limits.maxWickets > 0 && limits.maxWickets <= 10) ? limits.maxWickets : 10;

        const battingLineup: BattingPerformance[] = battingTeam.squad.map((p, idx) => { 
            const d = getPlayerById(p.id, allPlayers); 
            return { 
                playerId: d.id, 
                playerName: d.name, 
                runs: 0, 
                balls: 0, 
                fours: 0, 
                sixes: 0, 
                isOut: false, 
                dismissalText: 'not out', 
                dismissal: { type: 'not out', bowlerId: '' }, 
                ballsToFifty: 0, 
                ballsToHundred: 0,
                ppRuns: 0, ppBalls: 0, ppDismissals: 0,
                moRuns: 0, moBalls: 0, moDismissals: 0,
                doRuns: 0, doBalls: 0, doDismissals: 0,
                battingPosition: idx + 1
            }; 
        });
        
        const bowlingLineup = bowlingTeam.squad
            .filter(p => getPlayerById(p.id, allPlayers).role !== PlayerRole.WICKET_KEEPER)
            .map(p => { 
                const d = getPlayerById(p.id, allPlayers); 
                return { 
                    playerId: d.id, 
                    playerName: d.name, 
                    overs: '0.0', 
                    maidens: 0, 
                    runsConceded: 0, 
                    wickets: 0, 
                    ballsBowled: 0,
                    role: d.role,
                    skill: d.secondarySkill,
                    ppWickets: 0, ppRunsConceded: 0, ppBallsBowled: 0,
                    moWickets: 0, moRunsConceded: 0, moBallsBowled: 0,
                    doWickets: 0, doRunsConceded: 0, doBallsBowled: 0
                } 
            });
        
        if (bowlingLineup.length < 2) { 
            // Ensure at least 2 bowlers to avoid consecutive overs bug
            const p1 = getPlayerById(bowlingTeam.squad[0].id, allPlayers);
            const p2 = getPlayerById(bowlingTeam.squad[1]?.id || bowlingTeam.squad[0].id, allPlayers);
            
            if (bowlingLineup.length === 0) {
                bowlingLineup.push({ 
                    playerId: p1.id, playerName: p1.name, overs: '0.0', maidens: 0, runsConceded: 0, wickets: 0, ballsBowled: 0, role: p1.role, skill: p1.secondarySkill,
                    ppWickets: 0, ppRunsConceded: 0, ppBallsBowled: 0,
                    moWickets: 0, moRunsConceded: 0, moBallsBowled: 0,
                    doWickets: 0, doRunsConceded: 0, doBallsBowled: 0
                });
            }
            if (bowlingLineup.length === 1) {
                bowlingLineup.push({ 
                    playerId: p2.id, playerName: p2.name, overs: '0.0', maidens: 0, runsConceded: 0, wickets: 0, ballsBowled: 0, role: p2.role, skill: p2.secondarySkill,
                    ppWickets: 0, ppRunsConceded: 0, ppBallsBowled: 0,
                    moWickets: 0, moRunsConceded: 0, moBallsBowled: 0,
                    doWickets: 0, doRunsConceded: 0, doBallsBowled: 0
                });
            }
        }

        let onStrikeBatterIndex = 0, offStrikeBatterIndex = 1, bowlerIndex = 0, prevBowlerIndex = -1, runsThisOver = 0;

        while (balls < maxBalls && wickets < maxWicketsForInning) {
            if (target && score >= target) break;
            if (limits?.maxRuns && limits.maxRuns > 0 && score >= limits.maxRuns) break;

            const onStrikeBatter = battingLineup[onStrikeBatterIndex];
            if (!onStrikeBatter) break; 
            const onStrikeBatterDetails = getPlayerById(onStrikeBatter.playerId, allPlayers);
            const currentBowler = bowlingLineup[bowlerIndex];
            const bowlerDetails = getPlayerById(currentBowler.playerId, allPlayers);

            // Form factor: Random performance variance for this match
            const batterForm = playerForms[onStrikeBatterDetails.id] || 1.0;
            const bowlerForm = playerForms[bowlerDetails.id] || 1.0;

            // Fatigue factor: Skills decrease slightly as they play more
            const batterFatigue = Math.max(0.75, 1 - (onStrikeBatter.balls / 250)) * batterForm;
            const bowlerFatigue = Math.max(0.7, 1 - (currentBowler.ballsBowled / 150)) * bowlerForm;

            // Pressure factor: In a chase, required rate affects performance
            let pressureFactor = 1.0;
            let aggressionFactor = 1.0;
            
            if (target) {
                const remainingBalls = maxBalls - balls;
                const remainingRuns = target - score;
                if (remainingBalls > 0) {
                    const requiredRR = (remainingRuns / remainingBalls) * 6;
                    
                    // Batting aggression based on required rate - more dynamic
                    if (isT20) {
                        if (requiredRR > 12) aggressionFactor = 1.6;
                        else if (requiredRR > 10) aggressionFactor = 1.4;
                        else if (requiredRR > 8) aggressionFactor = 1.25;
                        else if (requiredRR < 6) aggressionFactor = 0.95;
                    } else if (isODI) {
                        if (requiredRR > 10) aggressionFactor = 1.5;
                        else if (requiredRR > 8) aggressionFactor = 1.3;
                        else if (requiredRR > 6) aggressionFactor = 1.15;
                        else if (requiredRR < 4) aggressionFactor = 0.85;
                    }
                    
                    // Pressure increases wicket chance if required rate is high
                    // BUT high skill batters handle it better
                    const skillClutchness = (onStrikeBatterDetails.battingSkill - 60) / 100;
                    if (requiredRR > 9) {
                        const basePressure = (requiredRR - 9) * 0.06;
                        pressureFactor = 1 + Math.max(0, basePressure - skillClutchness);
                    }
                }
            } else {
                // First innings aggression
                const progress = balls / maxBalls;
                if (isT20) {
                    if (progress > 0.8) aggressionFactor = 1.4; // Death overs
                    else if (progress > 0.5) aggressionFactor = 1.15;
                    else if (progress < 0.3) aggressionFactor = 1.05; // Powerplay
                } else if (isODI) {
                    if (progress > 0.9) aggressionFactor = 1.5;
                    else if (progress > 0.7) aggressionFactor = 1.2;
                    else if (progress < 0.2) aggressionFactor = 1.1;
                }
            }

            let batterProfile;
            const customProfile = onStrikeBatterDetails.customProfiles?.[format];
            if (customProfile && customProfile.avg > 0 && customProfile.sr > 0) {
                batterProfile = customProfile;
            } else {
                const batterTier = getBatterTier(onStrikeBatterDetails.battingSkill * batterFatigue);
                const batterStyle = onStrikeBatterDetails.style;
                // @ts-ignore
                batterProfile = BATTING_PROFILES[format][batterTier][batterStyle] || BATTING_PROFILES[format][batterTier]['N'];
            }

            let effectiveChasePenalty = target !== null ? pitchMods.chasePenalty : 1;
            if (target && aggressionFactor > 1.3) effectiveChasePenalty = 1.0;

            const expectedRunsPerBall = (batterProfile.sr / 100) * aggressionFactor * effectiveChasePenalty;
            
            // Skill based risk management: elite batters don't lose their wicket as easily when aggressive
            const riskMitigation = (onStrikeBatterDetails.battingSkill - 50) / 100;
            const aggressionWicketPenalty = Math.max(1.0, aggressionFactor - riskMitigation);
            
            const baseWicketProb = batterProfile.avg > 0 ? (expectedRunsPerBall / aggressionFactor / batterProfile.avg) * aggressionWicketPenalty : 0.05;
            
            let wicketProbability = (baseWicketProb * pressureFactor)
                + (((bowlerDetails.secondarySkill * bowlerFatigue) - (onStrikeBatterDetails.battingSkill * batterFatigue)) / 600) 
                + (bowlerDetails.role === PlayerRole.FAST_BOWLER ? pitchMods.paceBonus / 2 : 0) 
                + (bowlerDetails.role === PlayerRole.SPIN_BOWLER ? pitchMods.spinBonus / 2 : 0);
            
            wicketProbability *= formatMods.wicketChance;
            
            // Crisis Prevention: Drastically reduce wickets if score is extremely low
            if (!format.includes('First-Class')) {
                if (target) {
                    if (score < 50) wicketProbability *= 0.05;
                    else if (score < 120) wicketProbability *= 0.35;
                } else {
                    if (score < 40) wicketProbability *= 0.1;
                    else if (score < 80) wicketProbability *= 0.4;
                    else if (balls < 60 && score < 100) wicketProbability *= 0.7;
                }
            }

            // Format specific adjustments for realism
            if (format.includes('First-Class')) {
                wicketProbability *= 0.8; // Longer games, fewer wickets per ball
            } else if (isT20) {
                wicketProbability *= 1.1; // More risks in T20
            }

            wicketProbability = Math.max(0.004, Math.min(0.4, wicketProbability));

            balls++;
            onStrikeBatter.balls++;
            currentBowler.ballsBowled++;

            let currentPhase: 'pp' | 'mo' | 'do' | null = null;
            if (isT20) {
                if (balls <= 36) currentPhase = 'pp';
                else if (balls <= 96) currentPhase = 'mo';
                else currentPhase = 'do';
            } else if (isODI) {
                if (balls <= 60) currentPhase = 'pp';
                else if (balls <= 240) currentPhase = 'mo';
                else currentPhase = 'do';
            }

            if (currentPhase) {
                if (currentPhase === 'pp') {
                    onStrikeBatter.ppBalls = (onStrikeBatter.ppBalls || 0) + 1;
                    currentBowler.ppBallsBowled = (currentBowler.ppBallsBowled || 0) + 1;
                } else if (currentPhase === 'mo') {
                    onStrikeBatter.moBalls = (onStrikeBatter.moBalls || 0) + 1;
                    currentBowler.moBallsBowled = (currentBowler.moBallsBowled || 0) + 1;
                } else if (currentPhase === 'do') {
                    onStrikeBatter.doBalls = (onStrikeBatter.doBalls || 0) + 1;
                    currentBowler.doBallsBowled = (currentBowler.doBallsBowled || 0) + 1;
                }
            }

                let runsScored = 0;
                let isWicket = false;
                
                if (Math.random() < wicketProbability) {
                    isWicket = true;
                } else {
                    const rand = Math.random();
                    // Dynamic Probabilities based on skills & conditions
                    let p_dot=0.32, p_1=0.40, p_2=0.08, p_3=0.02, p_4=0.12, p_6=0.06;
                    
                    if (format.includes('First-Class')) {
                        p_dot = 0.70; p_1 = 0.22; p_2 = 0.03; p_3 = 0.01; p_4 = 0.04; p_6 = 0.00;
                    } else if (isT20) {
                        p_dot = 0.30; p_1 = 0.40; p_2 = 0.08; p_3 = 0.02; p_4 = 0.12; p_6 = 0.08;
                    } else if (isODI) {
                        p_dot = 0.35; p_1 = 0.42; p_2 = 0.09; p_3 = 0.02; p_4 = 0.08; p_6 = 0.04;
                    }

                    const skillDiff = (onStrikeBatterDetails.battingSkill * batterFatigue - bowlerDetails.secondarySkill * bowlerFatigue) / 100;
                    p_dot -= skillDiff * 0.1;
                    p_4 += skillDiff * 0.05;
                    p_6 += skillDiff * 0.03;

                    if (!target && !format.includes('First-Class') && score < 70) {
                        p_dot *= 0.6; p_4 *= 1.5; p_6 *= 2.0;
                    }

                    if (aggressionFactor > 1.2) { p_dot *= 0.8; p_4 *= 1.4; p_6 *= 1.6; }
                    else if (aggressionFactor < 0.9) { p_dot *= 1.4; p_4 *= 0.7; p_6 *= 0.5; }

                    const totalP = p_dot + p_1 + p_2 + p_3 + p_4 + p_6;
                    const normRand = rand * totalP;

                    if (normRand < p_dot) runsScored = 0;
                    else if (normRand < p_dot + p_1) runsScored = 1;
                    else if (normRand < p_dot + p_1 + p_2) runsScored = 2;
                    else if (normRand < p_dot + p_1 + p_2 + p_3) runsScored = 3;
                    else if (normRand < p_dot + p_1 + p_2 + p_3 + p_4) runsScored = 4;
                    else runsScored = 6;
                    
                    // Chaos Factor
                    if (runsScored === 0 && Math.random() < 0.02 && !format.includes('First-Class')) runsScored = 1;
                }

                if (isWicket) {
                    wickets++;
                    onStrikeBatter.isOut = true;
                    onStrikeBatter.dismissal = { type: 'bowled', bowlerId: currentBowler.playerId };
                    onStrikeBatter.dismissalText = `b ${currentBowler.playerName}`;
                    currentBowler.wickets++;

                    if (currentPhase) {
                        if (currentPhase === 'pp') {
                            onStrikeBatter.ppDismissals = (onStrikeBatter.ppDismissals || 0) + 1;
                            currentBowler.ppWickets = (currentBowler.ppWickets || 0) + 1;
                        } else if (currentPhase === 'mo') {
                            onStrikeBatter.moDismissals = (onStrikeBatter.moDismissals || 0) + 1;
                            currentBowler.moWickets = (currentBowler.moWickets || 0) + 1;
                        } else if (currentPhase === 'do') {
                            onStrikeBatter.doDismissals = (onStrikeBatter.doDismissals || 0) + 1;
                            currentBowler.doWickets = (currentBowler.doWickets || 0) + 1;
                        }
                    }

                    onStrikeBatterIndex = Math.max(onStrikeBatterIndex, offStrikeBatterIndex) + 1;
                } else {
                    const oldRuns = onStrikeBatter.runs;
                    onStrikeBatter.runs += runsScored;

                    if (oldRuns < 50 && onStrikeBatter.runs >= 50 && !onStrikeBatter.ballsToFifty) { onStrikeBatter.ballsToFifty = onStrikeBatter.balls; }
                    if (oldRuns < 100 && onStrikeBatter.runs >= 100 && !onStrikeBatter.ballsToHundred) { onStrikeBatter.ballsToHundred = onStrikeBatter.balls; }

                    score += runsScored;
                    currentBowler.runsConceded += runsScored;
                    runsThisOver += runsScored;
                    if (runsScored === 4) onStrikeBatter.fours++;
                    if (runsScored === 6) onStrikeBatter.sixes++;

                    if (currentPhase) {
                        if (currentPhase === 'pp') {
                            onStrikeBatter.ppRuns = (onStrikeBatter.ppRuns || 0) + runsScored;
                            currentBowler.ppRunsConceded = (currentBowler.ppRunsConceded || 0) + runsScored;
                        } else if (currentPhase === 'mo') {
                            onStrikeBatter.moRuns = (onStrikeBatter.moRuns || 0) + runsScored;
                            currentBowler.moRunsConceded = (currentBowler.moRunsConceded || 0) + runsScored;
                        } else if (currentPhase === 'do') {
                            onStrikeBatter.doRuns = (onStrikeBatter.doRuns || 0) + runsScored;
                            currentBowler.doRunsConceded = (currentBowler.doRunsConceded || 0) + runsScored;
                        }
                    }
                    
                    if (runsScored % 2 !== 0) { [onStrikeBatterIndex, offStrikeBatterIndex] = [offStrikeBatterIndex, onStrikeBatterIndex]; }
                }

            if (balls % 6 === 0) {
                if (runsThisOver === 0) currentBowler.maidens++;
                runsThisOver = 0;
                [onStrikeBatterIndex, offStrikeBatterIndex] = [offStrikeBatterIndex, onStrikeBatterIndex];
                
                const maxOversPerBowler = isT20 ? 4 : isODI ? 10 : Infinity;
                const lastBowlerIndex = bowlerIndex;
                
                // Smarter bowling rotation
                let bestNextBowlerIndex = -1;
                let bestScore = -Infinity;
                
                for (let i = 0; i < bowlingLineup.length; i++) {
                    if (i === lastBowlerIndex) continue; // Cannot bowl consecutive overs
                    if (bowlingLineup[i].ballsBowled >= maxOversPerBowler * 6) continue; // Max overs reached
                    
                    const b = bowlingLineup[i];
                    let bScore = b.skill;
                    
                    // Prefer strike bowlers if wickets are needed
                    if (wickets < 5) {
                        if (b.role === PlayerRole.FAST_BOWLER) bScore += 10;
                    } else {
                        if (b.role === PlayerRole.SPIN_BOWLER) bScore += 5;
                    }
                    
                    // Fatigue penalty
                    bScore -= (b.ballsBowled / 6) * 2;
                    
                    // Randomness
                    bScore += Math.random() * 10;
                    
                    if (bScore > bestScore) {
                        bestScore = bScore;
                        bestNextBowlerIndex = i;
                    }
                }
                
                if (bestNextBowlerIndex !== -1) {
                    bowlerIndex = bestNextBowlerIndex;
                } else {
                    // Fallback to simple rotation if no "best" found (shouldn't happen with 2+ bowlers)
                    bowlerIndex = (lastBowlerIndex + 1) % bowlingLineup.length;
                }
            }
        }

        return { 
            teamId: battingTeam.id, 
            teamName: battingTeam.name, 
            score, 
            wickets, 
            overs: formatOvers(balls), 
            extras, 
            batting: battingLineup.slice(0, Math.min(battingLineup.length, wickets + 2)), 
            bowling: bowlingLineup.map(b => ({...b, overs: formatOvers(b.ballsBowled)})) 
        };
    }, [gameData.scoreLimits]);

    const runLimitedOversMatchSimulation = useCallback((match: Match, teamAPlayers: Player[], teamBPlayers: Player[], gameData: GameData): MatchResult => {
        const allPlayersInMatch = [...teamAPlayers, ...teamBPlayers]; 
        const teamAData = gameData.teams.find(t => t.name === match.teamA); 
        const teamBData = gameData.teams.find(t => t.name === match.teamB);
        if(!teamAData || !teamBData) throw new Error(`Could not find team data for match: ${match.teamA} vs ${match.teamB}`);
        
        const teamA = { ...teamAData, squad: teamAPlayers }; 
        const teamB = { ...teamBData, squad: teamBPlayers };

        const homeGround = gameData.grounds.find(g => g.code === gameData.allTeamsData.find(t => t.name === match.teamA)?.homeGround); 
        const pitch = homeGround?.pitch || "Balanced Sporting Pitch";
        const groundCode = homeGround?.code || "KCG";

        // Generate form for all players in the match (0.9 to 1.1)
        const playerForms: Record<string, number> = {};
        allPlayersInMatch.forEach(p => {
            playerForms[p.id] = 0.9 + (Math.random() * 0.2);
        });

        const firstInning = simulateInning(teamA, teamB, gameData.currentFormat, null, pitch, groundCode, 1, allPlayersInMatch, playerForms);
        const secondInning = simulateInning(teamB, teamA, gameData.currentFormat, firstInning.score, pitch, groundCode, 2, allPlayersInMatch, playerForms);

        let winnerId: string | null = null, loserId: string | null = null, summary = '';

        if (secondInning.score > firstInning.score) {
            winnerId = teamB.id; loserId = teamA.id; summary = `${teamB.name} won by ${10 - secondInning.wickets} wickets.`;
        } else if (firstInning.score > secondInning.score) {
            winnerId = teamA.id; loserId = teamB.id; summary = `${teamA.name} won by ${firstInning.score - secondInning.score} runs.`;
        } else { 
            if (match.group !== 'Round-Robin') {
                const teamAIndex = gameData.standings[gameData.currentFormat].findIndex(s => s.teamId === teamA.id);
                const teamBIndex = gameData.standings[gameData.currentFormat].findIndex(s => s.teamId === teamB.id);
                if (teamAIndex < teamBIndex) {
                    winnerId = teamA.id; loserId = teamB.id; summary = `Match Tied (${teamA.name} won on higher league position)`;
                } else {
                    winnerId = teamB.id; loserId = teamA.id; summary = `Match Tied (${teamB.name} won on higher league position)`;
                }
            } else {
                summary = "Match Tied."; winnerId = null; loserId = null;
            }
        }

        let motm = { playerId: '', playerName: '', teamId: '', summary: '' }, bestScore = -1;
        for (const p of firstInning.batting) { const s = p.runs + (p.runs >= 50 ? 25 : 0) + (p.runs >= 100 ? 50 : 0); if (s > bestScore) { bestScore = s; motm = { playerId: p.playerId, playerName: p.playerName, teamId: teamA.id, summary: `${p.runs}(${p.balls})` }; } }
        for (const p of secondInning.batting) { const s = p.runs * 1.2 + (p.runs >= 50 ? 30 : 0) + (p.runs >= 100 ? 60 : 0); if (s > bestScore) { bestScore = s; motm = { playerId: p.playerId, playerName: p.playerName, teamId: teamB.id, summary: `${p.runs}(${p.balls})` }; } }
        for (const p of firstInning.bowling) { const s = p.wickets * 25 + (p.wickets >= 3 ? 25 : 0) + (p.wickets >= 5 ? 50 : 0) - p.runsConceded * 0.5; if (s > bestScore) { bestScore = s; motm = { playerId: p.playerId, playerName: p.playerName, teamId: teamB.id, summary: `${p.wickets}/${p.runsConceded}` }; } }
        for (const p of secondInning.bowling) { const s = p.wickets * 20 + (p.wickets >= 3 ? 20 : 0) + (p.wickets >= 5 ? 40 : 0) - p.runsConceded * 0.5; if (s > bestScore) { bestScore = s; motm = { playerId: p.playerId, playerName: p.playerName, teamId: teamA.id, summary: `${p.wickets}/${p.runsConceded}` }; } }

        return { matchNumber: match.matchNumber, winnerId, loserId, summary, firstInning, secondInning, manOfTheMatch: motm };
    }, [simulateInning]);

    const runFirstClassMatchSimulation = useCallback((match: Match, teamAPlayers: Player[], teamBPlayers: Player[], gameData: GameData): MatchResult => {
        const allPlayersInMatch = [...teamAPlayers, ...teamBPlayers]; 
        const teamAData = gameData.teams.find(t => t.name === match.teamA); 
        const teamBData = gameData.teams.find(t => t.name === match.teamB);
        if(!teamAData || !teamBData) throw new Error(`Could not find team data for match: ${match.teamA} vs ${match.teamB}`);
        
        const teamA = { ...teamAData, squad: teamAPlayers }; 
        const teamB = { ...teamBData, squad: teamBPlayers };

        const homeGround = gameData.grounds.find(g => g.code === gameData.allTeamsData.find(t => t.name === match.teamA)?.homeGround); 
        const pitch = homeGround?.pitch || "Balanced Sporting Pitch";
        const groundCode = homeGround?.code || "KCG";

        // Generate form for all players in the match (0.9 to 1.1)
        const playerForms: Record<string, number> = {};
        allPlayersInMatch.forEach(p => {
            playerForms[p.id] = 0.9 + (Math.random() * 0.2);
        });

        // First-Class Simulation: Multi-innings
        const firstInning = simulateInning(teamA, teamB, gameData.currentFormat, null, pitch, groundCode, 1, allPlayersInMatch, playerForms);
        const secondInning = simulateInning(teamB, teamA, gameData.currentFormat, null, pitch, groundCode, 2, allPlayersInMatch, playerForms);
        const thirdInning = simulateInning(teamA, teamB, gameData.currentFormat, null, pitch, groundCode, 3, allPlayersInMatch, playerForms);
        const fourthInning = simulateInning(teamB, teamA, gameData.currentFormat, (firstInning.score + thirdInning.score - secondInning.score), pitch, groundCode, 4, allPlayersInMatch, playerForms);

        let winnerId: string | null = null, loserId: string | null = null, isDraw = false, summary = '';
        const target = firstInning.score + thirdInning.score - secondInning.score + 1;
        
        if (fourthInning.score >= target) {
            winnerId = teamB.id; loserId = teamA.id; summary = `${teamB.name} won by ${10 - fourthInning.wickets} wickets.`;
        } else if (fourthInning.wickets >= 10) {
            winnerId = teamA.id; loserId = teamB.id; summary = `${teamA.name} won by ${target - 1 - fourthInning.score} runs.`;
        } else {
            isDraw = true; summary = 'Match Drawn.'; winnerId = null; loserId = null;
        }

        let motm = { playerId: '', playerName: '', teamId: '', summary: '' }, bestScore = -1;
        [firstInning, secondInning, thirdInning, fourthInning].forEach((inning, idx) => { 
            const btid = idx % 2 === 0 ? teamA.id : teamB.id; 
            const f_tid = idx % 2 === 0 ? teamB.id : teamA.id; 
            if (inning) {
                 for (const p of inning.batting) { const s = p.runs * 1.5 + (p.runs >= 50 ? 50 : 0) + (p.runs >= 100 ? 100 : 0); if (s > bestScore) { bestScore = s; motm = { playerId: p.playerId, playerName: p.playerName, teamId: btid, summary: `${p.runs}(${p.balls})` }; } } 
                 for (const p of inning.bowling) { const s = p.wickets * 30 + (p.wickets >= 3 ? 30 : 0) + (p.wickets >= 5 ? 75 : 0) - p.runsConceded * 0.5; if (s > bestScore) { bestScore = s; motm = { playerId: p.playerId, playerName: p.playerName, teamId: f_tid, summary: `${p.wickets}/${p.runsConceded}` }; } } 
            }
        });

        return { matchNumber: match.matchNumber, winnerId, loserId, isDraw, summary, firstInning, secondInning, thirdInning, fourthInning, manOfTheMatch: motm };
    }, [simulateInning]);

    const runSimulationForCurrentFormat = useCallback((match: Match, gameData: GameData) => {
        const teamAData = gameData.teams.find(t => t.name === match.teamA); 
        const teamBData = gameData.teams.find(t => t.name === match.teamB);
        if (!teamAData || !teamBData) throw new Error(`Team data not found for match: ${match.teamA} vs ${match.teamB}`);

        const getPlayingXI = (team: Team) => {
            const customXI = gameData.playingXIs[team.id]?.[gameData.currentFormat];
            if (customXI && customXI.length === 11) {
                const xiPlayers = customXI.map(id => team.squad.find(p => p.id === id)).filter(Boolean) as Player[];
                if (xiPlayers.length === 11) return xiPlayers;
            }
            return generateAutoXI(team.squad, gameData.currentFormat);
        };

        const teamAPlayers = getPlayingXI(teamAData); 
        const teamBPlayers = getPlayingXI(teamBData);

        return (gameData.currentFormat.includes('First-Class')) 
            ? runFirstClassMatchSimulation(match, teamAPlayers, teamBPlayers, gameData) 
            : runLimitedOversMatchSimulation(match, teamAPlayers, teamBPlayers, gameData);
    }, [runLimitedOversMatchSimulation, runFirstClassMatchSimulation]);

    const updateStatsFromMatch = useCallback((result: MatchResult, format: Format, gameData: GameData): GameData => {
        const newGameData = JSON.parse(JSON.stringify(gameData)) as GameData;
        const allInnings = [result.firstInning, result.secondInning, result.thirdInning, result.fourthInning].filter(Boolean) as Inning[];

        const isT20OrODI = format.includes('T20') || format.includes('One-Day') || format.includes('List-A');

        for (const inning of allInnings) {
            for (const batPerf of inning.batting) { 
                const player = newGameData.allPlayers.find(p => p.id === batPerf.playerId); 
                if (!player) continue; 
                if (!player.stats[format]) player.stats[format] = generateSingleFormatInitialStats();
                const stats = player.stats[format]; 
                stats.matches += (inning === result.firstInning || inning === result.secondInning ? 1 : 0); 
                stats.runs += batPerf.runs; 
                stats.ballsFaced += batPerf.balls; 
                if (batPerf.isOut) stats.dismissals++; 
                if (batPerf.runs > stats.highestScore) stats.highestScore = batPerf.runs; 
                if (batPerf.runs >= 100) stats.hundreds++; 
                else if (batPerf.runs >= 50) stats.fifties++; 
                else if (batPerf.runs >= 30) stats.thirties++;
                stats.fours += batPerf.fours; 
                stats.sixes += batPerf.sixes; 
                stats.average = stats.dismissals > 0 ? stats.runs / stats.dismissals : stats.runs; 
                stats.strikeRate = stats.ballsFaced > 0 ? (stats.runs / stats.ballsFaced) * 100 : 0; 

                if (batPerf.ballsToFifty && batPerf.ballsToFifty > 0) {
                    if (stats.fastestFifty === 0 || batPerf.ballsToFifty < stats.fastestFifty) {
                        stats.fastestFifty = batPerf.ballsToFifty;
                    }
                }
                if (batPerf.ballsToHundred && batPerf.ballsToHundred > 0) {
                    if (stats.fastestHundred === 0 || batPerf.ballsToHundred < stats.fastestHundred) {
                        stats.fastestHundred = batPerf.ballsToHundred;
                    }
                } 

                // Ensure phaseStats is initialized safely
                if (!stats.phaseStats) {
                    stats.phaseStats = {
                        batting: {
                            pp: { runs: 0, balls: 0, dismissals: 0 },
                            mo: { runs: 0, balls: 0, dismissals: 0 },
                            do: { runs: 0, balls: 0, dismissals: 0 }
                        },
                        bowling: {
                            pp: { wickets: 0, runsConceded: 0, ballsBowled: 0 },
                            mo: { wickets: 0, runsConceded: 0, ballsBowled: 0 },
                            do: { wickets: 0, runsConceded: 0, ballsBowled: 0 }
                        }
                    };
                }

                if (isT20OrODI) {
                    stats.phaseStats.batting.pp.runs += (batPerf.ppRuns || 0);
                    stats.phaseStats.batting.pp.balls += (batPerf.ppBalls || 0);
                    stats.phaseStats.batting.pp.dismissals += (batPerf.ppDismissals || 0);

                    stats.phaseStats.batting.mo.runs += (batPerf.moRuns || 0);
                    stats.phaseStats.batting.mo.balls += (batPerf.moBalls || 0);
                    stats.phaseStats.batting.mo.dismissals += (batPerf.moDismissals || 0);

                    stats.phaseStats.batting.do.runs += (batPerf.doRuns || 0);
                    stats.phaseStats.batting.do.balls += (batPerf.doBalls || 0);
                    stats.phaseStats.batting.do.dismissals += (batPerf.doDismissals || 0);
                }

                // Ensure positionStats is initialized safely
                if (!stats.positionStats) {
                    stats.positionStats = {};
                    for (let pos = 1; pos <= 11; pos++) {
                        stats.positionStats[pos] = { innings: 0, runs: 0, balls: 0, dismissals: 0, thirties: 0, fifties: 0, hundreds: 0 };
                    }
                }

                const pos = batPerf.battingPosition || 1;
                if (!stats.positionStats[pos]) {
                    stats.positionStats[pos] = { innings: 0, runs: 0, balls: 0, dismissals: 0, thirties: 0, fifties: 0, hundreds: 0 };
                }
                stats.positionStats[pos].innings += 1;
                stats.positionStats[pos].runs += batPerf.runs;
                stats.positionStats[pos].balls += batPerf.balls;
                if (batPerf.isOut) stats.positionStats[pos].dismissals += 1;
                if (batPerf.runs >= 100) stats.positionStats[pos].hundreds += 1;
                else if (batPerf.runs >= 50) stats.positionStats[pos].fifties += 1;
                else if (batPerf.runs >= 30) stats.positionStats[pos].thirties += 1;
            }

            for (const bowlPerf of inning.bowling) { 
                const player = newGameData.allPlayers.find(p => p.id === bowlPerf.playerId); 
                if (!player) continue; 
                if (!player.stats[format]) player.stats[format] = generateSingleFormatInitialStats();
                const stats = player.stats[format]; 
                stats.wickets += bowlPerf.wickets; 
                stats.runsConceded += bowlPerf.runsConceded; 
                stats.ballsBowled += bowlPerf.ballsBowled;
                stats.bowlingAverage = stats.wickets > 0 ? stats.runsConceded / stats.wickets : stats.runsConceded; 
                stats.economy = stats.ballsBowled > 0 ? (stats.runsConceded / stats.ballsBowled) * 6 : 0; 
                if (bowlPerf.wickets > stats.bestBowlingWickets || (bowlPerf.wickets === stats.bestBowlingWickets && bowlPerf.runsConceded < stats.bestBowlingRuns)) { 
                    stats.bestBowlingWickets = bowlPerf.wickets; 
                    stats.bestBowlingRuns = bowlPerf.runsConceded; 
                    stats.bestBowling = `${bowlPerf.wickets}/${bowlPerf.runsConceded}`; 
                } 
                if (bowlPerf.wickets >= 5) stats.fiveWicketHauls++; 
                else if (bowlPerf.wickets >= 3) stats.threeWicketHauls++; 

                // Ensure phaseStats is initialized safely
                if (!stats.phaseStats) {
                    stats.phaseStats = {
                        batting: {
                            pp: { runs: 0, balls: 0, dismissals: 0 },
                            mo: { runs: 0, balls: 0, dismissals: 0 },
                            do: { runs: 0, balls: 0, dismissals: 0 }
                        },
                        bowling: {
                            pp: { wickets: 0, runsConceded: 0, ballsBowled: 0 },
                            mo: { wickets: 0, runsConceded: 0, ballsBowled: 0 },
                            do: { wickets: 0, runsConceded: 0, ballsBowled: 0 }
                        }
                    };
                }

                if (isT20OrODI) {
                    stats.phaseStats.bowling.pp.wickets += (bowlPerf.ppWickets || 0);
                    stats.phaseStats.bowling.pp.runsConceded += (bowlPerf.ppRunsConceded || 0);
                    stats.phaseStats.bowling.pp.ballsBowled += (bowlPerf.ppBallsBowled || 0);

                    stats.phaseStats.bowling.mo.wickets += (bowlPerf.moWickets || 0);
                    stats.phaseStats.bowling.mo.runsConceded += (bowlPerf.moRunsConceded || 0);
                    stats.phaseStats.bowling.mo.ballsBowled += (bowlPerf.moBallsBowled || 0);

                    stats.phaseStats.bowling.do.wickets += (bowlPerf.doWickets || 0);
                    stats.phaseStats.bowling.do.runsConceded += (bowlPerf.doRunsConceded || 0);
                    stats.phaseStats.bowling.do.ballsBowled += (bowlPerf.doBallsBowled || 0);
                }
            }
        }

        const motmPlayer = newGameData.allPlayers.find(p => p.id === result.manOfTheMatch.playerId); 
        if (motmPlayer) { 
             if (!motmPlayer.stats[format]) motmPlayer.stats[format] = generateSingleFormatInitialStats();
            motmPlayer.stats[format].manOfTheMatchAwards++; 
        }

        newGameData.standings[format].forEach(s => {
            if (s.teamId === result.firstInning.teamId) {
                s.played++;
                if (result.winnerId === s.teamId) s.won++, s.points += format.includes('First-Class') ? 4 : 2;
                else if (!result.winnerId) s.points += 1; // Tie/Draw
                else s.lost++;
            } else if (s.teamId === result.secondInning?.teamId) {
                s.played++;
                if (result.winnerId === s.teamId) s.won++, s.points += format.includes('First-Class') ? 4 : 2;
                else if (!result.winnerId) s.points += 1; // Tie/Draw
                else s.lost++;
            }
        });

        newGameData.standings[format].sort((a, b) => b.points - a.points || b.netRunRate - a.netRunRate); 
        newGameData.matchResults[format].push(result); 

        // Update Team vs Team Records
        if (result.winnerId && result.loserId) {
            const teamAId = result.winnerId;
            const teamBId = result.loserId;
            let h2h = newGameData.records.teamVsTeam.find(r => 
                (r.teamAId === teamAId && r.teamBId === teamBId) || 
                (r.teamAId === teamBId && r.teamBId === teamAId)
            );
            if (!h2h) {
                const teamA = newGameData.teams.find(t => t.id === teamAId);
                const teamB = newGameData.teams.find(t => t.id === teamBId);
                h2h = {
                    teamAId, teamBId, 
                    teamAName: teamA?.name || 'Team A', 
                    teamBName: teamB?.name || 'Team B',
                    matches: 0, teamAWins: 0
                };
                newGameData.records.teamVsTeam.push(h2h);
            }
            h2h.matches += 1;
            if (result.winnerId === h2h.teamAId) h2h.teamAWins += 1;
        }

        // --- NEW: Update Player vs Team Records ---
        for (const inning of allInnings) {
            const oppTeamId = inning.teamId === result.firstInning.teamId ? (result.secondInning?.teamId || '') : result.firstInning.teamId;
            const oppTeam = newGameData.teams.find(t => t.id === oppTeamId);
            
            if (oppTeam) {
                // Update Batting vs Team
                for (const b of inning.batting) {
                    if (!newGameData.records.playerVsTeam[b.playerId]) newGameData.records.playerVsTeam[b.playerId] = {};
                    if (!newGameData.records.playerVsTeam[b.playerId][oppTeam.id]) {
                        newGameData.records.playerVsTeam[b.playerId][oppTeam.id] = { runs: 0, balls: 0, dismissals: 0, wickets: 0, runsConceded: 0, ballsBowled: 0 };
                    }
                    const r = newGameData.records.playerVsTeam[b.playerId][oppTeam.id];
                    r.runs += b.runs;
                    r.balls += b.balls;
                    if (b.isOut) r.dismissals += 1;
                }
                // Update Bowling vs Team
                for (const b of inning.bowling) {
                    if (!newGameData.records.playerVsTeam[b.playerId]) newGameData.records.playerVsTeam[b.playerId] = {};
                    if (!newGameData.records.playerVsTeam[b.playerId][oppTeam.id]) {
                        newGameData.records.playerVsTeam[b.playerId][oppTeam.id] = { runs: 0, balls: 0, dismissals: 0, wickets: 0, runsConceded: 0, ballsBowled: 0 };
                    }
                    const r = newGameData.records.playerVsTeam[b.playerId][oppTeam.id];
                    r.wickets += b.wickets;
                    r.runsConceded += b.runsConceded;
                    r.ballsBowled += b.ballsBowled;
                }
            }
        }

        // 1. Decrement match-based injuries
        newGameData.allPlayers.forEach(p => {
            if (p.injury && p.injury.durationType === 'matches') {
                p.injury.durationValue -= 1;
                if (p.injury.durationValue <= 0) {
                    p.injury = null;
                }
            }
        });

        // 2. Simulate fresh injuries (1.5% chance per match split among active participants)
        const activePlayerIds = new Set<string>();
        for (const inning of allInnings) {
            inning.batting.forEach(b => activePlayerIds.add(b.playerId));
            inning.bowling.forEach(b => activePlayerIds.add(b.playerId));
        }
        if (activePlayerIds.size > 0 && Math.random() < 0.015) {
            const injuredId = Array.from(activePlayerIds)[Math.floor(Math.random() * activePlayerIds.size)];
            const injuredPlayer = newGameData.allPlayers.find(p => p.id === injuredId);
            if (injuredPlayer && !injuredPlayer.injury) {
                const injuryTemplates = [
                    { text: "Sprained Hamstring", type: "matches" as const, min: 1, max: 2 },
                    { text: "Torn Calf Muscle", type: "matches" as const, min: 3, max: 6 },
                    { text: "Broken Finger", type: "matches" as const, min: 2, max: 4 },
                    { text: "Side Strain", type: "matches" as const, min: 1, max: 3 },
                    { text: "Shoulder Dislocation", type: "seasons" as const, min: 1, max: 2 },
                    { text: "Ankle Ligament Tear", type: "seasons" as const, min: 1, max: 1 },
                ];
                const selectedTemplate = injuryTemplates[Math.floor(Math.random() * injuryTemplates.length)];
                const val = Math.floor(Math.random() * (selectedTemplate.max - selectedTemplate.min + 1)) + selectedTemplate.min;
                injuredPlayer.injury = {
                    durationType: selectedTemplate.type,
                    durationValue: val,
                    text: selectedTemplate.text
                };
            }
        }

        // 3. Synchronize all nested squads
        newGameData.teams.forEach(t => {
            t.squad = t.squad.map(sqPlayer => {
                const refreshed = newGameData.allPlayers.find(p => p.id === sqPlayer.id);
                return refreshed ? JSON.parse(JSON.stringify(refreshed)) : sqPlayer;
            });
        });

        return newGameData;
    }, []);

    return { runSimulationForCurrentFormat, updateStatsFromMatch };
}