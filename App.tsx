import React, { useState, useEffect } from 'react';
import { AppState, GameData, Team, Format, MatchResult, Standing, Player } from './types';
import { PLAYERS, TEAMS, GROUNDS, PRE_BUILT_SQUADS, INITIAL_SPONSORSHIPS, INITIAL_NEWS } from './data';
import { LoadingSpinner, generateLeagueSchedule } from './utils';
import { useFirebase } from './components/FirebaseProvider';
import { saveGameToFirebase, signIn, signOutUser, getSaves } from './services/firebase';

// Components
import MainMenu from './components/MainMenu';
import TeamSelection from './components/TeamSelection';
import CareerHub from './components/CareerHub';
import AuctionRoom from './components/AuctionRoom';

export const MAX_SQUAD_SIZE = 22;
export const MIN_SQUAD_SIZE = 15;
export const MAX_FOREIGN_PLAYERS = 3;

export const App = () => {
  const [appState, setAppState] = useState<AppState>('MAIN_MENU');
  const [gameData, setGameData] = useState<GameData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [theme, setTheme] = useState<'light' | 'dark'>('dark');
  const [feedbackMessage, setFeedbackMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);
  const [hasSaveData, setHasSaveData] = useState(false);
  const [deferredPrompt, setDeferredPrompt] = useState<any>(null);
  const [showInstallBtn, setShowInstallBtn] = useState(false);
  const { user, loading: firebaseLoading } = useFirebase();
  const [authError, setAuthError] = useState<{ title: string; message: string; copyText?: string } | null>(null);

  const handleSignIn = async () => {
    showFeedback("Opening Sign-In...");
    try {
      const u = await signIn();
      if (u) {
        showFeedback(`Signed in as ${u.displayName || 'Manager'}!`);
      }
    } catch (err: any) {
      console.error("Auth error caught in App.tsx:", err);
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

  useEffect(() => {
    const handler = (e: Event) => {
      e.preventDefault();
      setDeferredPrompt(e);
      setShowInstallBtn(true);
    };
    window.addEventListener('beforeinstallprompt', handler);
    return () => window.removeEventListener('beforeinstallprompt', handler);
  }, []);

  const handleInstallClick = async () => {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const { outcome } = await deferredPrompt.userChoice;
    if (outcome === 'accepted') {
      showFeedback("Thank you for installing Sike's Cricket Manager!");
    }
    setDeferredPrompt(null);
    setShowInstallBtn(false);
  };

  useEffect(() => {
    const savedTheme = localStorage.getItem('cricketManagerTheme') || 'dark';
    setTheme(savedTheme as 'light' | 'dark');
    const savedGame = localStorage.getItem('cricketManagerSave');
    if (savedGame) {
        setHasSaveData(true);
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    if (theme === 'light') {
        document.documentElement.classList.remove('dark');
    } else {
        document.documentElement.classList.add('dark');
    }
    localStorage.setItem('cricketManagerTheme', theme);
  }, [theme]);

  useEffect(() => {
    if (gameData && !isLoading) {
      localStorage.setItem('cricketManagerSave', JSON.stringify(gameData));
      setHasSaveData(true);
      
      // Auto-save to cloud if logged in
      if (user) {
          saveGameToFirebase(user.uid, 'autosave', 'Auto Save', gameData).catch(console.error);
      }
    }
  }, [gameData, isLoading, user]);

  const showFeedback = (text: string, type: 'success' | 'error' = 'success') => {
    setFeedbackMessage({ text, type });
    setTimeout(() => setFeedbackMessage(null), 2500);
  };

  const saveGame = async () => {
    if (!gameData) return;
    if (user) {
        showFeedback("Syncing with cloud...");
        await saveGameToFirebase(user.uid, 'autosave', 'Auto Save', gameData);
        showFeedback("Progress synced to cloud!");
    } else {
        showFeedback("Progress is saved locally! Sign in for cloud backup.");
    }
  };

  const loadGame = async () => {
    if (window.confirm("Loading will overwrite current progress. Continue?")) {
        if (user) {
            showFeedback("Fetching cloud saves...");
            const cloudSaves = await getSaves(user.uid);
            if (cloudSaves.length > 0) {
                // Find latest autosave
                const latest = cloudSaves.find((s: any) => s.id === 'autosave') || cloudSaves[0];
                setGameData(latest.data);
                showFeedback("Cloud Save Loaded!", "success");
                setAppState('CAREER_HUB');
                return;
            }
        }
        
        const savedGame = localStorage.getItem('cricketManagerSave');
        if (savedGame) {
            try {
                setGameData(JSON.parse(savedGame));
                showFeedback("Game Loaded!", "success");
                setAppState('CAREER_HUB');
            } catch (e) {
                console.error("Failed to parse saved game data during load:", e);
                localStorage.removeItem('cricketManagerSave');
                setHasSaveData(false);
                showFeedback("Failed to load saved game. It may be corrupt.", "error");
            }
        } else {
            showFeedback("No saved game found.", "error");
        }
    }
  };

  const resumeGame = () => {
    const savedGame = localStorage.getItem('cricketManagerSave');
    if (savedGame) {
        try {
            setGameData(JSON.parse(savedGame));
            setAppState('CAREER_HUB');
            showFeedback("Game Resumed!", "success");
        } catch(e) {
            console.error("Failed to parse saved game data:", e);
            localStorage.removeItem('cricketManagerSave');
            setHasSaveData(false);
            showFeedback("Failed to load saved game. It may be corrupt.", "error");
        }
    }
  };

  const handleStartNewGame = () => {
    if (hasSaveData && !window.confirm("Starting a new game will overwrite your saved progress. Are you sure?")) {
        return;
    }
    setAppState('TEAM_SELECTION');
  };

  const initializeNewGame = (userTeamId: string) => {
    setIsLoading(true);
    // Hard clear of any previous sessions to ensure fresh start
    localStorage.removeItem('cricketManagerSave');
    
    const allPlayersPool = [...PLAYERS].sort(() => Math.random() - 0.5);
    const initialTeamsData = [...TEAMS];
    const usedPlayerIds = new Set<string>();

    const initialTeams: Team[] = initialTeamsData.map(teamData => {
        // For a meaningful auction, we only retain a few core players (e.g., 4)
        const targetRetainedSize = 4;
        
        let squad: Player[] = [];
        // 1. Try to use pre-built if available (first 4)
        const preBuiltIds = (PRE_BUILT_SQUADS[teamData.id] || []).slice(0, targetRetainedSize);
        preBuiltIds.forEach(pid => {
            const p = PLAYERS.find(pl => pl.id === pid);
            if (p && !usedPlayerIds.has(pid)) {
                squad.push(JSON.parse(JSON.stringify(p)));
                usedPlayerIds.add(pid);
            }
        });

        // 2. If for some reason we don't have enough, fill to 4
        while (squad.length < targetRetainedSize) {
            const leftoverIndex = allPlayersPool.findIndex(p => !usedPlayerIds.has(p.id));
            if (leftoverIndex !== -1) {
                const p = allPlayersPool[leftoverIndex];
                squad.push(JSON.parse(JSON.stringify(p)));
                usedPlayerIds.add(p.id);
            } else {
                break;
            }
        }

        return { id: teamData.id, name: teamData.name, squad, captains: {}, purse: 100.0, firstAidKits: 1 };
    });

    const initialStandings = (teams: Team[]) => teams.map(team => ({ 
        teamId: team.id, teamName: team.name, played: 0, won: 0, lost: 0, drawn: 0, points: 0, netRunRate: 0, runsFor: 0, runsAgainst: 0 
    }));

    const schedules = {
        [Format.T20]: generateLeagueSchedule(initialTeams, Format.T20, true),
        [Format.ODI]: generateLeagueSchedule(initialTeams, Format.ODI, true),
        [Format.SHIELD]: generateLeagueSchedule(initialTeams, Format.SHIELD, true),
    };

    const newGameData: GameData = {
      userTeamId,
      teams: initialTeams,
      grounds: [...GROUNDS],
      allTeamsData: initialTeamsData,
      allPlayers: [...PLAYERS],
      schedule: schedules,
      currentMatchIndex: {
        [Format.T20]: 0,
        [Format.ODI]: 0,
        [Format.SHIELD]: 0,
      },
      standings: {
        [Format.T20]: initialStandings(initialTeams),
        [Format.ODI]: initialStandings(initialTeams),
        [Format.SHIELD]: initialStandings(initialTeams),
      },
      matchResults: Object.values(Format).reduce((acc, format) => {
        acc[format] = [];
        return acc;
      }, {} as Record<Format, MatchResult[]>),
      playingXIs: {},
      currentSeason: 1,
      currentFormat: Format.T20, 
      awardsHistory: [],
      scoreLimits: {},
      records: {
        batterVsBowler: [],
        teamVsTeam: [],
        playerVsTeam: [],
      },
      promotionHistory: [],
      popularity: 50,
      sponsorships: INITIAL_SPONSORSHIPS,
      news: INITIAL_NEWS,
      activeMatch: null,
      settings: {
          isDoubleRoundRobin: true
      }
    };
    setGameData(newGameData);
    setAppState('AUCTION');
    setIsLoading(false);
  };

  const handleAuctionComplete = (finalTeams: Team[]) => {
      setGameData(prev => {
          if (!prev) return null;
          return { ...prev, teams: finalTeams };
      });
      setAppState('CAREER_HUB');
      showFeedback("Draft Room Closed! Ready for Match 1.", "success");
  };

  const resetGame = () => {
      if (window.confirm("WARNING: This will PERMANENTLY DELETE all your progress, including career records, stats, and saved drafts. Are you sure?")) {
          localStorage.removeItem('cricketManagerSave');
          localStorage.removeItem('cricketManagerTheme'); // Optional: reset theme too
          setGameData(null);
          setHasSaveData(false);
          setAppState('MAIN_MENU');
          window.location.reload(); // Hard refresh to ensure all states are clean
      }
  };

  const renderContent = () => {
    if (isLoading) {
        return <div className="bg-white dark:bg-gray-900 h-full flex items-center justify-center"><LoadingSpinner /></div>;
    }
    switch(appState) {
        case 'MAIN_MENU': return <MainMenu onStartNewGame={handleStartNewGame} onResumeGame={resumeGame} hasSaveData={hasSaveData} user={user} onSignIn={handleSignIn} onSignOut={signOutUser} />;
        case 'TEAM_SELECTION': return <TeamSelection onTeamSelected={initializeNewGame} theme={theme} />;
        case 'AUCTION': return gameData ? <AuctionRoom gameData={gameData} onAuctionComplete={handleAuctionComplete} /> : null;
        case 'CAREER_HUB': return gameData ? <CareerHub gameData={gameData} setGameData={setGameData} onResetGame={resetGame} theme={theme} setTheme={setTheme} saveGame={saveGame} loadGame={loadGame} showFeedback={showFeedback} /> : null;
        default: return <div>Error</div>;
    }
  }

  return (
    <div className="bg-gray-100 dark:bg-gray-900 min-h-screen flex items-center justify-center font-sans">
      <div className="w-full max-w-md h-screen max-h-[932px] bg-gray-50 dark:bg-[#2C3531] border-4 border-gray-300 dark:border-gray-700 rounded-[60px] shadow-2xl shadow-black/50 overflow-hidden relative text-gray-900 dark:text-gray-200 flex flex-col">
        {showInstallBtn && (
          <div className="bg-teal-600 dark:bg-teal-700 text-white p-3 text-xs flex justify-between items-center relative z-50 animate-pulse">
            <span className="font-bold">📱 Play Offline: Install App!</span>
            <button
              onClick={handleInstallClick}
              className="bg-white dark:bg-gray-900 text-teal-600 dark:text-teal-400 font-extrabold px-3 py-1 rounded shadow hover:bg-teal-50 dark:hover:bg-gray-800 transition-colors"
            >
              INSTALL
            </button>
          </div>
        )}
        {renderContent()}
        {feedbackMessage && (
            <div className={`absolute bottom-28 left-1/2 -translate-x-1/2 px-4 py-2 rounded-lg z-50 shadow-lg text-white font-semibold ${feedbackMessage.type === 'success' ? 'bg-green-500' : 'bg-red-500'}`}>
                {feedbackMessage.text}
            </div>
        )}
        {authError && (
          <div className="absolute inset-0 bg-black/80 flex items-center justify-center p-6 z-[100] animate-fade-in">
            <div className="bg-slate-900 border border-slate-800 p-6 rounded-3xl w-full max-w-sm text-center space-y-4 shadow-2xl relative text-white">
              <div className="text-4xl">⚠️</div>
              <h3 className="text-lg font-black text-rose-400 uppercase tracking-tight">{authError.title}</h3>
              <p className="text-xs text-slate-300 leading-relaxed text-left whitespace-pre-line bg-slate-950/50 p-3.5 rounded-xl border border-white/5 font-mono max-h-[195px] overflow-y-auto">
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
    </div>
  );
};