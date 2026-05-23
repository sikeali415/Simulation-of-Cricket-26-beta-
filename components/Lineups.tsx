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
    }, [selectedTeam, selectedFormat, gameData, handleUpdatePlayingXI]);

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
            {players.map(player => (
                <li key={player.id} className={`flex flex-col p-2 rounded-md transition-colors ${playerToSwap?.id === player.id ? 'bg-teal-200 dark:bg-teal-800' : 'bg-gray-100 dark:bg-gray-900/50'} ${player.injury ? 'border-l-4 border-amber-500' : ''}`}>
                    <div className="flex items-center w-full">
                        <span className={`font-bold w-10 text-xs text-center py-0.5 rounded text-white ${getRoleColor(player.role)}`}>{player.role}</span>
                        <span className="flex-grow flex flex-col justify-center min-w-0 px-2">
                            <span className="truncate text-sm font-semibold text-slate-900 dark:text-slate-100 flex items-center gap-1.5 flex-wrap">
                                {player.name} {player.isForeign ? <span className="text-[9px] bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400 font-extrabold px-1 rounded">F</span> : ''} {player.id === captainId ? <span className="text-[9px] bg-yellow-1050 bg-yellow-4050 text-yellow-600 dark:text-yellow-400 font-black px-1 rounded">C</span> : ''}
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
                            <span className="text-[8px] uppercase text-gray-500 leading-none">Bat</span>
                            {player.battingSkill}
                        </span>
                        <span className="font-semibold text-xs text-gray-500 mr-4 flex flex-col items-center">
                            <span className="text-[8px] uppercase text-gray-500 leading-none">Bowl</span>
                            {player.secondarySkill}
                        </span>
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
                        {isXI ? (
                             <button onClick={() => selectPlayerForSwap(player)} className="text-red-500 hover:scale-110 transition-transform"><Icons.RemoveCircle /></button>
                        ) : (
                            <button onClick={() => completeSwap(player)} disabled={!playerToSwap || (isDomesticOnlyFormat && player.isForeign)} className="disabled:opacity-30 text-teal-500 hover:scale-110 transition-transform">
                                <Icons.PlusCircle />
                            </button>
                        )}
                    </div>

                    {/* Dynamic Badges and Injury Display */}
                    <div className="pl-12 pr-2 mt-1 flex flex-col gap-1 w-full border-t border-slate-200/50 dark:border-slate-800/50 pt-1">
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
            ))}
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