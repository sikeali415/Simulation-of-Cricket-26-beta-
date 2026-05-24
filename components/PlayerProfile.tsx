import React, { useState, useMemo } from 'react';
import { Player, Format } from '../types';
import { getRoleColor, getRoleFullName, aggregateStats, getPlayerBadges } from '../utils';

interface PlayerProfileProps {
    player: Player | null;
    onBack: () => void;
    initialFormat: Format;
}

const getTacticalWeakness = (player: Player) => {
    const hash = player.name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const bowlingTypes = [
        'Left-Arm Express Fast Bowlers',
        'Off-Spin Deliveries on Dusty Spin Tracks',
        'Slower Off-Cutter Swings',
        'Surgical In-swinging Yorkers',
        'Short-Pitch Bouncers on Quick Fast Tracks'
    ];
    return bowlingTypes[hash % bowlingTypes.length];
};

const getPlayerBio = (player: Player) => {
    const hash = player.name.split('').reduce((acc, char) => acc + char.charCodeAt(0), 0);
    const bios = [
        `Known for exceptional hand-eye coordination and tactical awareness, ${player.name} is a versatile asset in any squad. Their ability to read the pitch state early allows them to adapt gameplay pacing seamlessly under volatile match situations.`,
        `A natural leader and disciplined competitor, ${player.name} brings tremendous work ethic and tactical balance. Highly regarded by team scouts for delivering under high-pressure run chases and big match scenarios.`,
        `Rising through the franchise developmental ranks, ${player.name} has solidified their position with consistent high-grade performances. Possesses classical technique coupled with highly explosive boundary-clearing dynamics.`,
        `An elite tactician on the field, ${player.name}'s match preparation and mechanical consistency make them a reliable game changer. Scouts emphasize their high tactical cricket IQ and composure during death overs.`
    ];
    return bios[hash % bios.length];
};

