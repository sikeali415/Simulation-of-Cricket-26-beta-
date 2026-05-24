
import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { Home, Trophy, BarChart3, Settings as SettingsIcon, Newspaper, Users, Database, LayoutGrid, ArrowRightLeft, Scale, Wallet, Gavel } from 'lucide-react';
import { GameData, CareerScreen, MatchResult, Player, Format, PromotionRecord, Team, LiveMatchState, NewsArticle, PlayerRole } from '../types';
import { TEAMS, INITIAL_SPONSORSHIPS, INITIAL_NEWS } from '../data';
import { Icons } from './Icons';
import { getPlayerById, generateLeagueSchedule, negotiateSponsorships, generateMatchNews, generatePreMatchNews, getPlayerBasePrice, getPlayerMarketPrice } from '../utils';
import { useSimulation } from '../hooks/useSimulation';

// Components
import Dashboard from './Dashboard';
import Schedule from './Schedule';
import News from './News';
import Lineups from './Lineups';
import Editor from './Editor';
import Standings from './Standings';
import Stats from './Stats';
import Settings from './Settings';
import PlayerProfile from './PlayerProfile';
import MatchResultScreen from './MatchResultScreen';
import ForwardResultsScreen from './ForwardResultsScreen';
import AwardsAndRecordsScreen from './AwardsRecordsScreen';
import EndOfFormatScreen from './EndOfFormatScreen';
import Transfers from './Transfers';
import ComparisonScreen from './ComparisonScreen';
import LiveMatchScreen from './LiveMatchScreen';
import SponsorRoom from './SponsorRoom';
import AuctionRoom from './AuctionRoom';
import PlayerDatabase from './PlayerDatabase';

import { useFirebase } from './FirebaseProvider';
import { signIn, signOutUser } from '../services/firebase';

interface CareerHubProps {
    gameData: GameData;
    setGameData: React.Dispatch<React.SetStateAction<GameData | null>>;
    onResetGame: () => void;
    theme: 'light' | 'dark';
    setTheme: (theme: 'light' | 'dark') => void;
    saveGame: () => void;
    loadGame: () => void;
    showFeedback: (message: string, type?: 'success' | 'error') => void;
}

const BottomNavBar = ({ activeScreen, setScreen }: { activeScreen: CareerScreen, setScreen: (screen: CareerScreen) => void }) => {
    const navItems = [
        { name: 'HOME', screen: 'DASHBOARD' as CareerScreen, icon: Home },
        { name: 'STANDINGS', screen: 'LEAGUES' as CareerScreen, icon: Trophy },
        { name: 'STATS', screen: 'STATS' as CareerScreen, icon: BarChart3 },
        { name: 'SETTINGS', screen: 'SETTINGS' as CareerScreen, icon: SettingsIcon },
    ];
    return (
        <nav className="bg-white/80 dark:bg-[#0A0F0F]/90 border-t border-slate-200 dark:border-slate-800/50 flex justify-around items-center h-[80px] pb-4 backdrop-blur-xl sticky bottom-0 z-50">
            {navItems.map(item => {
                const isActive = activeScreen === item.screen;
                return (
                    <button
                        key={item.name}
                        onClick={() => setScreen(item.screen)}
                        className={`relative flex flex-col items-center justify-center space-y-1 w-1/4 pt-2 transition-all duration-300 ${isActive ? 'text-teal-500' : 'text-slate-400 hover:text-slate-600 dark:hover:text-slate-200'}`}
                    >
                        {isActive && (
                            <motion.div 
                                layoutId="nav-active"
                                className="absolute -top-2 w-12 h-1 bg-teal-500 rounded-full"
                                transition={{ type: 'spring', stiffness: 380, damping: 30 }}
                            />
                        )}
                        <item.icon size={20} strokeWidth={isActive ? 2.5 : 2} />
                        <span className="text-[9px] font-black tracking-[0.15em] uppercase">{item.name}</span>
                    </button>
                );
            })}
        </nav>
    );
};

