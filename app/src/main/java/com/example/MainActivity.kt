package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.*
import com.example.ui.CricketViewModel
import com.example.ui.LeagueStanding
import com.example.ui.ScheduledMatch
import com.example.ui.components.StadiumCanvas
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    CricketManagerAppLayout(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CricketManagerAppLayout(modifier: Modifier = Modifier) {
    val viewModel: CricketViewModel = viewModel()
    var selectedTab by remember { mutableStateOf(0) }
    var activeDetailPlayer by remember { mutableStateOf<Player?>(null) }

    val userTeam = viewModel.userTeam
    val userTeamColor = remember(userTeam.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(userTeam.colorHex))
        } catch (e: Exception) {
            Color(0xFF8B5CF6) // purple default
        }
    }

    val matchStage by viewModel.matchStage.collectAsState()
    val activeFormat by viewModel.activeFormat.collectAsState()

    // Automatically switch tabs if a match goes active
    LaunchedEffect(matchStage) {
        if (matchStage == MatchStage.PLAYING || matchStage == MatchStage.INNINGS_BREAK) {
            selectedTab = 4 // Simulation screen tab
        }
    }

    Column(modifier = modifier.background(MaterialTheme.colorScheme.background)) {
        // --- CUSTOMIZABLE SPORTS OVERLAY TELEVIEW BANNER ---
        val tvChannel by viewModel.selectedTVChannel.collectAsState()
        val tvColor = when (tvChannel) {
            "PrimeCast Ultra" -> Color(0xFFE91E63)
            "Roar Sports Live" -> Color(0xFFD50000)
            "CricketNow HD" -> Color(0xFF29B6F6)
            else -> Color(0xFF00E676)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(userTeamColor.copy(alpha = 0.4f), MaterialTheme.colorScheme.background)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = userTeam.flagEmoji,
                            fontSize = 28.sp
                        )
                        Column {
                            Text(
                                text = "CRICKET MANAGER 26",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = userTeamColor,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${userTeam.name} Franchises Club • Season Round ${viewModel.currentRound.collectAsState().value}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Sports Channel Live overlay Badge
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, tvColor),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Text(
                            text = tvChannel,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Main Navigation Scrollable Tab Rows to avoid squishing
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = userTeamColor,
            edgePadding = 12.dp,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Dashboard & League", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Lineups & Rules", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Transfers & County", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Branding Customizer", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 4,
                onClick = { selectedTab = 4 },
                text = { Text("Match Center", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Screen viewports switcher
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> LeagueDashboardScreen(viewModel, userTeamColor) { selectedTab = 4 }
                1 -> LineupsScreen(viewModel, userTeamColor, onPlayerClick = { activeDetailPlayer = it })
                2 -> TransfersScreen(viewModel, userTeamColor, onPlayerClick = { activeDetailPlayer = it })
                3 -> CustomizerScreen(viewModel, userTeamColor)
                4 -> MatchCenterScreen(viewModel, userTeamColor)
            }
        }

        activeDetailPlayer?.let { player ->
            PlayerProfileDialog(
                player = player,
                themeColor = userTeamColor,
                onDismiss = { activeDetailPlayer = null }
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 0: DASHBOARD & LEAGUE SCHEDULES
// -------------------------------------------------------------
@Composable
fun LeagueDashboardScreen(
    viewModel: CricketViewModel,
    themeColor: Color,
    onNavigateToMatch: () -> Unit
) {
    val activeFormat by viewModel.activeFormat.collectAsState()
    val scheduleMatches by viewModel.scheduleMatches.collectAsState()
    val currentRound by viewModel.currentRound.collectAsState()
    val standings by viewModel.standings.collectAsState()
    val userTeamId by viewModel.userTeamId.collectAsState()

    val nextUserMatch = remember(scheduleMatches, currentRound, userTeamId) {
        scheduleMatches.firstOrNull { match ->
            (match.homeTeamId == userTeamId || match.awayTeamId == userTeamId) &&
                    match.round >= currentRound && !match.isPlayed
        }
    }

    val teamsList by viewModel.teamsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Format Switcher Row
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "🏆 Select Active League Format",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MatchFormat.values().forEach { fmt ->
                        val isSel = activeFormat == fmt
                        OutlinedButton(
                            onClick = { viewModel.changeFormat(fmt) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = if (isSel) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = themeColor.copy(alpha = 0.2f),
                                    contentColor = themeColor
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            },
                            border = BorderStroke(1.dp, if (isSel) themeColor else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(fmt.displayName.replace("Cup", "").replace("Shield", "").trim(), fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Next Match Preview banner
        if (nextUserMatch != null) {
            val opponentId = if (nextUserMatch.homeTeamId == userTeamId) nextUserMatch.awayTeamId else nextUserMatch.homeTeamId
            val opponentObj = teamsList.firstOrNull { it.id == opponentId } ?: teamsList[0]

            Card(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, themeColor),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚔️ NEXT MATCH PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = themeColor
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(themeColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Round ${nextUserMatch.round}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(viewModel.userTeam.flagEmoji + " " + viewModel.userTeam.abbreviation, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("vs", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(opponentObj.flagEmoji + " " + opponentObj.abbreviation, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Format: ${activeFormat.displayName} (${activeFormat.maxOvers} Overs Limit • ${activeFormat.maxOversPerBowler} Overs/Bowler Cap)",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.loadMatchup(opponentId, activeFormat.maxOvers)
                                onNavigateToMatch()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, "Play", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play Match", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.quickSimulateMatch(nextUserMatch)
                                viewModel.recalculateStandings()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, "Sim", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quick Sim", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Standings Table
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "📊 League Standings",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )

                // Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Team", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(3f))
                    Text("P", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("W", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("L", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("PTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColor, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("NRR", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                }

                HorizontalDivider()

                standings.forEachIndexed { idx, row ->
                    val isUserTeam = row.teamId == userTeamId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isUserTeam) themeColor.copy(alpha = 0.12f) else Color.Transparent)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(3f), verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx + 1}. ", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(row.flagEmoji, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                row.teamName,
                                fontSize = 12.sp,
                                fontWeight = if (isUserTeam) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("${row.played}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("${row.won}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("${row.lost}", fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(
                            "${row.points}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColor,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text("%.3f".format(row.netRunRate), fontSize = 11.sp, modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                    }
                }
            }
        }

        // Full Fixtures list for active round
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 Round $currentRound Fixtures",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )

                    Button(
                        onClick = { viewModel.simulateRemainingMatchesForRound() },
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("Sim Round", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }

                scheduleMatches.filter { it.round == currentRound }.forEach { match ->
                    val hObj = teamsList.firstOrNull { it.id == match.homeTeamId } ?: teamsList[0]
                    val aObj = teamsList.firstOrNull { it.id == match.awayTeamId } ?: teamsList[0]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.5f)) {
                            Text(hObj.flagEmoji + " " + hObj.abbreviation, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            if (match.isPlayed) {
                                Text("${match.homeRuns}/${match.homeWickets}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Text(
                            "vs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(0.5f),
                            textAlign = TextAlign.Center
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1.5f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (match.isPlayed) {
                                Text("${match.awayRuns}/${match.awayWickets}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(aObj.abbreviation + " " + aObj.flagEmoji, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Result line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (match.isPlayed) match.resultText else "Scheduled Format: ${match.format}",
                            fontSize = 10.sp,
                            color = if (match.isPlayed) themeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = if (match.isPlayed) androidx.compose.ui.text.font.FontStyle.Normal else androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = if (match.isPlayed) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: SQUAD LINEUPS & LAWS VALIDATION
// -------------------------------------------------------------
@Composable
fun LineupsScreen(viewModel: CricketViewModel, themeColor: Color, onPlayerClick: (Player) -> Unit) {
    val userTeam = viewModel.userTeam
    var playingXIForeignCount = userTeam.players.take(11).count { it.isForeign }

    val squadValidation = viewModel.validateSquadRoster(userTeam.players)
    val lineupValidation = viewModel.validatePlayingXILineup(userTeam.players.take(11))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🛡️ Squad Lineups & Roster Limits",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor
        )

        // Rule Board Alert cards
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (squadValidation != null || lineupValidation != null) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    Color(0xFFE8F5E9)
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (squadValidation != null || lineupValidation != null) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = "Alert",
                    tint = if (squadValidation != null || lineupValidation != null) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                )
                Column {
                    Text(
                        text = if (squadValidation != null || lineupValidation != null) {
                            "Warning: Squad is ineligible! Correct issues before match play."
                        } else {
                            "Squad Compliant! Roster meets all league specifications."
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (squadValidation != null || lineupValidation != null) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            Color(0xFF1B5E20)
                        },
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = squadValidation ?: lineupValidation ?: "• Squad Size: ${userTeam.players.size}/21\n• Local Pakistani Players: ${userTeam.players.count { !it.isForeign }} (Min 12)\n• Playing XI Foreigners: $playingXIForeignCount (Limit 2 to 4)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Interactive List of Active XI
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🏏 Active Playing XI (First 11 players)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )

                HorizontalDivider()

                userTeam.players.take(11).forEachIndexed { index, p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayerClick(p) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(2f)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(themeColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColor)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            if (p.isForeign) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE53935))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("OS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Style chips
                        Text(
                            text = p.role.name.replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.weight(1f)
                        )

                        // Core skills
                        Text(
                            text = "BAT: ${p.battingSkill}  BOWL: ${p.bowlingSkill}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeColor
                        )
                    }
                }
            }
        }

        // Squad Bench Players (Remaining)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🛋️ Bench Players",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )

                HorizontalDivider()

                val bench = userTeam.players.drop(11)
                if (bench.isEmpty()) {
                    Text("No bench players currently in squad.", fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    bench.forEachIndexed { idx, p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayerClick(p) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(p.role.name, fontSize = 10.sp)
                            Text("BAT: ${p.battingSkill} • BOWL: ${p.bowlingSkill}", fontSize = 11.sp, color = themeColor)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: TRANSFERS & COUNTY POOL
// -------------------------------------------------------------
@Composable
fun TransfersScreen(viewModel: CricketViewModel, themeColor: Color, onPlayerClick: (Player) -> Unit) {
    val players by viewModel.playersStateFlow.collectAsState()
    val userTeam = viewModel.userTeam

    var nationalityFilter by remember { mutableStateOf("All") }
    var teamFilter by remember { mutableStateOf("Free Agents") }
    var countyFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val countryOptions = listOf("All", "Pakistan", "Foreign")
    val teamOptions = listOf("All", "Free Agents", "Kings", "Stars", "Sixers", "Gladiators", "Eagles", "Hawks")
    val countyOptions = listOf(
        "All", "Lahore", "Karachi", "Multan", "Peshawar", "Rawalpindi", "Mardan", "Quetta", "Sialkot", "Faisalabad", "Kent", "Surrey", "Yorkshire", "Lancashire"
    )

    // filter players using ViewModel logic
    val filteredPlayers = remember(nationalityFilter, teamFilter, countyFilter, searchQuery, players) {
        viewModel.getPlayersFiltered(nationalityFilter, if (teamFilter == "Free Agents") "All" else if (teamFilter == "All") "All" else teamFilter.lowercase(), countyFilter, searchQuery).filter { p ->
            if (teamFilter == "Free Agents") p.teamId == null else true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Budget overlay
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = themeColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACTIVE PURSE BALANCE", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("PKR %.2f Crore".format(userTeam.purseCr), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Squad: ${userTeam.players.size}/21", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Text(
            text = "🤝 Player Roster Transfers & Free Agency",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor
        )

        // FILTER PANEL
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🔍 Filter Database", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = themeColor)

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search player name...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = themeColor)
                )

                // Nationality row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Country: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(countryOptions) { opt ->
                            val isSel = nationalityFilter == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColor else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (isSel) themeColor else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable { nationalityFilter = opt }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(opt, fontSize = 10.sp, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Team row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Team: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(teamOptions) { opt ->
                            val isSel = teamFilter == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColor else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (isSel) themeColor else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable { teamFilter = opt }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(opt, fontSize = 10.sp, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // County row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("County: ", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(65.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(countyOptions) { opt ->
                            val isSel = countyFilter == opt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) themeColor else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (isSel) themeColor else MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable { countyFilter = opt }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(opt, fontSize = 10.sp, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // COUNT INDICATOR
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColor.copy(alpha = 0.1f))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Found ${filteredPlayers.size} players matching filters",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColor
                    )
                }
            }
        }

        // PLAYER LISTING
        filteredPlayers.forEach { p ->
            val isOwned = p.teamId == userTeam.id
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, if (isOwned) themeColor else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayerClick(p) }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(2f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(p.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            if (p.isForeign) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE53935))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("OVERSEAS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Role: ${p.role.name}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text("County: ${p.county}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("BATTING: ${p.battingSkill}", fontSize = 11.sp, color = themeColor, fontWeight = FontWeight.Bold)
                            Text("BOWLING: ${p.bowlingSkill}", fontSize = 11.sp, color = themeColor, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(
                            "%.2f Cr".format(p.marketPriceCr),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColor
                        )

                        if (isOwned) {
                            Button(
                                onClick = { viewModel.releaseSquadPlayer(p) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Release", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else if (p.teamId == null) {
                            Button(
                                onClick = { viewModel.purchaseMarketPlayer(p) },
                                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(28.dp),
                                enabled = userTeam.purseCr >= p.marketPriceCr
                            ) {
                                Text("Sign Buyer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(p.teamId!!.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: BRANDING & SPONSORSHIPS CUSTOMIZER
// -------------------------------------------------------------
@Composable
fun CustomizerScreen(viewModel: CricketViewModel, themeColor: Color) {
    val userTeam = viewModel.userTeam

    var teamName by remember { mutableStateOf(userTeam.name) }
    var abbreviation by remember { mutableStateOf(userTeam.abbreviation) }
    var emoji by remember { mutableStateOf(userTeam.flagEmoji) }
    var hexColorInput by remember { mutableStateOf(userTeam.colorHex) }

    val activeSponsor by viewModel.selectedSponsor.collectAsState()
    val activeTVChannel by viewModel.selectedTVChannel.collectAsState()

    val sponsorOptions = listOf("Malik Sports", "Sike's Brands", "Signify Ltd", "G.S Bats")
    val tvOptions = listOf("Roar Sports Live", "PrimeCast Ultra", "CricketNow HD", "Signify TV")

    // Preset color bubbles
    val styleColors = listOf(
        "#FBC02D", // gold
        "#10B981", // green
        "#EC4899", // pink
        "#8B5CF6", // purple
        "#3B82F6", // blue
        "#F97316", // orange
        "#D32F2F"  // crimson
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🎨 Club Branding & Scorecard Overlays",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = themeColor
        )

        // Franchise customization
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Customize Team Name & Logos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Franchise Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = abbreviation,
                        onValueChange = { abbreviation = it },
                        label = { Text("Abbreviation (e.g. GLD)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Team Logo Emoji", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Scorecard Match Highlights Theme Color", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                // presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    styleColors.forEach { color ->
                        val isSel = hexColorInput.equals(color, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    3.dp,
                                    if (isSel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CircleShape
                                )
                                    .clickable {
                                        hexColorInput = color
                                    }
                        )
                    }
                }

                OutlinedTextField(
                    value = hexColorInput,
                    onValueChange = { hexColorInput = it },
                    label = { Text("Or Type Hex Code Color (e.g., #8B5CF6)", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.updateTeamCustomization(teamName, abbreviation, emoji, hexColorInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Branding Customization", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Sponsor options
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sponsor Room Partnerships", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)

                sponsorOptions.forEach { opt ->
                    val isSel = activeSponsor == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setSponsorship(opt) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSel, onClick = { viewModel.setSponsorship(opt) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(opt, fontSize = 13.sp)
                    }
                }
            }
        }

        // TV overlays options
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select TV Channel Broadcast Watermark", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColor)

                tvOptions.forEach { opt ->
                    val isSel = activeTVChannel == opt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setTVChannel(opt) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSel, onClick = { viewModel.setTVChannel(opt) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(opt, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: BALL SIMULATOR MATCH CENTER
// -------------------------------------------------------------
@Composable
fun MatchCenterScreen(viewModel: CricketViewModel, themeColor: Color) {
    val matchStage by viewModel.matchStage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        when (matchStage) {
            MatchStage.TOSS -> TossPanelLayout(viewModel, themeColor)
            MatchStage.PLAYING -> LiveGameplayPanelLayout(viewModel, themeColor)
            MatchStage.INNINGS_BREAK -> InningsBreakPanelLayout(viewModel, themeColor)
            MatchStage.FINISHED -> MatchSummaryPanelLayout(viewModel, themeColor)
        }
    }
}

@Composable
fun TossPanelLayout(viewModel: CricketViewModel, themeColor: Color) {
    val homeTeam by viewModel.homeTeam.collectAsState()
    val awayTeam by viewModel.awayTeam.collectAsState()
    val userTossChoice by viewModel.userTossChoice.collectAsState()
    val tossChoice by viewModel.tossChoice.collectAsState()
    val tossWinner by viewModel.tossWinner.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🪙 Coin Toss Decides",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = themeColor
            )

            Text(
                text = "Flip heads or tails of the cricket coin prior to play matches:",
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Heads", "Tails").forEach { choice ->
                    OutlinedButton(
                        onClick = { viewModel.setUserTossChoice(choice) },
                        modifier = Modifier.weight(1f),
                        colors = if (userTossChoice == choice) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = themeColor,
                                contentColor = Color.White
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        border = BorderStroke(1.dp, themeColor)
                    ) {
                        Text(choice, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = { viewModel.performToss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("flip_coin_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text("Flip Coin", fontWeight = FontWeight.Bold)
            }

            if (tossWinner != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColor.copy(alpha = 0.15f))
                        .padding(12.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (tossWinner == homeTeam.name) {
                                "🎉 You Won the Toss!"
                            } else {
                                "Opposition ($tossWinner) Won the Toss!"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = themeColor,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Decision: Elected to $tossChoice first.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LiveGameplayPanelLayout(viewModel: CricketViewModel, themeColor: Color) {
    val battingTeamNow by viewModel.battingTeamNow.collectAsState()
    val bowlingTeamNow by viewModel.bowlingTeamNow.collectAsState()
    val currentInnings by viewModel.currentInnings.collectAsState()
    val isUserBatting by viewModel.isUserBatting.collectAsState()

    val runs by viewModel.runs.collectAsState()
    val wickets by viewModel.wickets.collectAsState()
    val balls by viewModel.balls.collectAsState()
    val target by viewModel.target.collectAsState()

    val strikerName by viewModel.strikerName.collectAsState()
    val nonStrikerName by viewModel.nonStrikerName.collectAsState()
    val currentBowlerName by viewModel.currentBowlerName.collectAsState()

    val recentBallLog by viewModel.recentBallLog.collectAsState()
    val commentaryList by viewModel.commentaryList.collectAsState()
    val isAutoSimulating by viewModel.isAutoSimulating.collectAsState()

    val bStatsMap by viewModel.batsmanStatsMap.collectAsState()
    val boStatsMap by viewModel.bowlerStatsMap.collectAsState()

    val milestoneText by viewModel.activeMilestoneText.collectAsState()
    val milestoneSubtext by viewModel.activeMilestoneSubtext.collectAsState()
    val isExploitWeaknessActive by viewModel.isExploitWeaknessActive.collectAsState()

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Play, 1: Gameplan

    // Dynamic team scorecard color mapping based on active batting team
    val battingTeamColor = remember(battingTeamNow.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(battingTeamNow.colorHex))
        } catch (e: Exception) {
            themeColor
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- MATCH MILESTONE CELEBRATIONS OVERLAY ---
        if (milestoneText != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(4.dp, Color(0xFFFFD700)),
                modifier = Modifier.fillMaxWidth().animateContentSize().testTag("milestone_overlay_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🎉 MATCH MILESTONE REVEAL 🎉",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF9100)
                    )
                    Text(
                        text = milestoneText ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = milestoneSubtext ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { viewModel.dismissMilestone() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Text("Dismiss / Resume Match 🏟️", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Sub Tab Selector
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = battingTeamColor,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("🏏 Play Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("🛂 Gameplan", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedSubTab == 0) {
            // Active Live Scoreboard
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(2.dp, battingTeamColor),
                modifier = Modifier.fillMaxWidth()
            ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(battingTeamColor)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "INNINGS $currentInnings",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = if (isUserBatting) "You are Batting 🏏" else "You are Bowling ⚾",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Teams displaying
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${battingTeamNow.flagEmoji} ${battingTeamNow.abbreviation}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = battingTeamColor
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("vs", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${bowlingTeamNow.abbreviation} ${bowlingTeamNow.flagEmoji}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Big Live Scores
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$runs/$wickets",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = battingTeamColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "in ${balls / 6}.${balls % 6} Overs",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Target banner
            if (currentInnings == 2 && target != null) {
                val needed = target!! - runs
                val maxBalls = (viewModel.oversLimit.value * 6) - balls
                Text(
                    text = "🎯 Need $needed runs in $maxBalls balls to win (Target: $target)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // 2D Canvas Arena Board
    val lastBallSymbol = recentBallLog.lastOrNull() ?: "0"
    StadiumCanvas(
        lastEvent = lastBallSymbol,
        modifier = Modifier.fillMaxWidth()
    )

    // Current Partnership Status
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🏏 Active Pitch Partnership",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = battingTeamColor
            )

            // Striker
            val sStats = bStatsMap[strikerName]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "* $strikerName",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(2f)
                )
                Row(
                    modifier = Modifier.weight(3f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${sStats?.runsScored ?: 0} (${sStats?.ballsFaced ?: 0})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SR: %.1f".format(sStats?.strikeRate ?: 0.0),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Non-striker
            val nsStats = bStatsMap[nonStrikerName]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "  $nonStrikerName",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.weight(2f)
                )
                Row(
                    modifier = Modifier.weight(3f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${nsStats?.runsScored ?: 0} (${nsStats?.ballsFaced ?: 0})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SR: %.1f".format(nsStats?.strikeRate ?: 0.0),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // Bowler
            val bowStats = boStatsMap[currentBowlerName]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🍒 $currentBowlerName",
                    fontSize = 12.sp,
                    modifier = Modifier.weight(2f),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.weight(3f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${bowStats?.wicketsTaken ?: 0}-${bowStats?.runsConceded ?: 0}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val ovStr = if (bowStats != null) "${bowStats.ballsBowled / 6}.${bowStats.ballsBowled % 6}" else "0.0"
                    Text(
                        text = "$ovStr Ov",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // Mini over log bubbles
    Column {
        Text(
            text = "🔴 This Over Conceded:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        if (recentBallLog.isEmpty()) {
            Text(
                text = "Starting over...",
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(recentBallLog) { item ->
                    val colorGroup = when (item) {
                        "W" -> Color(0xFFD32F2F)
                        "4" -> Color(0xFF2E7D32)
                        "6" -> Color(0xFF7B1FA2)
                        "0" -> Color(0xFF757575)
                        else -> Color(0xFF1976D2)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colorGroup)
                            .border(1.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    // Interactive Control Buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { viewModel.playSingleBall() },
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("play_ball_button"),
            colors = ButtonDefaults.buttonColors(containerColor = battingTeamColor)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Bowl Ball", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { viewModel.playWholeOverDirectly() },
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("simulate_over_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Sim Over", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = { viewModel.toggleAutoSimulate() },
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("auto_simulate_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isAutoSimulating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(
                if (isAutoSimulating) Icons.Default.Close else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(if (isAutoSimulating) "Stop" else "Auto Sim", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    // Scrollable Commentary log console
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "💬 Commentary Log Console",
                color = Color.Green,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(commentaryList) { line ->
                    val clr = when {
                        line.contains("OUT! ") || line.contains("departs") || line.contains("WICKET!") -> Color(0xFFFF5252)
                        line.contains("FOUR!") -> Color(0xFF69F0AE)
                        line.contains("SIX!") -> Color(0xFFE040FB)
                        line.contains("===") -> Color(0xFFFFD740)
                        else -> Color.White.copy(alpha = 0.85f)
                    }
                    Text(
                        text = line,
                        color = clr,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
        } else {
            // --- TAB 1: GAMEPLAN HUD ---
            LiveGameplanHUD(
                viewModel = viewModel,
                battingTeamNow = battingTeamNow,
                bowlingTeamNow = bowlingTeamNow,
                isUserBatting = isUserBatting,
                strikerName = strikerName,
                currentBowlerName = currentBowlerName,
                battingTeamColor = battingTeamColor,
                isExploitWeaknessActive = isExploitWeaknessActive
            )
        }
    }
}

@Composable
fun InningsBreakPanelLayout(viewModel: CricketViewModel, themeColor: Color) {
    val battingTeamNow by viewModel.battingTeamNow.collectAsState()
    val runs by viewModel.runs.collectAsState()
    val wickets by viewModel.wickets.collectAsState()
    val balls by viewModel.balls.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "☕ Innings Interval Break",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "${battingTeamNow.name} finished batting.",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 14.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL FIRST INNINGS SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$runs/$wickets",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = themeColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("in ${balls / 6}.${balls % 6} Overs", fontSize = 13.sp)
                }
            }

            Text(
                text = "Target score for chase: ${runs + 1} runs needed to win matches.",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontSize = 14.sp
            )

            Button(
                onClick = { viewModel.startSecondInnings() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Second Innings Chase 🚀", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MatchSummaryPanelLayout(viewModel: CricketViewModel, themeColor: Color) {
    val matchWinnerName by viewModel.matchWinnerName.collectAsState()
    val matchMargin by viewModel.matchMargin.collectAsState()
    val manOfTheMatch by viewModel.manOfTheMatch.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(2.dp, themeColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "🏆 Match Concluded",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = themeColor
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(themeColor.copy(alpha = 0.15f))
                    .fillMaxWidth()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("WINNER SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = themeColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = matchWinnerName,
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = matchMargin,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = themeColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Potm chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌟 PLAYER OF THE MATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(manOfTheMatch, fontSize = 16.sp, fontWeight = FontWeight.Black, color = themeColor)
                }
            }

            Button(
                onClick = { viewModel.resetMatchSetup() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Return to Career Hub", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LiveGameplanHUD(
    viewModel: CricketViewModel,
    battingTeamNow: Team,
    bowlingTeamNow: Team,
    isUserBatting: Boolean,
    strikerName: String,
    currentBowlerName: String,
    battingTeamColor: Color,
    isExploitWeaknessActive: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(battingTeamColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🛂", fontSize = 18.sp)
                }
                Column {
                    Text("LIVE GAMEPLAN TACTICAL HUD", fontSize = 14.sp, fontWeight = FontWeight.Black, color = battingTeamColor)
                    Text("Interactive Match Strategy Room", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                }
            }

            HorizontalDivider()

            val bTypeMap = mapOf(
                "ls" to "Leg Spin", "os" to "Off Spin", "lals" to "Left Arm Leg Spin", "laos" to "Left Arm Off Spin",
                "lac" to "Chinaman", "mv" to "Medium Variants", "fb" to "Fast Bowler", "fbs" to "Fast Controller", "m" to "Medium"
            )

            if (!isUserBatting) {
                // User is Bowling -> Target Opponent Batsman Weaknesses
                val activeBatsman = battingTeamNow.players.firstOrNull { it.name == strikerName }
                val activeBowler = bowlingTeamNow.players.firstOrNull { it.name == currentBowlerName }

                if (activeBatsman != null && activeBowler != null) {
                    // Batter info
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👤 Current Opponent Striker", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(activeBatsman.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Style: ${activeBatsman.playStyle.name} • BAT: ${activeBatsman.battingSkill}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Red.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    val friendlyWeak = bTypeMap[activeBatsman.weakness] ?: activeBatsman.weakness.uppercase()
                                    Text("WEAKNESS: $friendlyWeak", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }

                    // Bowler info
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🍒 Your Active Bowler Specialist", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = battingTeamColor.copy(alpha = 0.05f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(activeBowler.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("Style: ${bTypeMap[activeBowler.bowlingType] ?: activeBowler.bowlingType} • BOWL: ${activeBowler.bowlingSkill}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Compatibility Check
                    val bowlerMatchesWeakness = activeBowler.bowlingType == activeBatsman.weakness
                    if (bowlerMatchesWeakness) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5E9))
                                .border(1.dp, Color(0xFF2E7D32), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚡ TACTICAL SCENARIO ADVANTAGE: ${activeBowler.name}'s classification aligns perfectly with striker weakness! Yields high wicket chances.",
                                color = Color(0xFF1B5E20),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            val friendlyWeakname = bTypeMap[activeBatsman.weakness] ?: activeBatsman.weakness
                            Text(
                                text = "ℹ️ Captaincy Hint: Subbing in a ${friendlyWeakname} bowler would unlock severe tactical leverages against ${activeBatsman.name}.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // INTERACTIVE "EXPLOIT WEAKNESS" BUTTON
                    Button(
                        onClick = { viewModel.setExploitWeaknessActive(!isExploitWeaknessActive) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("exploit_weakness_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isExploitWeaknessActive) Color(0xFFD32F2F) else battingTeamColor
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isExploitWeaknessActive) {
                                "🔥 WEAKNESS EXPLOITER SPELL ACTIVE"
                            } else {
                                "🎯 EXPLOIT WEAKNESS FOCUS SPELL"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                // User is Batting
                val activeOpponentBowler = bowlingTeamNow.players.firstOrNull { it.name == currentBowlerName }
                val userBatsmanObj = battingTeamNow.players.firstOrNull { it.name == strikerName }

                if (activeOpponentBowler != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👤 Active Opponent Bowler Specialist", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(activeOpponentBowler.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Type: ${bTypeMap[activeOpponentBowler.bowlingType] ?: activeOpponentBowler.bowlingType}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                    Text("Skill: ${activeOpponentBowler.bowlingSkill}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📋 Batting Tactics & Captaincy Guidance", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val tipsText = when (activeOpponentBowler.deliveryStyle) {
                                    DeliveryStyle.SPIN -> {
                                        "🎯 SPIN TRAP DETECTED: Spinners thrive on extracting tight turns. Rotate batsman strikes often and prefer defense blocks on low bounces."
                                    }
                                    DeliveryStyle.FAST -> {
                                        "⚡ SPEED THREAT: Rapid pace. Play late off the backfoot. Use aggressive stances selectively when bowlers are downwind."
                                    }
                                    else -> {
                                        "🛡️ MEDIUM RESTRICTION: Highly accurate lengths. Best utilized by timing gaps cleanly or forcing risk-controlled ground shots."
                                    }
                                }
                                Text(tipsText, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (userBatsmanObj != null && userBatsmanObj.weakness == activeOpponentBowler.bowlingType) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFEBEE))
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            "⚠️ CAUTION CAPTAIN: Selected Striker ${userBatsmanObj.name} has a known vulnerability facing ${bTypeMap[activeOpponentBowler.bowlingType] ?: activeOpponentBowler.bowlingType}! Focus heavily on defense or execute strike rotations immediately.",
                                            color = Color(0xFFC62828),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerProfileDialog(player: Player, themeColor: Color, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = themeColor, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = player.name, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    if (player.isForeign) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE53935))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("OS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(
                    text = "${player.role.name.replace("_", " ")}  •  ${player.county}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skills Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BAT SKILL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${player.battingSkill}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = themeColor)
                            Text("Style: ${player.playStyle.name}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BOWL SKILL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                            Text("${player.bowlingSkill}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = themeColor)
                            val friendlyBType = when(player.bowlingType) {
                                "ls" -> "Leg Spin"
                                "os" -> "Off Spin"
                                "lals" -> "LA Leg Spin"
                                "laos" -> "LA Off Spin"
                                "lac" -> "Chinaman"
                                "mv" -> "Medium Var"
                                "fb" -> "Fast Bowler"
                                "fbs" -> "Fast Ctrl"
                                else -> "Medium"
                            }
                            Text("Style: $friendlyBType", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Career Statistics Boards
                Text("📊 Career Records", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Batting stats row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Innings", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${player.careerInningsBatting}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Runs", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${player.careerRuns}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Avg / SR", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("%.1f/%.1f".format(player.battingAverage, player.battingStrikeRate), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("HS", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${player.highestScore}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("50s / 100s", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${player.numFifties} / ${player.numCenturies}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bowling stats Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Wkts", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${player.careerWickets}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("BBI", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text(player.bestBowlingFigures, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Economy", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("%.2f".format(player.bowlingEconomy), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("3W / 5W Hauls", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${player.numThreeWickets} / ${player.numFiveWickets}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Scout Report about section
                Text("📋 Scout Report", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = themeColor)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "CORE EVALUATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = themeColor
                        )
                        val bTypeMap = mapOf(
                            "ls" to "Leg Spin", "os" to "Off Spin", "lals" to "Left Arm Leg Spin", "laos" to "Left Arm Off Spin",
                            "lac" to "Chinaman", "mv" to "Medium Variants", "fb" to "Fast Bowler", "fbs" to "Fast Controller", "m" to "Medium"
                        )
                        val desc = when (player.role) {
                            PlayerRole.BATSMAN -> {
                                if (player.isPowerHitter) {
                                    "Devastating powerhouse. Outstanding hand-eye coordination with high boundary frequency but vulnerable to subtle spinner variations."
                                } else if (player.isOpener || player.isTopOrder) {
                                    "Technical anchor. Formidable defense and outstanding temperament, perfect for taking new ball fatigue workloads."
                                } else {
                                    "Middle order maestro. Dynamic gap-finder and aggressive runner in hot chases."
                                }
                            }
                            PlayerRole.WICKET_KEEPER -> "A clean glove specialist showing extreme agility behind stumps. Highly reliable run accumulator."
                            PlayerRole.BOWLER -> {
                                if (player.deliveryStyle == DeliveryStyle.FAST) {
                                    "Rapid pace operator with fearsome bounce and lethal yorker variations. Relies on raw speed."
                                } else if (player.deliveryStyle == DeliveryStyle.SPIN) {
                                    "Deceptive spin doctor who thrives on drying surfaces. Exploits steep angles."
                                } else {
                                    "Intelligent swing merchant who masters wrist-seam control first under storm environments."
                                }
                            }
                            PlayerRole.ALL_ROUNDER -> "Dynamic match-winner providing supreme balance. Critical captaincy tool across both bowling and batting departments."
                        }
                        Text(
                            text = desc,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "TACTICAL RECOMMENDATIONS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = themeColor
                        )
                        val recDesc = when (player.role) {
                            PlayerRole.BATSMAN, PlayerRole.WICKET_KEEPER -> {
                                if (player.playStyle == PlayingStyle.AGGRESSIVE || player.playStyle == PlayingStyle.BLITZKRIEG) {
                                    "Unleash in early powerplays or death overs. High-impact risk is highly offset by supreme strike rate potential."
                                } else {
                                    "Let them construct the baseline. Avoid early matches on green wickets."
                                }
                            }
                            else -> {
                                if (player.deliveryStyle == DeliveryStyle.SPIN) {
                                    "Attack right after field spreads. Build pressure through dense close-in field setups."
                                } else {
                                    "Strike early with fresh bowler swing or spearhead death over wicket traps."
                                }
                            }
                        }
                        Text(
                            text = recDesc,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "SCOUTED WEAKNESS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.Red
                        )
                        val weakText = if (player.weakness.isNotEmpty()) {
                            "vulnerable against ${bTypeMap[player.weakness] ?: player.weakness.uppercase()} deliveries."
                        } else {
                            "shows no distinct tactical weakness based on telemetry reports."
                        }
                        Text(
                            text = "The player is chemically $weakText Maintain strict lines to choke their options.",
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}