const PlayerProfile: React.FC<PlayerProfileProps> = ({ player, onBack, initialFormat }) => {
    const [selectedFormat, setSelectedFormat] = useState<Format | 'Summary' | 'About'>('About');
    
    const summaryStats = useMemo(() => {
        if (!player) return null;
        
        const t20Formats = [Format.T20];
        const listAFormats = [Format.ODI];
        const fcFormats = [Format.SHIELD];
        
        const t20 = aggregateStats(player, t20Formats);
        const listA = aggregateStats(player, listAFormats);
        const fc = aggregateStats(player, fcFormats);
        const overall = aggregateStats(player, [...t20Formats, ...listAFormats, ...fcFormats]);

        return { t20, listA, fc, overall };
    }, [player]);

    if (!player || !summaryStats) return <div>Player not found. <button onClick={onBack}>Back</button></div>;
    
    const stats = (selectedFormat === 'Summary' || selectedFormat === 'About') 
        ? summaryStats.overall 
        : player.stats[selectedFormat];
    
    return (
        <div className="p-4 h-[calc(100vh-90px)] overflow-y-auto">
            <button onClick={onBack} className="mb-2 text-sm text-teal-500 hover:text-teal-400 transition-colors">&larr; Back to Stats</button>
            <div className="text-center mb-4">
                <h2 className="text-3xl font-bold">{player.name}</h2>
                <p className={`${getRoleColor(player.role)} font-semibold`}>{getRoleFullName(player.role)}</p>
                {player.teamName && <p className="text-sm text-gray-500">{player.teamName}</p>}
                
                {/* Badges Display */}
                <div className="flex flex-wrap gap-1.5 justify-center mt-3 max-w-md mx-auto">
                    {getPlayerBadges(player).map((badge, idx) => (
                        <span key={idx} className="px-2.5 py-1 text-[10px] font-bold rounded-full uppercase bg-gradient-to-r from-amber-500 to-yellow-600 text-slate-900 border border-amber-400 shadow-sm">
                            🏆 {badge}
                        </span>
                    ))}
                </div>
            </div>

            {/* Career Summary Table */}
            <div className="mb-6 overflow-x-auto">
                <h3 className="font-bold text-lg mb-2 text-center">Career Summary</h3>
                <table className="w-full text-xs text-center border-collapse">
                    <thead>
                        <tr className="bg-gray-200 dark:bg-gray-700/60 text-gray-700 dark:text-gray-300">
                            <th className="p-1.5 text-left rounded-l-lg">Format</th>
                            <th className="p-1.5">M</th>
                            <th className="p-1.5">Runs</th>
                            <th className="p-1.5">Avg</th>
                            <th className="p-1.5">SR</th>
                            <th className="p-1.5">Wkts</th>
                            <th className="p-1.5">Avg</th>
                            <th className="p-1.5 rounded-r-lg">Econ</th>
                        </tr>
                    </thead>
                    <tbody>
                         <tr className="border-b border-gray-100 dark:border-gray-700/50">
                            <td className="p-1.5 text-left font-semibold text-gray-700 dark:text-gray-300">First Class</td>
                            <td className="p-1.5">{summaryStats.fc.matches}</td>
                            <td className="p-1.5 font-bold">{summaryStats.fc.runs}</td>
                            <td className="p-1.5">{summaryStats.fc.average.toFixed(2)}</td>
                            <td className="p-1.5">{summaryStats.fc.strikeRate.toFixed(1)}</td>
                            <td className="p-1.5 font-bold">{summaryStats.fc.wickets}</td>
                            <td className="p-1.5">{summaryStats.fc.bowlingAverage.toFixed(2)}</td>
                            <td className="p-1.5">{summaryStats.fc.economy.toFixed(2)}</td>
                        </tr>
                        <tr className="border-b border-gray-100 dark:border-gray-700/50">
                            <td className="p-1.5 text-left font-semibold text-gray-700 dark:text-gray-300">List A</td>
                            <td className="p-1.5">{summaryStats.listA.matches}</td>
                            <td className="p-1.5 font-bold">{summaryStats.listA.runs}</td>
                            <td className="p-1.5">{summaryStats.listA.average.toFixed(2)}</td>
                            <td className="p-1.5">{summaryStats.listA.strikeRate.toFixed(1)}</td>
                            <td className="p-1.5 font-bold">{summaryStats.listA.wickets}</td>
                            <td className="p-1.5">{summaryStats.listA.bowlingAverage.toFixed(2)}</td>
                            <td className="p-1.5">{summaryStats.listA.economy.toFixed(2)}</td>
                        </tr>
                         <tr className="border-b border-gray-100 dark:border-gray-700/50">
                            <td className="p-1.5 text-left font-semibold text-gray-700 dark:text-gray-300">T20s</td>
                            <td className="p-1.5">{summaryStats.t20.matches}</td>
                            <td className="p-1.5 font-bold">{summaryStats.t20.runs}</td>
                            <td className="p-1.5">{summaryStats.t20.average.toFixed(2)}</td>
                            <td className="p-1.5">{summaryStats.t20.strikeRate.toFixed(1)}</td>
                            <td className="p-1.5 font-bold">{summaryStats.t20.wickets}</td>
                            <td className="p-1.5">{summaryStats.t20.bowlingAverage.toFixed(2)}</td>
                            <td className="p-1.5">{summaryStats.t20.economy.toFixed(2)}</td>
                        </tr>
                        <tr className="bg-teal-50 dark:bg-teal-900/10 font-bold border-t border-teal-200 dark:border-teal-900/40">
                            <td className="p-1.5 text-left text-teal-700 dark:text-teal-300 rounded-l-lg">Overall</td>
                            <td className="p-1.5">{summaryStats.overall.matches}</td>
                            <td className="p-1.5">{summaryStats.overall.runs}</td>
                            <td className="p-1.5">{summaryStats.overall.average.toFixed(2)}</td>
                            <td className="p-1.5">{summaryStats.overall.strikeRate.toFixed(1)}</td>
                            <td className="p-1.5">{summaryStats.overall.wickets}</td>
                            <td className="p-1.5">{summaryStats.overall.bowlingAverage.toFixed(2)}</td>
                            <td className="p-1.5 rounded-r-lg">{summaryStats.overall.economy.toFixed(2)}</td>
                        </tr>
                    </tbody>
                </table>
            </div>

            {/* Custom Tab selectors */}
            <div className="flex justify-start items-center overflow-x-auto border-b border-gray-300 dark:border-gray-700/80 mb-4 pb-1 gap-1.5 scrollbar-none">
                 {['About', 'Summary', ...Object.values(Format)].map(tab => (
                    <button 
                        key={tab} 
                        onClick={() => setSelectedFormat(tab as any)} 
                        className={`px-3.5 py-1.5 text-xs whitespace-nowrap font-semibold rounded-t-lg transition-all ${
                            selectedFormat === tab 
                                ? 'border-b-2 border-teal-500 text-teal-600 dark:text-teal-400 bg-teal-500/5' 
                                : 'text-gray-500 hover:text-gray-850 dark:hover:text-white'
                        }`}
                    >
                        {tab === 'Summary' ? 'Summary Statistics' : tab === 'About' ? 'Scouting & Bio' : tab}
                    </button>
                ))}
            </div>
            
            {/* About Profile Section */}
            {selectedFormat === 'About' && (
                <div className="space-y-4 animate-fadeIn">
                    {/* Bio paragraph */}
                    <div className="bg-gray-150 dark:bg-gray-800/40 p-4 rounded-xl border border-gray-200/50 dark:border-gray-700/40">
                        <h3 className="font-bold text-xs uppercase tracking-wider text-teal-600 dark:text-teal-400 mb-2">Immersive Biography</h3>
                        <p className="text-sm leading-relaxed text-gray-700 dark:text-gray-300">
                            {getPlayerBio(player)}
                        </p>
                    </div>

                    {/* Left/Right scouting blocks */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div className="bg-gray-150 dark:bg-gray-800/40 p-4 rounded-xl border border-gray-200/50 dark:border-gray-700/40">
                            <h3 className="font-bold text-xs uppercase tracking-wider text-teal-600 dark:text-teal-400 mb-3">Athletic Dossier</h3>
                            <div className="space-y-2 text-xs">
                                <div className="flex justify-between border-b border-gray-200/40 dark:border-gray-700/40 pb-1.5">
                                    <span className="text-gray-500">Nationality</span>
                                    <span className="font-semibold text-gray-900 dark:text-white">{player.nationality}</span>
                                </div>
                                <div className="flex justify-between border-b border-gray-200/40 dark:border-gray-700/40 pb-1.5">
                                    <span className="text-gray-500">Affiliation</span>
                                    <span className="font-semibold text-gray-900 dark:text-white">{player.isForeign ? '🌴 International Marquee' : '🏏 Domestic Class'}</span>
                                </div>
                                <div className="flex justify-between border-b border-gray-200/40 dark:border-gray-700/40 pb-1.5">
                                    <span className="text-gray-500">Tempo Role</span>
                                    <span className="font-semibold text-gray-900 dark:text-white">
                                        {player.style === 'A' ? 'Aggressive Starter' : player.style === 'D' ? 'Defensive Anchor' : player.style === 'NA' ? 'Highly Aggressive (Blitzkrieg)' : 'Balanced Tempo'}
                                    </span>
                                </div>
                                <div className="flex justify-between border-b border-gray-200/40 dark:border-gray-700/40 pb-1.5">
                                    <span className="text-gray-500">Base Bid Price</span>
                                    <span className="font-semibold text-amber-500">{player.basePrice ? `${player.basePrice.toFixed(2)} Crore` : 'N/A'}</span>
                                </div>
                                <div className="flex justify-between pb-1">
                                    <span className="text-gray-500">Fitness Condition</span>
                                    <span className={`font-semibold ${player.injury ? 'text-red-500' : 'text-emerald-500'}`}>
                                        {player.injury ? `Injured (${player.injury.text})` : '100% Match Fit'}
                                    </span>
                                </div>
                            </div>
                        </div>

                        {/* Tactical Scouting Card */}
                        <div className="bg-gray-150 dark:bg-gray-800/40 p-4 rounded-xl border border-gray-200/50 dark:border-gray-700/40">
                            <h3 className="font-bold text-xs uppercase tracking-wider text-teal-600 dark:text-teal-400 mb-3">Scouting Briefing</h3>
                            <div className="space-y-3 text-xs">
                                <div>
                                    <span className="text-gray-500 block mb-1">🎯 Core Tactical Strength:</span>
                                    <p className="font-semibold text-gray-950 dark:text-gray-150 bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1.5 rounded-lg text-[11px] leading-relaxed">
                                        {player.battingSkill > player.secondarySkill 
                                            ? `Elite batting caliber: Rated at ${player.battingSkill}/100 with advanced boundary placement techniques and timing mechanics.` 
                                            : `Specialist tactical bowler: Rated at ${player.secondarySkill}/100, skilled at delivering precision spells and inducing wickets.`}
                                    </p>
                                </div>
                                <div>
                                    <span className="text-gray-500 block mb-1">⚠️ Major Strategic Vulnerability:</span>
                                    <p className="font-semibold text-orange-600 dark:text-orange-400 bg-orange-500/10 border border-orange-500/20 px-2.5 py-1.5 rounded-lg text-[11px] leading-relaxed">
                                        Vulnerable against {getTacticalWeakness(player)}. Telemetry advises limiting exposure in such formats.
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Advisor notes */}
                    <div className="bg-gradient-to-r from-teal-500/10 to-teal-900/10 border border-teal-500/20 p-4 rounded-xl">
                        <span className="text-[10px] font-bold text-teal-600 dark:text-teal-400 uppercase tracking-widest block mb-1">📋 Coordinator Advisory Notes</span>
                        <p className="text-xs italic text-gray-700 dark:text-gray-300 leading-relaxed font-mono">
                            "Scouting telemetry reveals that {player.name} reacts exceptionally well under balanced tactical regimes. We recommend slotting them into comfortable orders to build momentum early."
                        </p>
                    </div>
                </div>
            )}

            {selectedFormat !== 'About' && (
            <div className="bg-gray-100 dark:bg-gray-800/50 p-4 rounded-lg">
                <h3 className="font-bold text-lg mb-2 text-center">{selectedFormat} Details</h3>
                <div className="grid grid-cols-3 gap-2 text-center">
                    <div><p className="font-bold text-xl">{stats.matches}</p><p className="text-xs text-gray-500">Matches</p></div>
                    <div><p className="font-bold text-xl">{stats.runs}</p><p className="text-xs text-gray-500">Runs</p></div>
                    <div><p className="font-bold text-xl">{stats.highestScore}</p><p className="text-xs text-gray-500">Highest</p></div>
                    <div><p className="font-bold text-xl">{stats.average.toFixed(2)}</p><p className="text-xs text-gray-500">Average</p></div>
                    <div><p className="font-bold text-xl">{stats.strikeRate.toFixed(2)}</p><p className="text-xs text-gray-500">Strike Rate</p></div>
                    <div><p className="font-bold text-xl">{stats.fifties}</p><p className="text-xs text-gray-500">50s</p></div>
                    <div><p className="font-bold text-xl">{stats.hundreds}</p><p className="text-xs text-gray-500">100s</p></div>
                    <div><p className="font-bold text-xl">{stats.fours}</p><p className="text-xs text-gray-500">Fours</p></div>
                    <div><p className="font-bold text-xl">{stats.sixes}</p><p className="text-xs text-gray-500">Sixes</p></div>
                </div>
                 <h3 className="font-bold text-lg mt-4 mb-2 text-center">Bowling Stats</h3>
                <div className="grid grid-cols-3 gap-2 text-center">
                    <div><p className="font-bold text-xl">{stats.wickets}</p><p className="text-xs text-gray-500">Wickets</p></div>
                    <div><p className="font-bold text-xl">{stats.bowlingAverage.toFixed(2)}</p><p className="text-xs text-gray-500">Average</p></div>
                    <div><p className="font-bold text-xl">{stats.economy.toFixed(2)}</p><p className="text-xs text-gray-500">Economy</p></div>
                    <div><p className="font-bold text-xl">{stats.bestBowling}</p><p className="text-xs text-gray-500">Best</p></div>
                    <div><p className="font-bold text-xl">{stats.threeWicketHauls}</p><p className="text-xs text-gray-500">3-fers</p></div>
                    <div><p className="font-bold text-xl">{stats.fiveWicketHauls}</p><p className="text-xs text-gray-500">5-fers</p></div>
                </div>
                 <h3 className="font-bold text-lg mt-4 mb-2 text-center">Milestones</h3>
                <div className="grid grid-cols-2 gap-2 text-center">
                    <div><p className="font-bold text-xl">{stats.fastestFifty > 0 ? `${stats.fastestFifty}` : '-'}</p><p className="text-xs text-gray-500">Fastest 50 (balls)</p></div>
                    <div><p className="font-bold text-xl">{stats.fastestHundred > 0 ? `${stats.fastestHundred}` : '-'}</p><p className="text-xs text-gray-500">Fastest 100 (balls)</p></div>
                </div>

                {/* Phase Stats (T20 and One-Day) */}
                {selectedFormat !== 'Summary' && (selectedFormat.includes('T20') || selectedFormat.includes('One-Day') || selectedFormat.includes('Cup')) && stats.phaseStats && (
                    <div className="mt-6 border-t border-gray-200 dark:border-gray-700 pt-4">
                        <h3 className="font-bold text-lg mb-3 text-center text-teal-600 dark:text-teal-400">Phase-wise Performance</h3>
                        
                        <div className="space-y-4">
                            {/* Batting Phases */}
                            <div className="bg-white dark:bg-gray-900/40 p-3 rounded-lg border border-gray-100 dark:border-gray-800">
                                <h4 className="font-bold text-xs uppercase tracking-wider text-left text-gray-500 mb-2">Batting Phase Records</h4>
                                <table className="w-full text-xs">
                                    <thead>
                                        <tr className="bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300">
                                            <th className="p-1.5 text-left">Phase</th>
                                            <th className="p-1.5 text-center">Runs</th>
                                            <th className="p-1.5 text-center">Balls</th>
                                            <th className="p-1.5 text-center">SR</th>
                                            <th className="p-1.5 text-center">Outs</th>
                                            <th className="p-1.5 text-center">Avg</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-150 dark:divide-gray-800">
                                        {[
                                            { code: 'pp', name: 'Powerplay (PP)' },
                                            { code: 'mo', name: 'Middle Overs (MO)' },
                                            { code: 'do', name: 'Death Overs (DO)' }
                                        ].map(ph => {
                                            // @ts-ignore
                                            const st = stats.phaseStats.batting?.[ph.code] || { runs: 0, balls: 0, dismissals: 0 };
                                            const sr = st.balls > 0 ? ((st.runs / st.balls) * 100).toFixed(1) : '0.0';
                                            const avg = st.dismissals > 0 ? (st.runs / st.dismissals).toFixed(1) : st.runs > 0 ? st.runs.toFixed(1) : '-';
                                            return (
                                                <tr key={ph.code}>
                                                    <td className="p-1.5 font-semibold text-left">{ph.name}</td>
                                                    <td className="p-1.5 text-center font-bold">{st.runs}</td>
                                                    <td className="p-1.5 text-center text-gray-500">{st.balls}</td>
                                                    <td className="p-1.5 text-center text-amber-600 dark:text-amber-400 font-medium">{sr}</td>
                                                    <td className="p-1.5 text-center text-gray-500">{st.dismissals}</td>
                                                    <td className="p-1.5 text-center font-semibold">{avg}</td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>

                            {/* Bowling Phases */}
                            <div className="bg-white dark:bg-gray-900/40 p-3 rounded-lg border border-gray-100 dark:border-gray-800">
                                <h4 className="font-bold text-xs uppercase tracking-wider text-left text-gray-500 mb-2">Bowling Phase Records</h4>
                                <table className="w-full text-xs">
                                    <thead>
                                        <tr className="bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300">
                                            <th className="p-1.5 text-left">Phase</th>
                                            <th className="p-1.5 text-center">Wkts</th>
                                            <th className="p-1.5 text-center">Runs</th>
                                            <th className="p-1.5 text-center">Balls</th>
                                            <th className="p-1.5 text-center">Econ</th>
                                            <th className="p-1.5 text-center">SR</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100 dark:divide-gray-800">
                                        {[
                                            { code: 'pp', name: 'Powerplay (PP)' },
                                            { code: 'mo', name: 'Middle Overs (MO)' },
                                            { code: 'do', name: 'Death Overs (DO)' }
                                        ].map(ph => {
                                            // @ts-ignore
                                            const st = stats.phaseStats.bowling?.[ph.code] || { wickets: 0, runsConceded: 0, ballsBowled: 0 };
                                            const econ = st.ballsBowled > 0 ? ((st.runsConceded / st.ballsBowled) * 6).toFixed(2) : '0.00';
                                            const sr = st.wickets > 0 ? (st.ballsBowled / st.wickets).toFixed(1) : '-';
                                            return (
                                                <tr key={ph.code}>
                                                    <td className="p-1.5 font-semibold text-left">{ph.name}</td>
                                                    <td className="p-1.5 text-center font-bold text-teal-600 dark:text-teal-400">{st.wickets}</td>
                                                    <td className="p-1.5 text-center text-gray-500">{st.runsConceded}</td>
                                                    <td className="p-1.5 text-center text-gray-500">{st.ballsBowled}</td>
                                                    <td className="p-1.5 text-center font-medium">{econ}</td>
                                                    <td className="p-1.5 text-center text-gray-500">{sr}</td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                )}

                {/* Batting Position Stats */}
                {stats.positionStats && Object.keys(stats.positionStats).length > 0 && (
                    <div className="mt-6 border-t border-gray-200 dark:border-gray-700 pt-4">
                        <h3 className="font-bold text-lg mb-3 text-center text-teal-600 dark:text-teal-400">Batting by Position (All Formats)</h3>
                        <div className="bg-white dark:bg-gray-900/40 p-3 rounded-lg border border-gray-100 dark:border-gray-800 overflow-x-auto">
                            <table className="w-full text-xs">
                                <thead>
                                    <tr className="bg-gray-100 dark:bg-gray-800 text-gray-700 dark:text-gray-300">
                                        <th className="p-1.5 text-left">Position</th>
                                        <th className="p-1.5 text-center">Innings</th>
                                        <th className="p-1.5 text-center">Runs</th>
                                        <th className="p-1.5 text-center">Average</th>
                                        <th className="p-1.5 text-center">SR</th>
                                        <th className="p-1.5 text-center">30s / 50s / 100s</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-150 dark:divide-gray-800">
                                    {Object.entries(stats.positionStats)
                                        .map(([posStr, st]) => ({ pos: Number(posStr), ...(st as any) }))
                                        .filter(p => p.innings > 0)
                                        .sort((a, b) => a.pos - b.pos)
                                        .map(p => {
                                            const avg = p.dismissals > 0 ? (p.runs / p.dismissals).toFixed(1) : p.runs > 0 ? p.runs.toFixed(1) : '-';
                                            const sr = p.balls > 0 ? ((p.runs / p.balls) * 100).toFixed(1) : '0.0';
                                            const label = p.pos === 1 || p.pos === 2 ? `#${p.pos} Opener` : p.pos === 3 ? `#3 One Down` : `#${p.pos}`;
                                            return (
                                                <tr key={p.pos}>
                                                    <td className="p-1.5 font-semibold text-left text-gray-700 dark:text-gray-300">{label}</td>
                                                    <td className="p-1.5 text-center">{p.innings}</td>
                                                    <td className="p-1.5 text-center font-bold text-teal-600 dark:text-teal-400">{p.runs}</td>
                                                    <td className="p-1.5 text-center font-semibold">{avg}</td>
                                                    <td className="p-1.5 text-center text-gray-500">{sr}</td>
                                                    <td className="p-1.5 text-center text-gray-500 font-medium">
                                                        {p.thirties || 0} / {p.fifties || 0} / {p.hundreds || 0}
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    {Object.values(stats.positionStats).filter((st: any) => st.innings > 0).length === 0 && (
                                        <tr>
                                            <td colSpan={6} className="p-4 text-center text-gray-550 italic">No innings recorded at any position yet.</td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}
            </div>
            )}
        </div>
    );
};

export default PlayerProfile;