const CareerHub: React.FC<CareerHubProps> = ({ gameData, setGameData, onResetGame, theme, setTheme, saveGame, loadGame, showFeedback }) => {
    const [screen, setScreen] = useState<CareerScreen>('DASHBOARD');
    const [selectedPlayer, setSelectedPlayer] = useState<Player | null>(null);
    const [playerProfileFormat, setPlayerProfileFormat] = useState<Format>(gameData.currentFormat);
    const [selectedMatchResult, setSelectedMatchResult] = useState<MatchResult | null>(null);
    const [forwardSimResults, setForwardSimResults] = useState<MatchResult[]>([]);

    const { runSimulationForCurrentFormat, updateStatsFromMatch } = useSimulation(gameData, setGameData);

    const optimizeAllSquads = () => {
        if (!gameData) return;
        setGameData(prev => {
            if (!prev) return null;
            const format = prev.currentFormat;
            const finalPlayingXIs = { ...prev.playingXIs };
            
            const updatedTeams = prev.teams.map(t => {
                const squad = [...t.squad].filter(p => !p.injury);
                const selectedIds = new Set<string>();

                // Helper to add best available by specific role
                const addMandatory = (roles: PlayerRole[], count: number) => {
                    const available = squad.filter(p => !selectedIds.has(p.id) && roles.includes(p.role));
                    available.sort((a,b) => (b.battingSkill + b.secondarySkill) - (a.battingSkill + a.secondarySkill));
                    available.slice(0, count).forEach(p => selectedIds.add(p.id));
                };

                // 1. Mandatory Minimums (User Rule)
                addMandatory([PlayerRole.WICKET_KEEPER], 1);
                addMandatory([PlayerRole.SPIN_BOWLER], 1);
                addMandatory([PlayerRole.FAST_BOWLER], 2);
                addMandatory([PlayerRole.ALL_ROUNDER], 1);
                addMandatory([PlayerRole.BATSMAN], 4);

                // 2. Fill to 11 ensuring 7-8 Batters/ARs and 5+ Bowling options
                while (selectedIds.size < 11) {
                    const currentXI = Array.from(selectedIds).map(id => squad.find(p => p.id === id)!).filter(Boolean);
                    const batARCount = currentXI.filter(p => p.role === PlayerRole.BATSMAN || p.role === PlayerRole.ALL_ROUNDER).length;
                    const bowlCount = currentXI.filter(p => p.role === PlayerRole.FAST_BOWLER || p.role === PlayerRole.SPIN_BOWLER || p.role === PlayerRole.ALL_ROUNDER).length;

                    const remaining = squad
                        .filter(p => !selectedIds.has(p.id))
                        .sort((a,b) => {
                            // Prioritize reaching user thresholds
                            if (batARCount < 7) {
                                const isABat = a.role === PlayerRole.BATSMAN || a.role === PlayerRole.ALL_ROUNDER;
                                const isBBat = b.role === PlayerRole.BATSMAN || b.role === PlayerRole.ALL_ROUNDER;
                                if (isABat && !isBBat) return -1;
                                if (!isABat && isBBat) return 1;
                            }
                            if (bowlCount < 5) {
                                const isABowl = a.role === PlayerRole.FAST_BOWLER || a.role === PlayerRole.SPIN_BOWLER || a.role === PlayerRole.ALL_ROUNDER;
                                const isBBowl = b.role === PlayerRole.FAST_BOWLER || b.role === PlayerRole.SPIN_BOWLER || b.role === PlayerRole.ALL_ROUNDER;
                                if (isABowl && !isBBowl) return -1;
                                if (!isABowl && isBBowl) return 1;
                            }
                            return (b.battingSkill + b.secondarySkill) - (a.battingSkill + a.secondarySkill);
                        });
                    
                    if (remaining.length === 0) break;
                    selectedIds.add(remaining[0].id);
                }

                // 3. Official Ordering (1-11)
                const finalPool = Array.from(selectedIds).map(id => squad.find(p => p.id === id)!).filter(Boolean);
                
                // User wants 1-7 as Opener/Batter/AR, and 8-11 as Bowlers
                // Logic: 
                // Group A: Best 7 batters (highest battingSkill) for 1-7
                // Group B: Best 4 bowlers (highest secondarySkill) from remaining for 8-11
                // Wait, if an AR is a top batter, they go in 1-7.
                
                // Pick bowlers for 8-11 first: 4 players with highest bowling skills, who are NOT the WK
                const nonWK = finalPool.filter(p => p.role !== PlayerRole.WICKET_KEEPER);
                const sortedByBowling = [...nonWK].sort((a,b) => b.secondarySkill - a.secondarySkill);
                const bowlers8to11 = sortedByBowling.slice(0, 4);
                
                const remainingForTop = finalPool.filter(p => !bowlers8to11.map(b => b.id).includes(p.id));
                const top7 = remainingForTop.sort((a,b) => b.battingSkill - a.battingSkill);

                // Re-sort bowlers to have best bowlers last (typical ordering)
                bowlers8to11.sort((a,b) => b.secondarySkill - a.secondarySkill); 

                const newXI = [...top7, ...bowlers8to11.reverse()].map(p => p.id);
                finalPlayingXIs[t.id] = { ...finalPlayingXIs[t.id], [format]: newXI };

                let cid = t.captainId;
                if (!cid || !newXI.includes(cid)) {
                    const currentXIPlayers = newXI.map(id => squad.find(p => p.id === id)).filter(Boolean) as Player[];
                    cid = currentXIPlayers.sort((a,b) => (b.battingSkill + b.secondarySkill) - (a.battingSkill + a.secondarySkill))[0]?.id || null;
                }

                return { ...t, captainId: cid };
            });

            return { ...prev, teams: updatedTeams, playingXIs: finalPlayingXIs };
        });
        showFeedback("Squads Optimized: 7-8 Batters/ARs & 5+ Bowling options prioritised.", "success");
    };

    useEffect(() => {
        if (gameData && (!gameData.sponsorships || !gameData.popularity || !gameData.news)) {
             setGameData(prev => {
                 if (!prev) return null;
                 return {
                     ...prev,
                     popularity: prev.popularity ?? 50,
                     sponsorships: prev.sponsorships ?? INITIAL_SPONSORSHIPS,
                     news: prev.news ?? INITIAL_NEWS
                 };
             });
        }
    }, [gameData, setGameData]);

    const userTeam = useMemo(() => {
        return gameData.teams.find(t => t.id === gameData.userTeamId) || gameData.teams[0];
    }, [gameData]);

    useEffect(() => {
        const schedule = gameData.schedule[gameData.currentFormat];
        const currentMatchIndex = gameData.currentMatchIndex[gameData.currentFormat];

        if (currentMatchIndex >= schedule.length) {
            const awardExists = gameData.awardsHistory.some(a => a.season === gameData.currentSeason && a.format === gameData.currentFormat);
            
            if (!awardExists) {
                const formatStats = new Map();
                gameData.teams.forEach(team => team.squad.forEach(player => {
                    const p = getPlayerById(player.id, gameData.allPlayers);
                    if (p) {
                       const fstats = p.stats[gameData.currentFormat];
                       const runs = fstats.runs || 0;
                       const wickets = fstats.wickets || 0;
                       const ballsFaced = fstats.ballsFaced || 0;
                       const strikeRate = ballsFaced > 0 ? (runs / ballsFaced) * 100 : 0;
                       const mvpPoints = runs * 1.0 + wickets * 22;

                       formatStats.set(p.id, { 
                           runs, 
                           wickets, 
                           ballsFaced,
                           strikeRate,
                           mvpPoints,
                           teamName: team.name, 
                           playerName: p.name 
                       });
                    }
                }));

                const sortedBatters = [...formatStats.entries()].sort((a, b) => b[1].runs - a[1].runs);
                const sortedBowlers = [...formatStats.entries()].sort((a, b) => b[1].wickets - a[1].wickets);
                const sortedMvps = [...formatStats.entries()].sort((a, b) => b[1].mvpPoints - a[1].mvpPoints);
                
                let filteredPowerHitters = [...formatStats.entries()].filter(entry => entry[1].runs >= 150);
                if (filteredPowerHitters.length === 0) {
                    filteredPowerHitters = [...formatStats.entries()].filter(entry => entry[1].runs >= 50);
                }
                if (filteredPowerHitters.length === 0) {
                    filteredPowerHitters = [...formatStats.entries()];
                }
                const sortedPowerHitters = filteredPowerHitters.sort((a, b) => b[1].strikeRate - a[1].strikeRate);

                const finalMatchNumber = schedule[schedule.length-1].matchNumber;
                const lastMatchResult = gameData.matchResults[gameData.currentFormat].find(r => r.matchNumber === finalMatchNumber);
                const winnerTeam = gameData.teams.find(t => t.id === lastMatchResult?.winnerId);

                const newAward = { 
                    season: gameData.currentSeason, 
                    format: gameData.currentFormat, 
                    winnerTeamId: winnerTeam?.id || '', 
                    winnerTeamName: winnerTeam?.name || 'N/A', 
                    bestBatter: { playerId: sortedBatters[0]?.[0] || '', playerName: sortedBatters[0]?.[1].playerName || 'N/A', teamName: sortedBatters[0]?.[1].teamName || 'N/A', runs: sortedBatters[0]?.[1].runs || 0 }, 
                    bestBowler: { playerId: sortedBowlers[0]?.[0] || '', playerName: sortedBowlers[0]?.[1].playerName || 'N/A', teamName: sortedBowlers[0]?.[1].teamName || 'N/A', wickets: sortedBowlers[0]?.[1].wickets || 0 },
                    mvp: { playerId: sortedMvps[0]?.[0] || '', playerName: sortedMvps[0]?.[1].playerName || 'N/A', teamName: sortedMvps[0]?.[1].teamName || 'N/A', points: Number((sortedMvps[0]?.[1].mvpPoints || 0).toFixed(1)) },
                    powerHitter: { playerId: sortedPowerHitters[0]?.[0] || '', playerName: sortedPowerHitters[0]?.[1].playerName || 'N/A', teamName: sortedPowerHitters[0]?.[1].teamName || 'N/A', strikeRate: Number((sortedPowerHitters[0]?.[1].strikeRate || 0).toFixed(1)) }
                };

                setGameData(prev => prev ? { ...prev, awardsHistory: [...prev.awardsHistory, newAward] } : null);
                setScreen('END_OF_FORMAT');
            }
        }
    }, [gameData.currentMatchIndex, gameData.currentFormat, gameData.currentSeason, gameData.awardsHistory, gameData.teams, gameData.allPlayers, gameData.matchResults, gameData.schedule, setGameData]);

    const handleUpdatePlayer = (updatedPlayer: Player) => {
        setGameData(prevData => {
            if (!prevData) return null;
            const newAllPlayers = prevData.allPlayers.map(p => p.id === updatedPlayer.id ? updatedPlayer : p);
            const newTeams = prevData.teams.map(team => ({
                ...team,
                squad: team.squad.map(squadPlayer => newAllPlayers.find(p => p.id === squadPlayer.id) || squadPlayer)
            }));
            return { ...prevData, allPlayers: newAllPlayers, teams: newTeams };
        });
    };

    const handleCreatePlayer = (newPlayer: Player) => {
        setGameData(prevData => {
            if (!prevData) return null;
            return { ...prevData, allPlayers: [...prevData.allPlayers, newPlayer] };
        });
    };

    const handleUpdateGround = (code: string, newPitch: string) => setGameData(prev => prev ? { ...prev, grounds: prev.grounds.map(g => g.code === code ? { ...g, pitch: newPitch } : g) } : null);
    
    const handleUpdateScoreLimits = (groundCode: string, format: Format, field: any, value: any, inning: number) => {
        setGameData(prev => {
            if (!prev) return null;
            const numValue = parseInt(value, 10);
            const newLimits: any = JSON.parse(JSON.stringify(prev.scoreLimits || {}));
            if (!newLimits[groundCode]) newLimits[groundCode] = {};
            if (!newLimits[groundCode][format]) newLimits[groundCode][format] = {};
            if (!newLimits[groundCode][format][inning]) newLimits[groundCode][format][inning] = {};
            
            if (value === '' || isNaN(numValue) || numValue <= 0) {
                delete newLimits[groundCode][format][inning][field];
            } else {
                newLimits[groundCode][format][inning][field] = numValue;
            }
            
            return { ...prev, scoreLimits: newLimits };
        });
    };

    const handleUpdateCaptain = useCallback((teamId: string, format: Format, playerId: string) => {
        setGameData(prevData => {
            if (!prevData) return null;
            return {
                ...prevData,
                teams: prevData.teams.map(t => {
                    if (t.id === teamId) {
                        return { ...t, captains: { ...t.captains, [format]: playerId } };
                    }
                    return t;
                })
            };
        });
        showFeedback("Captain updated!");
    }, [setGameData, showFeedback]);

    const handleUpdatePlayingXI = useCallback((teamId: string, format: Format, newXI: string[]) => {
        setGameData(prevData => {
            if (!prevData) return null;
            const teamXIs = prevData.playingXIs[teamId] || {};
            return {
                ...prevData,
                playingXIs: {
                    ...prevData.playingXIs,
                    [teamId]: {
                        ...teamXIs,
                        [format]: newXI
                    }
                }
            };
        });
    }, [setGameData]);

    const simulateBackgroundMatches = (currentData: GameData): GameData => {
        let updatedData = JSON.parse(JSON.stringify(currentData)) as GameData;
        Object.values(Format).forEach(f => {
            if (f === updatedData.currentFormat) return; 

            const schedule = updatedData.schedule[f];
            let mIdx = updatedData.currentMatchIndex[f];
            
            for (let i = 0; i < 8; i++) {
                if (mIdx < schedule.length) {
                    let match = JSON.parse(JSON.stringify(schedule[mIdx]));
                    
                    if (match.group !== 'Round-Robin') {
                        const standings = updatedData.standings[f];
                        const getTeamName = (pos: number) => standings[pos - 1]?.teamName;
                        const resolvePlaceholder = (placeholder: string) => {
                            if (['1st', '2nd', '3rd', '4th'].includes(placeholder)) return getTeamName(parseInt(placeholder[0]));
                            if (placeholder.startsWith('SF')) {
                                const sfRes = updatedData.matchResults[f].find(r => r.matchNumber === placeholder.split(' ')[0]);
                                return updatedData.teams.find(t => t.id === sfRes?.winnerId)?.name || 'TBD';
                            }
                            return placeholder;
                        };
                        match.teamA = resolvePlaceholder(match.teamA) || 'TBD';
                        match.teamB = resolvePlaceholder(match.teamB) || 'TBD';
                        if (match.teamA === 'TBD' || match.teamB === 'TBD') break;
                    }

                    const result = runSimulationForCurrentFormat(match, updatedData);
                    updatedData = updateStatsFromMatch(result, f, updatedData);
                    updatedData.currentMatchIndex[f]++;
                    mIdx++;
                }
            }
        });
        return updatedData;
    };

    const handleForwardDay = () => {
        if (!userTeam) return;
        let currentData = { ...gameData };
        let matchIndex = currentData.currentMatchIndex[currentData.currentFormat];
        let schedule = currentData.schedule[currentData.currentFormat];
        const results: MatchResult[] = [];
        const newNewsItems: NewsArticle[] = [];

        currentData = simulateBackgroundMatches(currentData);

        for(let i=0; i<5; i++) {
            if (matchIndex + i < schedule.length) {
                const m = schedule[matchIndex+i];
                if (m.teamA === userTeam.name || m.teamB === userTeam.name) {
                    const preNews = generatePreMatchNews(m, currentData);
                    newNewsItems.push(preNews);
                    break;
                }
            }
        }

        let simulatedCount = 0;
        const maxSimulations = 8;

        while (matchIndex < schedule.length && simulatedCount < maxSimulations) {
            let matchToSim = JSON.parse(JSON.stringify(schedule[matchIndex]));
            
            if (matchToSim.group !== 'Round-Robin') {
                const standings = currentData.standings[currentData.currentFormat];
                const getTeamName = (pos: number) => standings[pos - 1]?.teamName;
                const resolvePlaceholder = (placeholder: string) => {
                    if (['1st', '2nd', '3rd', '4th'].includes(placeholder)) {
                        const pos = parseInt(placeholder[0]);
                        return getTeamName(pos);
                    }
                    if (placeholder.startsWith('SF')) {
                        const sfMatchNumber = placeholder.split(' ')[0];
                        const sfResult = currentData.matchResults[currentData.currentFormat].find(r => r.matchNumber === sfMatchNumber);
                        const winner = currentData.teams.find(t => t.id === sfResult?.winnerId);
                        return winner?.name || null;
                    }
                    return placeholder;
                };
                matchToSim.teamA = resolvePlaceholder(matchToSim.teamA) || 'TBD';
                matchToSim.teamB = resolvePlaceholder(matchToSim.teamB) || 'TBD';
                
                if (matchToSim.teamA === 'TBD' || matchToSim.teamB === 'TBD') break; 
            }

            const isUserTeamMatch = matchToSim.teamA === userTeam.name || matchToSim.teamB === userTeam.name;
            if (isUserTeamMatch) break;

            const result = runSimulationForCurrentFormat(matchToSim, currentData);
            currentData = updateStatsFromMatch(result, currentData.currentFormat, currentData);
            currentData.currentMatchIndex[currentData.currentFormat]++; 
            results.push(result);
            simulatedCount++;
            
            if (matchToSim.group !== 'Round-Robin' || Math.random() < 0.3) {
                const sponsorship = currentData.sponsorships?.[currentData.currentFormat] || INITIAL_SPONSORSHIPS[currentData.currentFormat];
                newNewsItems.push(generateMatchNews(result, currentData.currentFormat, sponsorship));
            }
            
            matchIndex++;
        }

        if (newNewsItems.length > 0) currentData.news = [...newNewsItems, ...currentData.news].slice(0, 50);

        if (results.length > 0) {
            setForwardSimResults(results);
            setGameData(currentData); 
            setScreen('FORWARD_RESULTS');
        } else {
             if (matchIndex < schedule.length) {
                 if (newNewsItems.length > 0) {
                     setGameData(prev => prev ? { ...prev, news: [...newNewsItems, ...prev.news] } : null);
                 }
                 showFeedback("Match 1 or upcoming user match is next.", "success");
             } else {
                 showFeedback("Tournament matches completed.", "success");
             }
        }
    };

    const handlePlayMatch = () => {
        if (!userTeam) return;
        
        const schedule = gameData.schedule[gameData.currentFormat];
        const currentMatchIndex = gameData.currentMatchIndex[gameData.currentFormat];
        if (currentMatchIndex >= schedule.length) return;

        let matchToSim = JSON.parse(JSON.stringify(schedule[currentMatchIndex]));

        if (matchToSim.group !== 'Round-Robin') {
             const standings = gameData.standings[gameData.currentFormat];
             const getTeamName = (pos: number) => standings[pos - 1]?.teamName;
             const resolvePlaceholder = (placeholder: string) => {
                if (['1st', '2nd', '3rd', '4th'].includes(placeholder)) return getTeamName(parseInt(placeholder[0]));
                if (placeholder.startsWith('SF')) {
                    const sfMatchNumber = placeholder.split(' ')[0];
                    const sfResult = gameData.matchResults[gameData.currentFormat].find(r => r.matchNumber === sfMatchNumber);
                    return gameData.teams.find(t => t.id === sfResult?.winnerId)?.name || null;
                }
                return placeholder;
            };
            matchToSim.teamA = resolvePlaceholder(matchToSim.teamA) || 'TBD';
            matchToSim.teamB = resolvePlaceholder(matchToSim.teamB) || 'TBD';
        }

        if (matchToSim.teamA === 'TBD' || matchToSim.teamB === 'TBD') {
            showFeedback("Waiting for league stage to conclude.", "error");
            return;
        }

        const isUserTeamMatch = matchToSim.teamA === userTeam.name || matchToSim.teamB === userTeam.name;
        
        if (isUserTeamMatch) {
            // Validate lineup for injuries
            const playingXIIds = gameData.playingXIs[userTeam.id]?.[gameData.currentFormat] || [];
            const injuredPlayers = playingXIIds
                .map(id => userTeam.squad.find(p => p.id === id))
                .filter(p => p && p.injury);

            if (injuredPlayers.length > 0) {
                showFeedback(`Cannot start match! ${injuredPlayers[0]?.name} is injured. Replace them in Lineups.`, "error");
                setScreen('LINEUPS');
                return;
            }

            setScreen('LIVE_MATCH');
        } else {
             const result = runSimulationForCurrentFormat(matchToSim, gameData);
             const updatedData = updateStatsFromMatch(result, gameData.currentFormat, gameData);
             updatedData.currentMatchIndex[gameData.currentFormat]++;
             const sponsorship = updatedData.sponsorships?.[updatedData.currentFormat] || INITIAL_SPONSORSHIPS[updatedData.currentFormat];
             const newsItem = generateMatchNews(result, updatedData.currentFormat, sponsorship);
             updatedData.news = [newsItem, ...updatedData.news].slice(0, 50);
             setGameData(updatedData);
             setSelectedMatchResult(result);
             setScreen('MATCH_RESULT');
        }
    };

    const handleLiveMatchComplete = (result: MatchResult) => {
        const updatedData = updateStatsFromMatch(result, gameData.currentFormat, gameData);
        updatedData.currentMatchIndex[gameData.currentFormat]++;
        updatedData.activeMatch = null; 
        const sponsorship = updatedData.sponsorships?.[updatedData.currentFormat] || INITIAL_SPONSORSHIPS[updatedData.currentFormat];
        const newsItem = generateMatchNews(result, updatedData.currentFormat, sponsorship);
        updatedData.news = [newsItem, ...updatedData.news].slice(0, 50);
        setGameData(updatedData);
        setSelectedMatchResult(result);
        setScreen('MATCH_RESULT');
    };

    const handleLiveMatchExit = (stateToSave?: LiveMatchState) => {
        if (stateToSave) {
            setGameData(prev => prev ? { ...prev, activeMatch: stateToSave } : null);
            showFeedback("Match progress saved.", "success");
        } else setGameData(prev => prev ? { ...prev, activeMatch: null } : null);
        setScreen('DASHBOARD');
    }

    const handleFormatChange = useCallback((newFormat: Format) => {
        setGameData(prev => prev ? ({ ...prev, currentFormat: newFormat }) : null);
        setScreen('DASHBOARD');
    }, [setGameData]);

    const handleEndOfSeason = useCallback((retainedPlayers: Player[]) => {
        setGameData((prevData: GameData | null) => {
            if (!prevData) return null;
            
            // Retention price logic with performance adjustments
            const calculateRetentionPrice = (p: Player) => {
                const basePrice = p.basePrice || getPlayerBasePrice(p);
                const stats = p.stats[Format.T20];
                let multiplier = 4; // Default rule: Base Price x4
                
                if (stats && stats.matches >= 3) {
                    const batAvg = stats.average;
                    const bowlEcon = stats.economy;
                    
                    // Performance Increase
                    if (batAvg >= 35 || (stats.wickets >= 10 && bowlEcon <= 8.5)) {
                        multiplier = 5; // Performance bonus
                    }
                    // Performance Decrease
                    else if (batAvg < 15 && stats.wickets < 3) {
                        multiplier = 3; // Underperformer penalty
                    }
                }
                
                return Number((basePrice * multiplier).toFixed(2));
            };

            const isStruggling = (p: Player) => {
                const stats = p.stats[Format.T20];
                if (!stats || stats.matches < 3) return false;
                if (p.role === PlayerRole.BATSMAN || p.role === PlayerRole.WICKET_KEEPER) {
                    return stats.average < 18;
                }
                return stats.economy > 10.5;
            };

            const userCost = retainedPlayers.reduce((sum, p) => sum + calculateRetentionPrice(p), 0);

            // Get top 50 performers for priority
            const leagueBatters = [...prevData.allPlayers].sort((a,b) => (b.stats[Format.T20]?.runs || 0) - (a.stats[Format.T20]?.runs || 0));
            const top50Batters = new Set(leagueBatters.slice(0, 50).map(p => p.id));
            const leagueBowlers = [...prevData.allPlayers].sort((a,b) => (b.stats[Format.T20]?.wickets || 0) - (a.stats[Format.T20]?.wickets || 0));
            const top50Bowlers = new Set(leagueBowlers.slice(0, 50).map(p => p.id));

            const newTeams = prevData.teams.map(t => {
                let currentSquad: Player[] = [];
                let purseVal = 0;
                
                if (t.id === prevData.userTeamId) {
                    const validRetained = retainedPlayers.filter(p => !p.isForeign);
                    currentSquad = validRetained;
                    purseVal = Number((100.0 - userCost).toFixed(2));
                } else {
                    // Retention priority: Top 50 performers > Best of rest > No Foreigners > No Strugglers
                    const candidates = t.squad.filter(p => !p.isForeign && !isStruggling(p));
                    const performers = candidates.filter(p => top50Batters.has(p.id) || top50Bowlers.has(p.id));
                    
                    let aiRetained = [...performers];
                    if (aiRetained.length < 5) {
                        const others = candidates.filter(p => !performers.includes(p)).sort((a,b) => (b.battingSkill + b.secondarySkill) - (a.battingSkill + a.secondarySkill));
                        aiRetained = [...aiRetained, ...others.slice(0, 5 - aiRetained.length)];
                    }
                    
                    const aiCost = aiRetained.reduce((sum, p) => sum + calculateRetentionPrice(p), 0);
                    currentSquad = aiRetained;
                    purseVal = Number((100.0 - aiCost).toFixed(2));
                }

                const updatedSquad = currentSquad.map(p => {
                    const nextPrice = calculateRetentionPrice(p);
                    const newStats = { ...p.stats };
                    // Reset stats for new season but keep history? Usually we reset for the 'league' view.
                    // The core stats are format-based, maybe we keep them and add a 'career' log later.
                    // For now, let's just update the price.
                    
                    if (p.injury && p.injury.durationType === 'seasons') {
                        const nextDuration = p.injury.durationValue - 1;
                        if (nextDuration <= 0) {
                            return { ...p, injury: null, basePrice: nextPrice };
                        } else {
                            return { ...p, injury: { ...p.injury, durationValue: nextDuration }, basePrice: nextPrice };
                        }
                    }
                    return { ...p, basePrice: nextPrice };
                });

                return {
                    ...t,
                    squad: updatedSquad,
                    purse: purseVal,
                    firstAidKits: (t.firstAidKits || 0) + 1
                };
            });

            // Update all players prices and injuries
            const updatedAllPlayers = prevData.allPlayers.map(p => {
                const nextPrice = calculateRetentionPrice(p);
                let pUpdated = { ...p, basePrice: nextPrice };
                
                if (p.injury && p.injury.durationType === 'seasons') {
                    const nextDuration = p.injury.durationValue - 1;
                    if (nextDuration <= 0) {
                        pUpdated.injury = null;
                    } else {
                        pUpdated.injury = { ...p.injury, durationValue: nextDuration };
                    }
                }
                return pUpdated;
            });

            const initialStandings = (teams: Team[]) => teams.map(team => ({ teamId: team.id, teamName: team.name, played: 0, won: 0, lost: 0, drawn: 0, points: 0, netRunRate: 0, runsFor: 0, runsAgainst: 0 }));

            const seasonNews: NewsArticle = { 
                id: `s${prevData.currentSeason}-end`, 
                headline: `Season ${prevData.currentSeason+1} Draft Room Open!`, 
                date: new Date().toLocaleDateString(), 
                excerpt: "Teams reveal retained players & Medical replenish has arrived.", 
                content: "Windows for retention have closed. Every team has been supplied 1 emergency First Aid kit for the upcoming campaign.", 
                type: 'league' as const
            };

            return {
                ...prevData,
                currentSeason: prevData.currentSeason + 1,
                currentFormat: Format.T20,
                currentMatchIndex: { [Format.T20]: 0, [Format.ODI]: 0, [Format.SHIELD]: 0 } as Record<Format, number>,
                matchResults: { [Format.T20]: [], [Format.ODI]: [], [Format.SHIELD]: [] } as Record<Format, MatchResult[]>,
                standings: { 
                    [Format.T20]: initialStandings(newTeams), 
                    [Format.ODI]: initialStandings(newTeams), 
                    [Format.SHIELD]: initialStandings(newTeams) 
                },
                schedule: { 
                    [Format.T20]: generateLeagueSchedule(newTeams, Format.T20, true), 
                    [Format.ODI]: generateLeagueSchedule(newTeams, Format.ODI, true), 
                    [Format.SHIELD]: generateLeagueSchedule(newTeams, Format.SHIELD, true)
                },
                teams: newTeams,
                allPlayers: updatedAllPlayers,
                news: [seasonNews, ...prevData.news].slice(0, 50)
            };
        });
        setScreen('AUCTION_ROOM');
    }, [setGameData]);

    const { user } = useFirebase();
    const [authError, setAuthError] = useState<{ title: string; message: string; copyText?: string } | null>(null);

    const handleSignIn = async () => {
        showFeedback("Opening Sign-In...");
        try {
            const u = await signIn();
            if (u) {
                showFeedback(`Signed in as ${u.displayName || 'Manager'}!`);
            }
        } catch (err: any) {
            console.error("Auth error caught in CareerHub:", err);
            const code = err?.code || "";
            let title = "Sign-In Action Required";
            let message = "An error occurred during Google Sign-In. Please check your network and try again.";
            let copyText = undefined;

            if (code === "auth/unauthorized-domain" || err?.message?.includes("unauthorized-domain") || err?.message?.includes("auth/unauthorized-domain")) {
                title = "Domain Not Whitelisted";
                message = `To allow Google Sign-In, this site's domain must be authorized in your Firebase Project.\n\nOur environment relies on custom containers, which require adding the hostname below.\n\nInstructions:\n1. Open your Firebase Console.\n2. Go to Authentication -> Settings -> Authorized Domains.\n3. Add this specific hostname:\n   ${window.location.hostname}\n\nOnce authorized, try backing up or syncing your game!`;
                copyText = window.location.hostname;
            } else if (code === "auth/popup-blocked" || err?.message?.includes("popup-blocked")) {
                title = "Pop-up Blocked";
                message = "Your browser blocked the Google Sign-In window.\n\nPlease enable/allow pop-ups for this website in your browser settings and try again.";
            } else if (code === "auth/popup-closed-by-user" || err?.message?.includes("popup-closed-by-user")) {
                title = "Sign-In Cancelled";
                message = "The Google Sign-In popup was closed before completion. Please try again.";
            } else {
                message = `Error Details: ${err?.message || err}`;
            }

            setAuthError({ title, message, copyText });
            showFeedback("Sign-In Failed", "error");
        }
    };

    const renderScreen = () => {
        const commonProps = { gameData, userTeam, setGameData, setScreen, showFeedback, optimizeAllSquads };
        switch(screen) {
            case 'DASHBOARD': return <Dashboard {...commonProps} handlePlayMatch={handlePlayMatch} handleForwardDay={handleForwardDay} />;
            case 'LEAGUES': return <Standings gameData={gameData} />; 
            case 'SCHEDULE': return <Schedule gameData={gameData} userTeam={userTeam} viewMatchResult={result => { setSelectedMatchResult(result); setScreen('MATCH_RESULT'); }} />;
            case 'LINEUPS': return <Lineups {...commonProps} handleUpdatePlayingXI={handleUpdatePlayingXI} handleUpdateCaptain={handleUpdateCaptain} />;
            case 'EDITOR': return <Editor {...commonProps} handleUpdatePlayer={handleUpdatePlayer} handleCreatePlayer={handleCreatePlayer} handleUpdateGround={handleUpdateGround} handleUpdateScoreLimits={handleUpdateScoreLimits} />;
            case 'PLAYER_DATABASE': return <PlayerDatabase gameData={gameData} onAddPlayer={() => setScreen('EDITOR')} onViewPlayer={(p) => { setSelectedPlayer(p); setScreen('PLAYER_PROFILE'); }} />;
            case 'NEWS': return <News news={gameData.news} />;
            case 'STATS': return <Stats gameData={gameData} viewPlayerProfile={(p, f) => { setSelectedPlayer(p); setPlayerProfileFormat(f); setScreen('PLAYER_PROFILE'); }} />;
            case 'SETTINGS': return <Settings onResetGame={onResetGame} theme={theme} setTheme={setTheme} saveGame={saveGame} loadGame={loadGame} user={user} onSignIn={handleSignIn} onSignOut={signOutUser} />;
            case 'PLAYER_PROFILE': return <PlayerProfile player={selectedPlayer} onBack={() => setScreen('STATS')} initialFormat={playerProfileFormat} />;
            case 'MATCH_RESULT': return <MatchResultScreen result={selectedMatchResult} onBack={() => setScreen('DASHBOARD')} userTeamId={gameData.userTeamId} />;
            case 'FORWARD_RESULTS': return <ForwardResultsScreen results={forwardSimResults} onBack={() => setScreen('DASHBOARD')} userTeamId={gameData.userTeamId} />;
            case 'AWARDS_RECORDS': return <AwardsAndRecordsScreen gameData={gameData} />;
            case 'END_OF_FORMAT': return <EndOfFormatScreen gameData={gameData} handleFormatChange={handleFormatChange} handleEndSeason={handleEndOfSeason} />;
            case 'TRANSFERS': return <Transfers {...commonProps} />;
            case 'COMPARISON': return <ComparisonScreen gameData={gameData} />;
            case 'SPONSOR_ROOM': return <SponsorRoom gameData={gameData} setGameData={setGameData} />;
            case 'AUCTION_ROOM': return <AuctionRoom gameData={gameData} onAuctionComplete={(teams) => { 
                setGameData(prev => prev ? { ...prev, teams } : null);
                setScreen('DASHBOARD');
            }} />;
            case 'LIVE_MATCH': {
                const schedule = gameData.schedule[gameData.currentFormat];
                const currentMatchIndex = gameData.currentMatchIndex[gameData.currentFormat];
                const match = schedule[currentMatchIndex];
                let resolvedMatch = match ? JSON.parse(JSON.stringify(match)) : null;
                if (resolvedMatch) {
                    const resolvePlaceholder = (placeholder: string) => {
                        if (['1st', '2nd', '3rd', '4th'].includes(placeholder)) {
                            const pos = parseInt(placeholder[0]);
                            return gameData.standings[gameData.currentFormat][pos-1]?.teamName || 'TBD';
                        }
                        if (placeholder.startsWith('SF')) {
                            const sfResult = gameData.matchResults[gameData.currentFormat].find(r => r.matchNumber === placeholder.split(' ')[0]);
                            return gameData.teams.find(t => t.id === sfResult?.winnerId)?.name || 'TBD';
                        }
                        return placeholder;
                    };
                    resolvedMatch.teamA = resolvePlaceholder(resolvedMatch.teamA);
                    resolvedMatch.teamB = resolvePlaceholder(resolvedMatch.teamB);
                }
                return resolvedMatch ? (
                    <LiveMatchScreen match={resolvedMatch} gameData={gameData} onMatchComplete={handleLiveMatchComplete} onExit={handleLiveMatchExit} savedState={gameData.activeMatch} /> 
                ) : <div className="p-4 text-center"><p>Tournament finished.</p><button onClick={() => setScreen('DASHBOARD')} className="mt-4 bg-teal-500 text-white px-4 py-2 rounded">Back</button></div>;
            }
            default: return <div>Coming Soon</div>
        }
    }

    return (
        <div className="flex flex-col h-full bg-slate-50 dark:bg-[#050808] text-slate-900 dark:text-slate-100 relative">
            <main className="flex-grow overflow-y-auto relative">
                <AnimatePresence mode="wait">
                    <motion.div
                        key={screen}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -10 }}
                        transition={{ duration: 0.2, ease: "easeOut" }}
                        className="h-full"
                    >
                        {renderScreen()}
                    </motion.div>
                </AnimatePresence>
            </main>
            <BottomNavBar activeScreen={screen} setScreen={setScreen} />

            {authError && (
                <div className="absolute inset-0 bg-black/85 flex items-center justify-center p-6 z-[100] animate-fade-in">
                    <div className="bg-slate-900 border border-slate-800 p-6 rounded-3xl w-full max-w-sm text-center space-y-4 shadow-2xl relative text-white">
                        <div className="text-4xl">⚠️</div>
                        <h3 className="text-lg font-black text-rose-400 uppercase tracking-tight">{authError.title}</h3>
                        <p className="text-xs text-slate-300 leading-relaxed text-left whitespace-pre-line bg-slate-950/50 p-3.5 rounded-xl border border-white/5 font-mono max-h-[195px] overflow-y-auto w-full">
                            {authError.message}
                        </p>
                        {authError.copyText && (
                            <button
                                onClick={() => {
                                    navigator.clipboard.writeText(authError.copyText!);
                                    showFeedback("Copied to clipboard!");
                                }}
                                className="w-full py-2 bg-teal-500 hover:bg-teal-400 text-black rounded-xl font-bold text-xs uppercase tracking-wider transition-all active:scale-95"
                            >
                                Copy Hostname
                            </button>
                        )}
                        <button
                            onClick={() => setAuthError(null)}
                            className="w-full py-2 bg-white/10 hover:bg-white/15 text-white rounded-xl font-bold text-xs uppercase tracking-wider transition-all"
                        >
                            Close
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CareerHub;
