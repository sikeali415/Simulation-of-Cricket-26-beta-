import React, { useState, useEffect, useMemo } from 'react';
import { GameData, Team, Format, Player } from '../types';
import { Icons } from './Icons';
import { getRoleColor, generateAutoXI, getPlayerBadges } from '../utils';

interface LineupsProps {
    gameData: GameData;
    userTeam: Team | null;
    handleUpdatePlayingXI: (teamId: string, format: Format, newXI: string[]) => void;
    handleUpdateCaptain: (teamId: string, format: Format, playerId: string) => void;
    showFeedback: (message: string, type?: 'success' | 'error') => void;
    setGameData: React.Dispatch<React.SetStateAction<GameData | null>>;
}

const Lineups: React.FC<LineupsProps> = ({ gameData, userTeam, handleUpdatePlayingXI, handleUpdateCaptain, showFeedback, setGameData }) => {
    // Hooks must be at the top level
    const [selectedTeamId, setSelectedTeamId] = useState(userTeam?.id || '');
    const selectedTeam = useMemo(() => gameData.teams.find(t => t.id === selectedTeamId), [gameData.teams, selectedTeamId]);

    const [showFirstAidModal, setShowFirstAidModal] = useState<{playerId: string, name: string} | null>(null);

    const handleFirstAidAction = (action: 'quick' | 'emergency' | 'save') => {
        if (!showFirstAidModal || !setGameData) return;
        const playerId = showFirstAidModal.playerId;

        setGameData(prev => {
            if (!prev) return null;
            
            const newTeams = prev.teams.map(t => {
                if (t.id === prev.userTeamId) {
                    return { ...t, firstAidKits: Math.max(0, (t.firstAidKits || 0) - 1) };
                }
                return t;
            });

            const updatePlayer = (p: Player) => {
                if (p.id !== playerId) return p;
                const newP = { ...p };
                if (action === 'quick') {
                    if (newP.injury) {
                        newP.injury = { ...newP.injury, durationValue: Math.max(1, Math.floor(newP.injury.durationValue / 2)) };
                    }
                } else if (action === 'emergency') {
                    newP.healthStatus = 'temporary_fit';
                    newP.injury = null; // Clear active injury status so they can be selected
                } else if (action === 'save') {
                    if (newP.injury && newP.injury.durationType === 'seasons') {
                        newP.injury = { ...newP.injury, durationValue: 1 };
                    }
                }
                return newP;
            };

            const fullyUpdatedTeams = newTeams.map(t => ({
                ...t,
                squad: t.squad.map(updatePlayer)
            }));

            return {
                ...prev,
                teams: fullyUpdatedTeams,
                allPlayers: prev.allPlayers.map(updatePlayer)
            };
        });

        setShowFirstAidModal(null);
        showFeedback("First Aid used successfully!", "success");
    };

    const useFirstAid = (playerId: string) => {
        const player = selectedTeam?.squad.find(p => p.id === playerId);
        if (!player) return;
        setShowFirstAidModal({ playerId, name: player.name });
    };
    
    const [category, setCategory] = useState<'T20' | 'List A' | 'First Class'>('T20');
    const [selectedFormat, setSelectedFormat] = useState<Format>(gameData.currentFormat);

    const [playingXI, setPlayingXI] = useState<Player[]>([]);
    const [bench, setBench] = useState<Player[]>([]);
    const [playerToSwap, setPlayerToSwap] = useState<Player | null>(null);

    const [draggedIndex, setDraggedIndex] = useState<number | null>(null);

    const handleDragStart = (e: React.DragEvent, index: number) => {
        setDraggedIndex(index);
        e.dataTransfer.effectAllowed = "move";
    };

    const handleDragOver = (e: React.DragEvent, index: number) => {
        e.preventDefault();
    };

    const handleDrop = (e: React.DragEvent, targetIndex: number) => {
        e.preventDefault();
        if (draggedIndex === null || draggedIndex === targetIndex) return;

        const updatedXI = [...playingXI];
        const [movedPlayer] = updatedXI.splice(draggedIndex, 1);
        updatedXI.splice(targetIndex, 0, movedPlayer);

        setPlayingXI(updatedXI);
        handleUpdatePlayingXI(selectedTeam.id, selectedFormat, updatedXI.map(p => p.id));
        setDraggedIndex(null);
        showFeedback("Batting order updated!", "success");
    };

    const handleDragEnd = () => {
        setDraggedIndex(null);
    };

    const shiftPlayerPosition = (index: number, direction: 'up' | 'down') => {
        if (!playingXI || playingXI.length < 2) return;
        const targetIndex = direction === 'up' ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= playingXI.length) return;

        const updatedXI = [...playingXI];
        const temp = updatedXI[index];
        updatedXI[index] = updatedXI[targetIndex];
        updatedXI[targetIndex] = temp;

        setPlayingXI(updatedXI);
        handleUpdatePlayingXI(selectedTeam.id, selectedFormat, updatedXI.map(p => p.id));
        showFeedback("Batting order updated!", "success");
    };

    // Sync selectedTeamId if userTeam changes
    useEffect(() => {
        if (userTeam && !selectedTeamId) {
            setSelectedTeamId(userTeam.id);
        }
    }, [userTeam, selectedTeamId]);

    // Helper to get formats for a category
    const getFormatsForCategory = (cat: 'T20' | 'List A' | 'First Class') => {
        switch(cat) {
            case 'T20': return [Format.T20];
            case 'List A': return [Format.ODI];
            case 'First Class': return [Format.SHIELD];
        }
    };

    // Auto-switch selected format when category changes
    useEffect(() => {
        const formats = getFormatsForCategory(category);
        if (!formats.includes(selectedFormat)) {
            setSelectedFormat(formats[0]);
        }
    }, [category, selectedFormat]);

    const isDomesticOnlyFormat = [Format.ODI, Format.SHIELD].includes(selectedFormat);

    const memoizedSquadIdsKey = useMemo(() => {
        return JSON.stringify(selectedTeam?.squad.map(p => p.id) || []);
    }, [selectedTeam?.squad]);

    const memoizedXiIdsKey = useMemo(() => {
        if (!selectedTeam) return '';
        return JSON.stringify(gameData.playingXIs[selectedTeam.id]?.[selectedFormat] || []);
    }, [gameData.playingXIs, selectedTeam, selectedFormat]);

    useEffect(() => {
        if (!selectedTeam) return;
        const teamData = gameData.teams.find(t => t.id === selectedTeam.id);
        if (!teamData) return;

        const xiIds = gameData.playingXIs[teamData.id]?.[selectedFormat] || [];
        let xiPlayers: Player[] = [];

        if (xiIds.length === 11) {
             const foundPlayers = xiIds.map(id => teamData.squad.find(p => p.id === id)).filter(Boolean) as Player[];
             if (foundPlayers.length === 11) {
                xiPlayers = foundPlayers;
             } else {
                xiPlayers = generateAutoXI(teamData.squad, selectedFormat);
                handleUpdatePlayingXI(teamData.id, selectedFormat, xiPlayers.map(p => p.id));
             }
        } else {
            xiPlayers = generateAutoXI(teamData.squad, selectedFormat);
            handleUpdatePlayingXI(teamData.id, selectedFormat, xiPlayers.map(p => p.id));
        }
        
        setPlayingXI(xiPlayers);
        const xiIdSet = new Set(xiPlayers.map(p => p.id));
        setBench(teamData.squad.filter(p => !xiIdSet.has(p.id)));
    }, [selectedTeamId, selectedFormat, memoizedSquadIdsKey, memoizedXiIdsKey, handleUpdatePlayingXI]);

    useEffect(() => {
        setPlayerToSwap(null);
    }, [selectedTeam, selectedFormat]);

    if (!userTeam || !selectedTeam) {
        return (
            <div className="p-8 h-full flex flex-col items-center justify-center text-center">
                <div className="w-12 h-12 border-4 border-teal-500 border-t-transparent rounded-full animate-spin mb-4"></div>
                <p className="text-gray-500">Loading squad data...</p>
            </div>
        );
    }

    const captainId = selectedTeam.captains[selectedFormat] || '';

    const setCaptain = (playerId: string | null) => {
        if (playerToSwap) {
            showFeedback("Finish the current player swap first.", "error");
            return;
        }
        if (playerId) {
            const player = playingXI.find(p => p.id === playerId);
            if (isDomesticOnlyFormat && player?.isForeign) {
                showFeedback("Foreign players cannot be captain in ODI & First-Class formats.", "error");
                return;
            }
            handleUpdateCaptain(selectedTeam.id, selectedFormat, playerId);
            showFeedback(`${player?.name} appointed as Captain!`, "success");
        } else {
            handleUpdateCaptain(selectedTeam.id, selectedFormat, "");
            showFeedback("Captain sacked!", "info");
        }
    };

    const setWicketkeeper = (playerId: string) => {
        if (!selectedTeam || !setGameData) return;
        const player = selectedTeam.squad.find(p => p.id === playerId);
        if (!player) return;

        setGameData(prev => {
            if (!prev) return null;
            const updatedTeams = prev.teams.map(t => {
                if (t.id !== selectedTeamId) return t;
                const updatedSquad = t.squad.map(p => {
                    if (p.id === playerId) {
                        return { ...p, role: 'WK' as any };
                    } else if (p.role === 'WK') {
                        return { ...p, role: 'BT' as any };
                    }
                    return p;
                });
                return { ...t, squad: updatedSquad };
            });
            return {
                ...prev,
                teams: updatedTeams,
                allPlayers: prev.allPlayers.map(p => {
                    if (p.id === playerId) {
                        return { ...p, role: 'WK' as any };
                    } else if (p.role === 'WK' && prev.teams.find(t => t.id === selectedTeamId)?.squad.some(s => s.id === p.id)) {
                        return { ...p, role: 'BT' as any };
                    }
                    return p;
                })
            };
        });
        showFeedback(`${player.name} designated as Wicketkeeper!`, "success");
    };

    const togglePlayerRole = (playerId: string, currentRole: any) => {
        if (!selectedTeam || !setGameData) return;
        const rolesOrder = ['BT', 'WK', 'AR', 'SB', 'BL'];
        const nextRoleIdx = (rolesOrder.indexOf(currentRole) + 1) % rolesOrder.length;
        const newRole = rolesOrder[nextRoleIdx];

        setGameData(prev => {
            if (!prev) return null;
            const updatedTeams = prev.teams.map(t => {
                if (t.id !== selectedTeamId) return t;
                const updatedSquad = t.squad.map(p => {
                    if (p.id === playerId) {
                        return { ...p, role: newRole as any };
                    }
                    return p;
                });
                return { ...t, squad: updatedSquad };
            });
            return {
                ...prev,
                teams: updatedTeams,
                allPlayers: prev.allPlayers.map(p => {
                    if (p.id === playerId) {
                        return { ...p, role: newRole as any };
                    }
                    return p;
                })
            };
        });
        showFeedback(`Player role updated to ${newRole}!`, "success");
    };

    const selectPlayerForSwap = (player: Player) => {
        if (player.id === selectedTeam.captainId) {
            showFeedback("Cannot swap the captain. Sack the captain or appoint a new one first.", "error");
            return;
        }
        if (playerToSwap && playerToSwap.id === player.id) {
            setPlayerToSwap(null);
        } else {
            setPlayerToSwap(player);
        }
    };

    const completeSwap = (playerFromBench: Player) => {
        if (!playerToSwap) return;
        if (playerFromBench.injury) {
            showFeedback(`Cannot select ${playerFromBench.name} as they are currently injured and unfit to play! Use a First Aid kit if available.`, "error");
            return;
        }
        if (isDomesticOnlyFormat && playerFromBench.isForeign) {
            showFeedback("Foreign players are not allowed in this format.", "error");
            return;
        }
        
        const newXI = playingXI.map(p => p.id === playerToSwap.id ? playerFromBench : p);
        const newBench = bench.filter(p => p.id !== playerFromBench.id);
        newBench.push(playerToSwap);
        newBench.sort((a, b) => a.name.localeCompare(b.name));

        setPlayingXI(newXI);
        setBench(newBench);
        handleUpdatePlayingXI(selectedTeam.id, selectedFormat, newXI.map(p => p.id));
        setPlayerToSwap(null);
        showFeedback("Players swapped successfully!", "success");
    };

    const renderPlayerList = (players: Player[], isXI: boolean) => (
        <ul className="space-y-1">
            {players.map((player, idx) => {
                const isFirstOpener = isXI && idx === 0;
                const isSecondOpener = isXI && idx === 1;
                const isCaptain = player.id === captainId;

                return (
                    <li 
                        key={player.id} 
                        draggable={isXI}
                        onDragStart={(e) => isXI && handleDragStart(e, idx)}
                        onDragOver={(e) => isXI && handleDragOver(e, idx)}
                        onDrop={(e) => isXI && handleDrop(e, idx)}
                        onDragEnd={() => isXI && handleDragEnd()}
                        className={`flex flex-col p-2 rounded-md transition-all ${
                            draggedIndex === idx && isXI 
                                ? 'bg-teal-1050 dark:bg-teal-900 border border-dashed border-teal-500 pl-4' 
                                : playerToSwap?.id === player.id 
                                ? 'bg-teal-200 dark:bg-teal-800' 
                                : 'bg-gray-100 dark:bg-gray-900/50'
                        } ${player.injury ? 'border-l-4 border-amber-500' : ''}`}
                    >
                        <div className="flex items-center w-full">
                            {/* Drag handle and Lineup Position badge */}
                            {isXI && (
                                <div className="flex items-center gap-1.5 mr-1.5 shrink-0 select-none">
                                    <div 
                                        className="cursor-grab active:cursor-grabbing text-slate-400 hover:text-slate-100 font-mono text-base font-bold pr-0.5"
                                        title="Drag to rearrange batting order"
                                    >
                                        ⠿
                                    </div>
                                    <span className="w-5 h-5 rounded-full bg-slate-200 dark:bg-slate-800 text-[10px] text-slate-800 dark:text-slate-300 font-bold flex items-center justify-center border border-slate-300 dark:border-slate-700" title={`Batting position ${idx + 1}`}>
                                        {idx + 1}
                                    </span>
                                </div>
                            )}

                            <button 
                                onClick={() => togglePlayerRole(player.id, player.role)}
                                className={`font-bold w-10 text-[10px] text-center py-0.5 rounded text-white shrink-0 hover:scale-105 cursor-pointer transition-transform ${getRoleColor(player.role)}`}
                                title="Click to cycle player role"
                            >
                                {player.role}
                            </button>
                            
                            <span className="flex-grow flex flex-col justify-center min-w-0 px-2">
                                <span className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-1.5 flex-wrap">
                                    {player.name} 
                                    {player.isForeign && <span className="text-[9px] bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400 font-extrabold px-1 rounded">F</span>} 
                                    {isCaptain && <span className="text-[9px] bg-yellow-500 dark:bg-yellow-600 text-amber-950 dark:text-amber-50 font-black px-1 rounded">C</span>}
                                    {isFirstOpener && <span className="text-[8px] bg-teal-100 dark:bg-teal-950 text-teal-605 dark:text-teal-400 font-bold px-1 rounded uppercase">Opener #1</span>}
                                    {isSecondOpener && <span className="text-[8px] bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 font-bold px-1 rounded uppercase">Opener #2</span>}
                                    {!isFirstOpener && !isSecondOpener && isXI && idx <= 5 && <span className="text-[8px] bg-sky-100 dark:bg-sky-950 text-sky-600 dark:text-sky-400 font-medium px-1 rounded uppercase">Middle Order</span>}
                                    {isXI && idx >= 6 && <span className="text-[8px] bg-zinc-200 dark:bg-zinc-850 text-zinc-650 dark:text-zinc-400 font-mono px-1 rounded uppercase">Tail</span>}
                                </span>
                                {player.stats[selectedFormat] && player.stats[selectedFormat].matches > 0 ? (
                                    <span className="text-[10px] text-teal-600 dark:text-teal-400 font-mono mt-0.5 leading-none">
                                        {player.role === 'BT' || player.role === 'WK' ? (
                                            <span>R: <strong>{player.stats[selectedFormat].runs}</strong> | Avg: <strong>{player.stats[selectedFormat].average ? player.stats[selectedFormat].average.toFixed(1) : '-'}</strong> | SR: <strong>{player.stats[selectedFormat].strikeRate ? player.stats[selectedFormat].strikeRate.toFixed(1) : '-'}</strong></span>
                                        ) : player.role === 'SB' || player.role === 'BL' ? (
                                            <span>W: <strong>{player.stats[selectedFormat].wickets}</strong> | Econ: <strong>{player.stats[selectedFormat].economy ? player.stats[selectedFormat].economy.toFixed(2) : '-'}</strong> | Avg: <strong>{player.stats[selectedFormat].bowlingAverage ? player.stats[selectedFormat].bowlingAverage.toFixed(1) : '-'}</strong></span>
                                        ) : (
                                            <span>R: <strong>{player.stats[selectedFormat].runs}</strong> | W: <strong>{player.stats[selectedFormat].wickets}</strong> | Econ: <strong>{player.stats[selectedFormat].economy ? player.stats[selectedFormat].economy.toFixed(2) : '-'}</strong></span>
                                        )}
                                    </span>
                                ) : (
                                    <span className="text-[10px] text-gray-400 italic mt-0.5 leading-none">No league stats yet</span>
                                )}
                            </span>
                            <span className="font-semibold text-xs text-slate-800 dark:text-slate-200 mr-2 flex flex-col items-center">
                                <span className="text-[8px] uppercase text-gray-400 leading-none">Bat</span>
                                {player.battingSkill}
                            </span>
                            <span className="font-semibold text-xs text-gray-400 mr-2.5 flex flex-col items-center">
                                <span className="text-[8px] uppercase text-gray-500 leading-none">Bowl</span>
                                {player.secondarySkill}
                            </span>

                            {/* Tactile up/down shifting buttons */}
                            {isXI && (
                                <div className="flex flex-col gap-0.5 mr-2 shrink-0 select-none">
                                    <button 
                                        onClick={() => shiftPlayerPosition(idx, 'up')}
                                        disabled={idx === 0}
                                        className="h-4 w-5 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 disabled:opacity-30 rounded text-[9px] flex items-center justify-center text-slate-750 dark:text-slate-300 font-bold leading-none cursor-pointer"
                                        title="Shift batting order up"
                                    >
                                        ▲
                                    </button>
                                    <button 
                                        onClick={() => shiftPlayerPosition(idx, 'down')}
                                        disabled={idx === 10}
                                        className="h-4 w-5 bg-slate-200 dark:bg-slate-800 hover:bg-slate-300 dark:hover:bg-slate-700 disabled:opacity-30 rounded text-[9px] flex items-center justify-center text-slate-755 dark:text-slate-300 font-bold leading-none cursor-pointer"
                                        title="Shift batting order down"
                                    >
                                        ▼
                                    </button>
                                </div>
                            )}

                            {isXI && (
                                player.id === captainId ? (
                                    <div className="flex gap-1 items-center mr-1">
                                        <span className="text-lg" title="Captain">🧢</span>
                                        <button onClick={() => setCaptain(null)} className="text-[9px] bg-red-500/20 hover:bg-red-500 text-red-600 hover:text-white font-bold p-1 rounded transition-colors" title="Sack Captain">✕</button>
                                    </div>
                                ) : (
                                    <button onClick={() => setCaptain(player.id)} className="text-[14px] bg-slate-700/50 hover:bg-yellow-500 text-slate-400 hover:text-slate-900 p-1 rounded mr-1 transition-all" title="Make Captain">🧢</button>
                                )
                            )}
                            {isXI && (
                                player.role === 'WK' ? (
                                    <span className="text-lg mr-1.5 leading-none select-none" title="Designated Wicketkeeper">🧤</span>
                                ) : (
                                    <button onClick={() => setWicketkeeper(player.id)} className="text-[14.5px] bg-slate-700/50 hover:bg-emerald-600 hover:text-white text-slate-350 p-1 rounded mr-1 transition-all" title="Designate Wicketkeeper">🧤</button>
                                )
                            )}
                            {isXI ? (
                                 <button onClick={() => selectPlayerForSwap(player)} className="text-red-500 hover:scale-110 transition-transform"><Icons.RemoveCircle /></button>
                            ) : (
                                <button onClick={() => completeSwap(player)} disabled={!playerToSwap || (isDomesticOnlyFormat && player.isForeign)} className="disabled:opacity-30 text-teal-500 hover:scale-110 transition-transform">
                                    <Icons.PlusCircle />
                                </button>
                            )}
                        </div>

                        {/* Dynamic Badges and Injury Display */}
                        <div className="pl-8 pr-2 mt-1 flex flex-col gap-1 w-full border-t border-slate-200/50 dark:border-slate-800/50 pt-1">
                            {/* Injury Alert & First Aid Treating */}
                            {player.injury && (
                                <div className="flex items-center justify-between bg-amber-500/10 border border-amber-500/20 rounded p-1 mb-1 animate-pulse">
                                    <span className="text-[9px] text-amber-500 font-bold flex items-center gap-1">
                                        🚨 Injured: {player.injury.text} ({player.injury.durationValue} {player.injury.durationValue === 1 ? 'match' : player.injury.durationType})
                                    </span>
                                    {selectedTeam.id === gameData.userTeamId && selectedTeam.firstAidKits && selectedTeam.firstAidKits > 0 && (
                                        <button 
                                            onClick={() => useFirstAid(player.id)}
                                            className="text-[8px] bg-red-600 hover:bg-red-500 text-white font-extrabold px-1.5 py-0.5 rounded shadow flex items-center gap-0.5 cursor-pointer"
                                        >
                                            💊 USE FIRST AID
                                        </button>
                                    )}
                                </div>
                            )}

                            {/* Badges list */}
                            <div className="flex flex-wrap gap-1">
                                {getPlayerBadges(player).slice(0, 1).map((badge, bidx) => (
                                    <span key={bidx} className="text-[9px] font-semibold bg-amber-500/10 dark:bg-amber-500/5 text-amber-600 dark:text-amber-400 border border-amber-500/20 rounded-full px-1.5 leading-none py-0.5">
                                        🏆 {badge}
                                    </span>
                                ))}
                            </div>
                        </div>
                    </li>
                );
            })}
        </ul>
    );

    return (
        <div className="p-2 h-[calc(100vh-90px)] flex flex-col">
            <h2 className="text-xl font-bold text-center mb-2">Manage Lineups</h2>
            <div className="mb-2">
                <select 
                    value={selectedTeamId} 
                    onChange={(e) => setSelectedTeamId(e.target.value)}
                    className="w-full p-2 rounded-md bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600"
                >
                    {gameData.teams.map(team => <option key={team.id} value={team.id}>{team.name}</option>)}
                </select>
            </div>

            {selectedTeam.id === gameData.userTeamId && (
                <div className="bg-slate-800 border border-slate-700/60 p-2.5 rounded-lg mb-2 flex justify-between items-center text-xs text-white">
                    <div className="flex items-center gap-2">
                        <span className="text-base">🚑</span>
                        <div>
                            <p className="font-extrabold uppercase text-slate-300">Team Medical Center</p>
                            <p className="text-[9px] text-slate-400">Emergency healing reserves</p>
                        </div>
                    </div>
                    <div className="bg-teal-950 border border-teal-500/30 px-2.5 py-1 rounded-md text-center font-bold flex items-center justify-center">
                        <span className="text-sm text-teal-400 font-black">{selectedTeam.firstAidKits || 0}</span>
                        <span className="text-[9px] text-gray-400 uppercase ml-1.5">First Aid Kits</span>
                    </div>
                </div>
            )}
            
             {/* Category Tabs */}
             <div className="flex justify-center border-b border-gray-300 dark:border-gray-700 mb-2">
                {['T20', 'List A', 'First Class'].map((cat) => (
                    <button 
                        key={cat} 
                        onClick={() => setCategory(cat as any)} 
                        className={`px-4 py-2 text-sm font-semibold ${category === cat ? 'border-b-2 border-teal-500 text-teal-500' : 'text-gray-500'}`}
                    >
                        {cat}
                    </button>
                ))}
            </div>
            
            {/* Tournament Dropdown */}
            <div className="mb-2">
                <select
                    value={selectedFormat}
                    onChange={(e) => setSelectedFormat(e.target.value as Format)}
                    className="w-full p-2 rounded-md bg-white dark:bg-gray-800 border border-gray-300 dark:border-gray-600 text-sm"
                >
                    {getFormatsForCategory(category).map(f => (
                        <option key={f} value={f}>{f}</option>
                    ))}
                </select>
            </div>

            {/* Auto Optimize Button */}
            <div className="mb-2">
                <button
                    onClick={() => {
                        const optimalXI = generateAutoXI(selectedTeam.squad, selectedFormat);
                        setPlayingXI(optimalXI);
                        const xiSet = new Set(optimalXI.map(p => p.id));
                        setBench(selectedTeam.squad.filter(p => !xiSet.has(p.id)));
                        handleUpdatePlayingXI(selectedTeam.id, selectedFormat, optimalXI.map(p => p.id));
                        showFeedback(`${selectedTeam.name} Playing XI Auto-Optimized!`, "success");
                    }}
                    className="w-full flex items-center justify-center gap-1.5 py-1.5 px-3 rounded-md bg-gradient-to-r from-teal-500 to-emerald-600 hover:brightness-110 text-white font-bold text-xs transition-all shadow-sm cursor-pointer"
                >
                    <Icons.PlusCircle />
                    <span>Auto-Optimize Playing XI</span>
                </button>
            </div>

            {isDomesticOnlyFormat && (
                <div className="bg-yellow-100 dark:bg-yellow-900/50 text-yellow-800 dark:text-yellow-300 p-2 rounded-md text-sm text-center my-2">
                    Only domestic players are allowed in ODI and First-Class formats.
                </div>
            )}
            <div className="flex-grow overflow-y-auto">
                <h3 className="font-bold my-2">Playing XI ({playingXI.length} / 11)</h3>
                {renderPlayerList(playingXI, true)}
                <h3 className="font-bold my-2">Bench</h3>
                {renderPlayerList(bench, false)}
            </div>

            {/* First Aid Choice Modal */}
            {showFirstAidModal && (
                <div className="fixed inset-0 z-[200] bg-black/80 flex items-center justify-center p-4">
                    <div className="bg-slate-800 rounded-xl border border-slate-700 w-full max-w-sm p-6 shadow-2xl animate-fade-in">
                        <h3 className="text-white font-black text-xl mb-2">Use First Aid?</h3>
                        <p className="text-slate-400 text-sm mb-6">Select how you want to treat <span className="text-teal-400 font-bold">{showFirstAidModal.name}</span>:</p>
                        
                        <div className="space-y-3">
                            <button 
                                onClick={() => handleFirstAidAction('quick')}
                                className="w-full p-4 bg-slate-700 hover:bg-slate-600 rounded-lg text-left transition-colors flex items-center gap-3"
                            >
                                <span className="text-2xl">⚡</span>
                                <div>
                                    <p className="text-white font-bold text-sm">Quick Recovery</p>
                                    <p className="text-[10px] text-slate-400 uppercase">Reduce duration by 50%</p>
                                </div>
                            </button>
                            
                            <button 
                                onClick={() => handleFirstAidAction('emergency')}
                                className="w-full p-4 bg-slate-700 hover:bg-slate-600 rounded-lg text-left transition-colors flex items-center gap-3"
                            >
                                <span className="text-2xl">🚑</span>
                                <div>
                                    <p className="text-white font-bold text-sm">Emergency Play</p>
                                    <p className="text-[10px] text-slate-400 uppercase">Play immediately (-15% skills)</p>
                                </div>
                            </button>

                            <button 
                                onClick={() => handleFirstAidAction('save')}
                                className="w-full p-4 bg-slate-700 hover:bg-slate-600 rounded-lg text-left transition-colors flex items-center gap-3"
                            >
                                <span className="text-2xl">🏛️</span>
                                <div>
                                    <p className="text-white font-bold text-sm">Save Long-Term</p>
                                    <p className="text-[10px] text-slate-400 uppercase">Convert major injury to 1 season</p>
                                </div>
                            </button>

                            <button 
                                onClick={() => setShowFirstAidModal(null)}
                                className="w-full py-3 text-slate-500 font-bold hover:text-white transition-colors"
                            >
                                Cancel
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Lineups;