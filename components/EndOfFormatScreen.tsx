import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { GameData, Format, Player, Team } from '../types';
import { Icons } from './Icons';
import { getRoleFullName, getPlayerBasePrice, getPlayerMarketPrice } from '../utils';

interface EndOfFormatScreenProps {
    gameData: GameData;
    handleFormatChange: (newFormat: Format) => void;
    handleEndSeason: (retainedPlayers: Player[]) => void;
}

const EndOfFormatScreen: React.FC<EndOfFormatScreenProps> = ({ gameData, handleFormatChange, handleEndSeason }) => {
    const [view, setView] = useState<'awards' | 'retention'>('awards');
    const [retentionTab, setRetentionTab] = useState<'retain' | 'directSign'>('retain');
    const [retainedIds, setRetainedIds] = useState<Set<string>>(new Set());
    
    // Direct Signing States (1 Foreign and 1 Local max)
    const [selectedForeignSignId, setSelectedForeignSignId] = useState<string>('');
    const [selectedLocalSignId, setSelectedLocalSignId] = useState<string>('');
    const [errorMessage, setErrorMessage] = useState<string | null>(null);

    const userTeam = useMemo(() => gameData.teams.find(t => t.id === gameData.userTeamId), [gameData]);
    
    const lastAward = gameData.awardsHistory[gameData.awardsHistory.length-1];
    
    const formatsOrder = [
        Format.T20, Format.ODI, Format.SHIELD
    ];

    const currentIdx = formatsOrder.indexOf(gameData.currentFormat);
    const nextFormat = currentIdx !== -1 && currentIdx < formatsOrder.length - 1 ? formatsOrder[currentIdx + 1] : null;

    // Standard Retention Players
    const retainedPlayersList = useMemo(() => {
        return userTeam?.squad.filter(p => retainedIds.has(p.id)) || [];
    }, [userTeam, retainedIds]);

    const nationalCount = useMemo(() => retainedPlayersList.filter(p => !p.isForeign).length, [retainedPlayersList]);
    const internationalCount = useMemo(() => retainedPlayersList.filter(p => p.isForeign).length, [retainedPlayersList]);

    const totalRetentionCost = useMemo(() => {
        return retainedPlayersList.reduce((sum, p) => sum + getPlayerMarketPrice(p), 0);
    }, [retainedPlayersList]);

    // Eligible players from OTHER teams for Direct Signings
    const availablePlayersPool = useMemo(() => {
        const userSquadIds = new Set(userTeam?.squad.map(p => p.id) || []);
        return gameData.allPlayers.filter(p => !userSquadIds.has(p.id));
    }, [gameData.allPlayers, userTeam]);

    const availableForeignPlayers = useMemo(() => {
        return availablePlayersPool.filter(p => p.isForeign).sort((a,b) => (b.battingSkill + b.secondarySkill) - (a.battingSkill + a.secondarySkill));
    }, [availablePlayersPool]);

    const availableLocalPlayers = useMemo(() => {
        return availablePlayersPool.filter(p => !p.isForeign).sort((a,b) => (b.battingSkill + b.secondarySkill) - (a.battingSkill + a.secondarySkill));
    }, [availablePlayersPool]);

    const directSignedForeign = useMemo(() => {
        return gameData.allPlayers.find(p => p.id === selectedForeignSignId) || null;
    }, [gameData.allPlayers, selectedForeignSignId]);

    const directSignedLocal = useMemo(() => {
        return gameData.allPlayers.find(p => p.id === selectedLocalSignId) || null;
    }, [gameData.allPlayers, selectedLocalSignId]);

    const directSignCost = useMemo(() => {
        let cost = 0;
        if (directSignedForeign) {
            cost += getPlayerBasePrice(directSignedForeign) * 10;
        }
        if (directSignedLocal) {
            cost += getPlayerBasePrice(directSignedLocal) * 10;
        }
        return cost;
    }, [directSignedForeign, directSignedLocal]);

    const toggleRetention = (id: string) => {
        const player = userTeam?.squad.find(p => p.id === id);
        if (!player) return;
        setErrorMessage(null);

        setRetainedIds(prev => {
            const next = new Set(prev);
            if (next.has(id)) {
                next.delete(id);
            } else {
                const alreadySelected = userTeam?.squad.filter(p => next.has(p.id)) || [];
                const nextCost = alreadySelected.reduce((sum, p) => sum + getPlayerMarketPrice(p), 0) + getPlayerMarketPrice(player);
                
                if (nextCost > 30.0) {
                    setErrorMessage("Pre-season retention budget would exceed 30.0 Crore limit!");
                    return prev;
                }

                const natCount = alreadySelected.filter(p => !p.isForeign).length;
                const intCount = alreadySelected.filter(p => p.isForeign).length;

                if (player.isForeign) {
                    if (intCount < 1) {
                        next.add(id);
                    } else {
                        setErrorMessage("Max 1 foreign/international retention allowed!");
                    }
                } else {
                    if (natCount < 3) {
                        next.add(id);
                    } else {
                        setErrorMessage("Max 3 national/local retentions allowed!");
                    }
                }
            }
            return next;
        });
    };

    const finalizeSeason = () => {
        if (totalRetentionCost > 30.0) {
            setErrorMessage("Standard core retention cost exceeds the 30 Crores preseason retention budget limit!");
            return;
        }

        const finalRetained = userTeam?.squad.filter(p => retainedIds.has(p.id)) || [];
        const finalDirectSigned: Player[] = [];
        if (directSignedForeign) finalDirectSigned.push(directSignedForeign);
        if (directSignedLocal) finalDirectSigned.push(directSignedLocal);

        const combinedKeptList = [...finalRetained, ...finalDirectSigned];
        handleEndSeason(combinedKeptList);
    };

    if (view === 'retention') {
        return (
            <div className="p-6 h-full flex flex-col bg-slate-950 text-white overflow-hidden">
                <div className="mb-4">
                    <h2 className="text-3xl font-black italic tracking-tighter uppercase mb-1">Pre-Season HQ</h2>
                    <p className="text-slate-400 text-[11px] uppercase tracking-widest leading-none">Assemble your foundation and secure premium marquee signings.</p>
                </div>

                {/* Main Tabs */}
                <div className="flex border-b border-slate-800 mb-4 gap-2">
                    <button 
                        onClick={() => { setRetentionTab('retain'); setErrorMessage(null); }}
                        className={`flex-1 py-3 text-xs uppercase font-black tracking-widest border-b-2 text-center transition-all ${retentionTab === 'retain' ? 'border-teal-500 text-teal-400 bg-teal-500/5' : 'border-transparent text-slate-500 hover:text-slate-300'}`}
                    >
                        Core Retentions ({retainedIds.size}/4)
                    </button>
                    <button 
                        onClick={() => { setRetentionTab('directSign'); setErrorMessage(null); }}
                        className={`flex-1 py-3 text-xs uppercase font-black tracking-widest border-b-2 text-center transition-all ${retentionTab === 'directSign' ? 'border-teal-500 text-teal-400 bg-teal-500/5' : 'border-transparent text-slate-500 hover:text-slate-300'}`}
                    >
                        Direct Marquee (10x Base)
                    </button>
                </div>

                {errorMessage && (
                    <div className="mb-4 bg-red-900/30 border border-red-500 text-red-200 p-3 rounded-xl text-xs flex items-center gap-2 font-mono">
                        <span className="animate-pulse">⚠️</span> {errorMessage}
                    </div>
                )}

                {/* Tab Contents */}
                <div className="flex-1 overflow-y-auto min-h-0 mb-4 pr-1">
                    {retentionTab === 'retain' ? (
                        <div className="space-y-2">
                            <p className="text-[10px] text-slate-500 uppercase tracking-widest mb-3 leading-tight">
                                Retained players are kept on with realistic <strong>market valuations</strong> based on historical parameters. Max spend: 30.0 Cr.
                            </p>
                            {userTeam?.squad.map(p => {
                                const isRetained = retainedIds.has(p.id);
                                const marketPrice = getPlayerMarketPrice(p);
                                const canRetain = isRetained || (p.isForeign ? internationalCount < 1 : nationalCount < 3);
                                
                                return (
                                    <div 
                                        key={p.id} 
                                        onClick={() => toggleRetention(p.id)}
                                        className={`p-4 rounded-2xl border-2 transition-all cursor-pointer ${isRetained ? 'bg-teal-500/10 border-teal-500 shadow-[0_0_15px_rgba(20,184,166,0.15)]' : canRetain ? 'bg-slate-900/80 border-slate-800 hover:border-slate-700' : 'bg-slate-900/30 border-slate-900/50 opacity-40'}`}
                                    >
                                        <div className="flex justify-between items-center">
                                            <div>
                                                <p className="font-bold text-sm flex items-center gap-2">
                                                    {p.name} {p.isForeign ? '✈️' : ''}
                                                    <span className="text-[9px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded font-mono font-normal">Skl: {Math.max(p.battingSkill, p.secondarySkill)}</span>
                                                </p>
                                                <p className="text-[10px] text-zinc-500 uppercase font-bold mt-1">
                                                    {getRoleFullName(p.role)} • {p.isForeign ? 'International' : 'National'}
                                                </p>
                                            </div>
                                            <div className="text-right">
                                                <div className={`text-xs font-black tracking-widest ${isRetained ? 'text-teal-400' : 'text-slate-500'}`}>
                                                    {isRetained ? 'RETAINED' : 'RELEASED'}
                                                </div>
                                                <div className="text-[10px] text-teal-500 font-mono mt-1 font-bold">
                                                    CR: {marketPrice.toFixed(1)} Cr
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    ) : (
                        <div className="space-y-5">
                            <div className="bg-gradient-to-r from-teal-950/20 to-slate-900 border border-teal-500/20 p-4 rounded-2xl">
                                <h3 className="text-xs font-black uppercase text-teal-400 tracking-wider mb-1 flex items-center gap-2">
                                    🌟 Direct Marquee Signings
                                </h3>
                                <p className="text-[10px] text-slate-400 leading-relaxed font-sans">
                                    Directly sign exactly <strong>one Foreign</strong> and <strong>one Local</strong> champion from the open pool immediately. To claim premium priority before the auction floor opens, these players require a pre-season fee of <strong>10x of their base prices</strong>.
                                </p>
                            </div>

                            {/* Foreign Dropdown */}
                            <div className="space-y-2">
                                <label className="text-xs font-black uppercase text-zinc-400 tracking-widest block">
                                    Direct Sign: 1x Marquee Foreign Player
                                </label>
                                <select 
                                    value={selectedForeignSignId}
                                    onChange={(e) => { setSelectedForeignSignId(e.target.value); setErrorMessage(null); }}
                                    className="w-full p-3 rounded-2xl bg-slate-900 border border-slate-800 text-xs font-bold text-slate-200 focus:outline-none focus:border-teal-500 transition-colors"
                                >
                                    <option value="">-- None Selected --</option>
                                    {availableForeignPlayers.map(p => {
                                        const bp = getPlayerBasePrice(p);
                                        const fee = bp * 10;
                                        return (
                                            <option key={p.id} value={p.id}>
                                                {p.name} (Skl: {Math.max(p.battingSkill, p.secondarySkill)}) - Fee: {fee.toFixed(1)} Cr
                                            </option>
                                        );
                                    })}
                                </select>
                                {directSignedForeign && (
                                    <div className="p-3 rounded-xl bg-teal-950/20 border border-teal-500/20 text-[11px] font-mono flex justify-between items-center text-teal-300">
                                        <span>{directSignedForeign.name} ({getRoleFullName(directSignedForeign.role)})</span>
                                        <strong>Cost: {(getPlayerBasePrice(directSignedForeign) * 10).toFixed(1)} Cr</strong>
                                    </div>
                                )}
                            </div>

                            {/* Local Dropdown */}
                            <div className="space-y-2">
                                <label className="text-xs font-black uppercase text-zinc-400 tracking-widest block">
                                    Direct Sign: 1x Marquee Local Player
                                </label>
                                <select 
                                    value={selectedLocalSignId}
                                    onChange={(e) => { setSelectedLocalSignId(e.target.value); setErrorMessage(null); }}
                                    className="w-full p-3 rounded-2xl bg-slate-900 border border-slate-800 text-xs font-bold text-slate-200 focus:outline-none focus:border-teal-500 transition-colors"
                                >
                                    <option value="">-- None Selected --</option>
                                    {availableLocalPlayers.map(p => {
                                        const bp = getPlayerBasePrice(p);
                                        const fee = bp * 10;
                                        return (
                                            <option key={p.id} value={p.id}>
                                                {p.name} (Skl: {Math.max(p.battingSkill, p.secondarySkill)}) - Fee: {fee.toFixed(1)} Cr
                                            </option>
                                        );
                                    })}
                                </select>
                                {directSignedLocal && (
                                    <div className="p-3 rounded-xl bg-teal-950/20 border border-teal-500/20 text-[11px] font-mono flex justify-between items-center text-teal-300">
                                        <span>{directSignedLocal.name} ({getRoleFullName(directSignedLocal.role)})</span>
                                        <strong>Cost: {(getPlayerBasePrice(directSignedLocal) * 10).toFixed(1)} Cr</strong>
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>

                {/* Pre-Season Summary Metrics Dashboard */}
                <div className="grid grid-cols-3 gap-2 mb-4">
                    <div className="bg-slate-900/80 p-3 rounded-2xl border border-slate-800 text-center">
                        <span className="text-[9px] font-black uppercase text-slate-500 block leading-none mb-1">Retention Purse</span>
                        <span className={`text-sm font-black italic tracking-tight ${totalRetentionCost > 30.0 ? 'text-red-400' : 'text-teal-400'}`}>
                            {totalRetentionCost.toFixed(1)} <span className="text-[10px] font-normal text-slate-500">/ 30m</span>
                        </span>
                    </div>
                    <div className="bg-slate-900/80 p-3 rounded-2xl border border-slate-800 text-center">
                        <span className="text-[9px] font-black uppercase text-slate-500 block leading-none mb-1">Direct Marquees</span>
                        <span className="text-sm font-black italic tracking-tight text-white">
                            {directSignCost.toFixed(1)} <span className="text-[10px] font-normal text-slate-500">Cr</span>
                        </span>
                    </div>
                    <div className="bg-slate-900/80 p-3 rounded-2xl border border-slate-800 text-center">
                        <span className="text-[9px] font-black uppercase text-slate-500 block leading-none mb-1">Season Budget Left</span>
                        <span className="text-sm font-black italic tracking-tight text-yellow-400">
                            {(100.0 - (totalRetentionCost + directSignCost)).toFixed(1)} <span className="text-[10px] font-normal text-slate-500">/ 100</span>
                        </span>
                    </div>
                </div>

                <button 
                    onClick={finalizeSeason}
                    disabled={totalRetentionCost > 30.0}
                    className="w-full bg-teal-500 py-5 rounded-3xl text-lg font-black italic uppercase tracking-tighter shadow-2xl hover:bg-teal-400 transition-all active:scale-95 disabled:opacity-30 disabled:pointer-events-none text-slate-950"
                >
                    Finalize & Start {gameData.currentFormat === Format.SHIELD ? 'Season Draft' : 'Auction Draft'}
                </button>
            </div>
        );
    }

    return (
        <div className="p-6 text-center flex flex-col justify-between min-h-full bg-slate-950 text-white overflow-y-auto">
            <div className="space-y-6">
                <div>
                    <span className="text-[10px] font-black tracking-widest text-teal-400 uppercase bg-teal-500/10 border border-teal-500/20 px-3 py-1 rounded-full">{lastAward?.format} Tournament Finished</span>
                    <h2 className="text-3xl font-black italic tracking-tighter uppercase mt-3 mb-1 text-white">Season Finale</h2>
                    <p className="text-xs text-slate-400 capitalize font-medium">{lastAward?.format} champions crowned & seasonal honors awarded.</p>
                </div>

                {/* Championship Glory Card */}
                <div className="relative overflow-hidden bg-gradient-to-b from-amber-500/20 to-slate-900 border-2 border-amber-500/40 rounded-3xl p-6 shadow-[0_0_30px_rgba(245,158,11,0.15)]">
                    {/* Golden Trophy Graphic */}
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16 mx-auto mb-2 text-amber-500 drop-shadow-[0_0_20px_rgba(245,158,11,0.5)] animate-pulse">
                      <path d="M19 4h-2V2H7v2H5c-1.1 0-2 .9-2 2v3c0 1.88 1.31 3.47 3.09 3.86C6.72 14.47 8.91 16 11.5 16h1c2.59 0 4.78-1.53 5.41-3.14c1.78-.39 3.09-1.98 3.09-3.86V6c0-1.1-.9-2-2-2zM5 9V6h2v4.86C5.81 10.45 5 9.8 5 9zm14 0c0 .8-.81 1.45-2 1.86V6h2v3zm-4 7h-6v2h6v-2zm2 3H7v2h10v-2z"/>
                    </svg>
                    
                    <p className="text-xs font-black uppercase text-amber-400 tracking-widest mb-1">CHAMPIONSHIP GLORY</p>
                    <p className="text-3xl font-black italic uppercase tracking-tighter text-white drop-shadow-md">{lastAward?.winnerTeamName}</p>
                    <p className="text-[10px] text-amber-200/60 mt-2 font-mono uppercase tracking-widest">Season {lastAward?.season} Winners</p>
                </div>

                {/* Awards Cabinet */}
                <div className="space-y-3">
                    <h3 className="text-xs font-black uppercase tracking-widest text-slate-400 text-left pl-1">🏆 Season Accolades</h3>
                    
                    <div className="grid grid-cols-2 gap-3 pb-4 text-left">
                        {/* MVP (Player of the Season) */}
                        <div className="bg-slate-900 border border-slate-800 p-4 rounded-2xl flex flex-col justify-between">
                            <div>
                                <p className="text-[9px] font-black uppercase tracking-widest text-emerald-400 mb-2">⭐ Player of Season</p>
                                <p className="font-extrabold text-sm text-white leading-tight truncate">{lastAward?.mvp?.playerName || 'N/A'}</p>
                                <p className="text-[10px] text-slate-500 font-bold uppercase truncate mt-0.5">{lastAward?.mvp?.teamName || 'N/A'}</p>
                            </div>
                            <div className="mt-4 pt-2 border-t border-slate-800/50">
                                <span className="text-xs font-mono font-bold text-emerald-400">{lastAward?.mvp?.points || '0'} <span className="text-[9px] text-slate-500">Pts</span></span>
                            </div>
                        </div>

                        {/* Power Hitter of the Season */}
                        <div className="bg-slate-900 border border-slate-800 p-4 rounded-2xl flex flex-col justify-between">
                            <div>
                                <p className="text-[9px] font-black uppercase tracking-widest text-amber-400 mb-2 font-sans">🔥 Power Hitter</p>
                                <p className="font-extrabold text-sm text-white leading-tight truncate">{lastAward?.powerHitter?.playerName || 'N/A'}</p>
                                <p className="text-[10px] text-slate-500 font-bold uppercase truncate mt-0.5">{lastAward?.powerHitter?.teamName || 'N/A'}</p>
                            </div>
                            <div className="mt-4 pt-2 border-t border-slate-800/50">
                                <span className="text-xs font-mono font-bold text-amber-400">SR {lastAward?.powerHitter?.strikeRate || '0'}</span>
                            </div>
                        </div>

                        {/* Best Batter (Orange Cap) */}
                        <div className="bg-slate-900 border border-slate-800 p-4 rounded-2xl flex flex-col justify-between">
                            <div>
                                <p className="text-[9px] font-black uppercase tracking-widest text-blue-400 mb-2">🏏 Batter of Season</p>
                                <p className="font-extrabold text-sm text-white leading-tight truncate">{lastAward?.bestBatter.playerName}</p>
                                <p className="text-[10px] text-slate-500 font-bold uppercase truncate mt-0.5">{lastAward?.bestBatter.teamName}</p>
                            </div>
                            <div className="mt-4 pt-2 border-t border-slate-800/50">
                                <span className="text-xs font-mono font-bold text-blue-400">{lastAward?.bestBatter.runs} <span className="text-[9px] text-slate-500">Runs</span></span>
                            </div>
                        </div>

                        {/* Best Bowler (Purple Cap) */}
                        <div className="bg-slate-900 border border-slate-800 p-4 rounded-2xl flex flex-col justify-between">
                            <div>
                                <p className="text-[9px] font-black uppercase tracking-widest text-purple-400 mb-2">⚡ Bowler of Season</p>
                                <p className="font-extrabold text-sm text-white leading-tight truncate">{lastAward?.bestBowler.playerName}</p>
                                <p className="text-[10px] text-slate-500 font-bold uppercase truncate mt-0.5">{lastAward?.bestBowler.teamName}</p>
                            </div>
                            <div className="mt-4 pt-2 border-t border-slate-800/50">
                                <span className="text-xs font-mono font-bold text-purple-400">{lastAward?.bestBowler.wickets} <span className="text-[9px] text-slate-500">Wickets</span></span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div className="mt-6 space-y-4">
                {nextFormat ? (
                    <button 
                        onClick={() => handleFormatChange(nextFormat)} 
                        className="w-full bg-teal-500 text-slate-950 font-black py-5 px-10 text-lg rounded-3xl shadow-2xl uppercase italic tracking-tighter hover:bg-teal-400 transition-all active:scale-95"
                    >
                        Proceed to {nextFormat}
                    </button>
                ) : (
                    <button 
                        onClick={() => setView('retention')} 
                        className="w-full bg-teal-500 text-slate-950 font-black py-5 px-10 text-lg rounded-3xl shadow-2xl uppercase italic tracking-tighter hover:bg-teal-400 transition-all active:scale-95"
                    >
                        End Season & Retain Players
                    </button>
                )}
            </div>
        </div>
    );
};

export default EndOfFormatScreen;
