package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Donation
import com.example.data.Project
import com.example.data.UserWallet
import kotlinx.coroutines.launch
import java.text.DecimalFormat

// Color Palette for Municipal high-fidelity theme (Professional Polish style)
private val PrimaryEmerald = Color(0xFF005FB8) // Professional Polished Blue
private val DarkEmerald = Color(0xFF004A8F)    // Darker corporate brand accent
private val LightEmerald = Color(0xFFEFF3F9)   // Soft polished blue highlight

private val PrimaryOchre = Color(0xFFF4B400)   // Warm status indicators
private val DarkOchre = Color(0xFFB58400)
private val LightOchre = Color(0xFFFFFDE7)

private val M3Blue = Color(0xFF005FB8)         // Synced with Polished Blue
private val M3Red = Color(0xFFBA1A1A)          // Refined red for penalties/deletions

private val SlateBg = Color(0xFFF3F4F6)        // Premium slate backdrop
private val DarkCardBg = Color(0xFF1E2022)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BonyanMainScreen(viewModel: BonyanViewModel, modifier: Modifier = Modifier) {
    // Explicitly enforce Right-to-Left (RTL) for Arabic native experience
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val uiRole by viewModel.activeRole.collectAsStateWithLifecycle()
        val projects by viewModel.allProjects.collectAsStateWithLifecycle()
        val donations by viewModel.allDonations.collectAsStateWithLifecycle()
        val wallets by viewModel.allWallets.collectAsStateWithLifecycle()

        val donorBalance by viewModel.donorWalletBalance.collectAsStateWithLifecycle()
        val contractorBalance by viewModel.contractorWalletBalance.collectAsStateWithLifecycle()

        val selectedCell by viewModel.selectedGridCoordinate.collectAsStateWithLifecycle()
        val selectedProject by viewModel.currentSelectedProject.collectAsStateWithLifecycle()

        val successMsg by viewModel.successMessage.collectAsStateWithLifecycle()
        val errorMsg by viewModel.errorMessage.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        // Sync snackbar alerts with ViewModel message streams
        LaunchedEffect(successMsg, errorMsg) {
            if (successMsg != null) {
                snackbarHostState.showSnackbar(successMsg!!)
                viewModel.clearMessages()
            } else if (errorMsg != null) {
                snackbarHostState.showSnackbar(errorMsg!!)
                viewModel.clearMessages()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = "Bonyan Logo",
                                tint = PrimaryEmerald,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "بنيان التفاعلي",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    actions = {
                        // Display interactive status system fund total
                        val systemFund = projects.sumOf { it.collectedAmount }
                        val moneyFormat = DecimalFormat("#,###")
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Fund",
                                    tint = DarkEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "صندوق التضامن: ${moneyFormat.format(systemFund)} ريال",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkEmerald
                                )
                            }
                        }
                    }
                )
            },
            containerColor = SlateBg
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWideScreen = maxWidth > 800.dp

                if (isWideScreen) {
                    // Two-column layout for expanded/tablet sizes
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column: Navigation + Map Grid (60% width)
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RoleSelectorPanel(activeRole = uiRole, onRoleSelected = { viewModel.changeRole(it) })
                            WalletStatusBanner(
                                activeRole = uiRole,
                                donorBalance = donorBalance,
                                contractorBalance = contractorBalance
                            )
                            MapGridCard(
                                projects = projects,
                                selectedCell = selectedCell,
                                selectedProject = selectedProject,
                                onCellSelected = { x, y ->
                                    val match = projects.find { getGridX(it.latitude) == x && getGridY(it.longitude) == y }
                                    if (match != null) {
                                        viewModel.currentSelectedProject.value = match
                                        viewModel.selectedGridCoordinate.value = null
                                    } else {
                                        viewModel.selectedGridCoordinate.value = Pair(x, y)
                                        viewModel.currentSelectedProject.value = null
                                    }
                                },
                                onProjectSelected = {
                                    viewModel.currentSelectedProject.value = it
                                    viewModel.selectedGridCoordinate.value = null
                                }
                            )
                        }

                        // Right Column: Form details / lists (40% width)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailsPane(
                                activeRole = uiRole,
                                selectedCell = selectedCell,
                                selectedProject = selectedProject,
                                projects = projects,
                                donations = donations,
                                donorBalance = donorBalance,
                                contractorBalance = contractorBalance,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    // Mobile portrait responsive flow layout
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            RoleSelectorPanel(activeRole = uiRole, onRoleSelected = { viewModel.changeRole(it) })
                        }
                        item {
                            WalletStatusBanner(
                                activeRole = uiRole,
                                donorBalance = donorBalance,
                                contractorBalance = contractorBalance
                            )
                        }
                        item {
                            MapGridCard(
                                projects = projects,
                                selectedCell = selectedCell,
                                selectedProject = selectedProject,
                                onCellSelected = { x, y ->
                                    val match = projects.find { getGridX(it.latitude) == x && getGridY(it.longitude) == y }
                                    if (match != null) {
                                        viewModel.currentSelectedProject.value = match
                                        viewModel.selectedGridCoordinate.value = null
                                    } else {
                                        viewModel.selectedGridCoordinate.value = Pair(x, y)
                                        viewModel.currentSelectedProject.value = null
                                    }
                                },
                                onProjectSelected = {
                                    viewModel.currentSelectedProject.value = it
                                    viewModel.selectedGridCoordinate.value = null
                                }
                            )
                        }
                        item {
                            DetailsPane(
                                activeRole = uiRole,
                                selectedCell = selectedCell,
                                selectedProject = selectedProject,
                                projects = projects,
                                donations = donations,
                                donorBalance = donorBalance,
                                contractorBalance = contractorBalance,
                                viewModel = viewModel,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 400.dp, max = 800.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- ROW/GRID METRIC TRANSLATORS ----------------
// We project coordinates to an interactive grid for simulation gameplay
private fun getGridX(lat: Double): Int {
    // Riyadh sample 24.7... Maps to X [0-5]
    return when {
        lat > 25.5 -> 0
        lat > 24.5 -> 1
        lat > 23.5 -> 2
        lat > 22.5 -> 3
        lat > 21.5 -> 4
        else -> 5
    }
}

private fun getGridY(lon: Double): Int {
    // Longitude Maps to Y [0-5]
    return when {
        lon > 48.0 -> 0
        lon > 46.0 -> 1
        lon > 44.0 -> 2
        lon > 42.0 -> 3
        lon > 40.0 -> 4
        else -> 5
    }
}

private fun indexToGridLabel(x: Int, y: Int): String {
    val xChar = when(x) {
        0 -> "أ"
        1 -> "ب"
        2 -> "ج"
        3 -> "د"
        4 -> "هـ"
        else -> "و"
    }
    return "$xChar${y + 1}"
}

// Map cells helper to feed latitude/longitude from selected indices back
private fun gridToLatitude(x: Int): Double {
    return when(x) {
        0 -> 25.8
        1 -> 24.7
        2 -> 23.8
        3 -> 22.8
        4 -> 21.8
        else -> 20.8
    }
}

private fun gridToLongitude(y: Int): Double {
    return when(y) {
        0 -> 49.2
        1 -> 46.8
        2 -> 44.2
        3 -> 42.8
        4 -> 40.8
        else -> 38.8
    }
}

// ---------------- COMPOSABLE BLOCKS ----------------

@Composable
fun RoleSelectorPanel(
    activeRole: String,
    onRoleSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "اختر دورك الوظيفي لحوكمة وبناء المدينة:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val roles = listOf(
                    Triple("citizen", "المراسلون 📣", PrimaryEmerald),
                    Triple("donor", "المتبرعون 💎", PrimaryOchre),
                    Triple("contractor", "المقاولون 🏗️", M3Blue)
                )

                roles.forEach { (roleKey, label, color) ->
                    val isSelected = activeRole == roleKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) color.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) color else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onRoleSelected(roleKey) }
                            .testTag("role_${roleKey}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WalletStatusBanner(
    activeRole: String,
    donorBalance: Double,
    contractorBalance: Double
) {
    val moneyFormat = DecimalFormat("#,###")
    AnimatedContent(
        targetState = activeRole,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "wallet"
    ) { role ->
        when (role) {
            "citizen" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightEmerald),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Citizen Role",
                            tint = DarkEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "أهلاً بك يا باني المجتمع!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = DarkEmerald
                            )
                            Text(
                                text = "يمكنك الآن تحديد أي مربع شاغر بالخريطة لتصوير ورصد مشكلة أو اقتراح مشروع حيوي للأهالي.",
                                fontSize = 11.sp,
                                color = DarkEmerald.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
            "donor" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightOchre),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrimaryOchre.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Paid,
                            contentDescription = "Donor",
                            tint = DarkOchre,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "بقية رصيد استثماراتك الخيرية المتوفرة:",
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = DarkOchre
                            )
                            Text(
                                text = "${moneyFormat.format(donorBalance)} ريال سعودي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = DarkOchre
                            )
                        }
                        Badge(containerColor = PrimaryOchre) {
                            Text("محفظة المانح", color = Color.White, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                        }
                    }
                }
            }
            "contractor" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, M3Blue.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Contractor Wallet",
                            tint = M3Blue,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رصيد مؤسستك المقاولاتية المتاح للضمان والأنشطة:",
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                            Text(
                                text = "${moneyFormat.format(contractorBalance)} ريال سعودي",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = M3Blue
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(M3Blue, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("بوابة المقاولين", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MapGridCard(
    projects: List<Project>,
    selectedCell: Pair<Int, Int>?,
    selectedProject: Project?,
    onCellSelected: (Int, Int) -> Unit,
    onProjectSelected: (Project) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "الخريطة التفاعلية للمدينة 🗺️",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "حي الوطن والأقاليم المترابطة - مقدرة برصد المساحات",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Small legend
                Text(
                    text = "مساحة 6×6 خلايا",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Map grid grid coordinates drawing (6 columns x 6 rows)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Horizontal column header characters (أ, ب, ج, د, هـ, و)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Spacer(modifier = Modifier.width(32.dp)) // Corner indent
                    val colLabels = listOf("أ", "ب", "ج", "د", "هـ", "و")
                    colLabels.forEach { char ->
                        Text(
                            text = char,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                for (row in 0..5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Vertical row numbers label
                        Text(
                            text = "${row + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )

                        for (col in 0..5) {
                            val cellProject = projects.find { getGridX(it.latitude) == col && getGridY(it.longitude) == row }
                            val isSelectedCell = selectedCell?.first == col && selectedCell?.second == row
                            val isSelectedProject = selectedProject != null && cellProject != null && selectedProject.id == cellProject.id

                            val gridBg = when {
                                isSelectedProject -> MaterialTheme.colorScheme.primaryContainer
                                isSelectedCell -> PrimaryOchre.copy(alpha = 0.25f)
                                cellProject != null -> {
                                    when (cellProject.status) {
                                        "مكتمل_التنفيذ" -> LightEmerald
                                        "قيد_التنفيذ" -> Color(0xFFEFF6FF)
                                        "مكتمل_التمويل" -> Color(0xFFFAF5FF)
                                        else -> Color(0xFFFFFBEB)
                                    }
                                }
                                else -> SlateBg
                            }

                            val borderStroke = when {
                                isSelectedProject -> BorderStroke(2.dp, PrimaryEmerald)
                                isSelectedCell -> BorderStroke(2.dp, PrimaryOchre)
                                cellProject != null -> {
                                    val strokeColor = when (cellProject.status) {
                                        "مكتمل_التنفيذ" -> DarkEmerald
                                        "قيد_التنفيذ" -> M3Blue
                                        "مكتمل_التمويل" -> Color(0xFF9333EA)
                                        else -> DarkOchre
                                    }
                                    BorderStroke(1.5.dp, strokeColor)
                                }
                                else -> BorderStroke(1.dp, Color(0xFFE2E8F0))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(gridBg)
                                    .border(borderStroke, RoundedCornerShape(8.dp))
                                    .clickable { onCellSelected(col, row) }
                                    .testTag("grid_cell_${col}_${row}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cellProject != null) {
                                    val icon = when (cellProject.category) {
                                        "طرق" -> Icons.Default.Construction
                                        "مستشفيات" -> Icons.Default.LocalHospital
                                        "زراعة" -> Icons.Default.Agriculture
                                        "تعليم" -> Icons.Default.School
                                        else -> Icons.Default.Home
                                    }

                                    val iconColor = when (cellProject.status) {
                                        "مكتمل_التنفيذ" -> DarkEmerald
                                        "قيد_التنفيذ" -> M3Blue
                                        "مكتمل_التمويل" -> Color(0xFF9333EA)
                                        else -> DarkOchre
                                    }

                                    Icon(
                                        imageVector = icon,
                                        contentDescription = cellProject.title,
                                        tint = iconColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    // Empty cell, show subtle text label coord
                                    Text(
                                        text = "${col}${row}",
                                        fontSize = 8.sp,
                                        color = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Map Legends Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val labels = listOf(
                    Triple("قيد_التمويل", "تحت التمويل 🟠", DarkOchre),
                    Triple("مكتمل_التمويل", "جاهز للتنفيذ 🟣", Color(0xFF9333EA)),
                    Triple("قيد_التنفيذ", "تحت التنفيذ 🔵", M3Blue),
                    Triple("مكتمل_التنفيذ", "مكتمل المنفعة 🟢", DarkEmerald)
                )

                labels.forEach { (_, text, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(text = text, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun DetailsPane(
    activeRole: String,
    selectedCell: Pair<Int, Int>?,
    selectedProject: Project?,
    projects: List<Project>,
    donations: List<Donation>,
    donorBalance: Double,
    contractorBalance: Double,
    viewModel: BonyanViewModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Selected layout state decision
            when {
                selectedProject != null -> {
                    ProjectDetailsSegment(
                        project = selectedProject,
                        activeRole = activeRole,
                        donorBalance = donorBalance,
                        contractorBalance = contractorBalance,
                        viewModel = viewModel,
                        onClose = { viewModel.currentSelectedProject.value = null }
                    )
                }
                selectedCell != null && activeRole == "citizen" -> {
                    NewReportFormSegment(
                        coordinate = selectedCell,
                        viewModel = viewModel,
                        onCancel = { viewModel.selectedGridCoordinate.value = null }
                    )
                }
                else -> {
                    // Empty or list directory layout
                    TabsDefaultListsSegment(
                        activeRole = activeRole,
                        projects = projects,
                        donations = donations,
                        onProjectSelected = {
                            viewModel.currentSelectedProject.value = it
                            viewModel.selectedGridCoordinate.value = null
                        }
                    )
                }
            }
        }
    }
}

// ---------------- 1. PROJECT DETAILS VIEW + WORKFLOW INTERACTIONS ----------------

@Composable
fun ProjectDetailsSegment(
    project: Project,
    activeRole: String,
    donorBalance: Double,
    contractorBalance: Double,
    viewModel: BonyanViewModel,
    onClose: () -> Unit
) {
    val moneyFormat = DecimalFormat("#,###")
    val progress = if (project.requiredAmount > 0) (project.collectedAmount / project.requiredAmount).toFloat() else 0f
    val remaining = project.requiredAmount - project.collectedAmount

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val catIcon = when(project.category) {
                "طرق" -> Icons.Default.Construction
                "مستشفيات" -> Icons.Default.LocalHospital
                "زراعة" -> Icons.Default.Agriculture
                "تعليم" -> Icons.Default.School
                else -> Icons.Default.Home
            }
            Icon(imageVector = catIcon, contentDescription = project.category, tint = PrimaryEmerald, modifier = Modifier.size(24.dp))
            Text(
                text = "تفاصيل المشروع: ${project.category}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onClose) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
        }
    }

    Divider(modifier = Modifier.padding(vertical = 8.dp))

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(text = project.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateBg),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "الوصف وضرورة المشروع الأهلية:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text(text = project.description, fontSize = 12.sp, lineHeight = 18.sp, color = Color.DarkGray)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("المرسل الجغرافي:", fontSize = 9.sp, color = Color.DarkGray)
                        Text(project.reporterName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text("إحداثيات المربع:", fontSize = 9.sp, color = Color.DarkGray)
                        val xVal = getGridX(project.latitude)
                        val yVal = getGridY(project.longitude)
                        Text(indexToGridLabel(xVal, yVal), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryEmerald)
                    }
                }
            }
        }

        // Crowdfunding Target Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (progress >= 1f) LightEmerald else LightOchre),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (progress >= 1f) PrimaryEmerald.copy(alpha = 0.3f) else PrimaryOchre.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("مؤشر التمويل التضامني", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        val statusLabel = when(project.status) {
                            "مكتمل_التنفيذ" -> "مكتمل ومنتفع به 🟢"
                            "قيد_التنفيذ" -> "قيد البناء والتشييد 👷"
                            "مكتمل_التمويل" -> "مكتمل التمويل - جاهز للتعاقد 🔏"
                            else -> "قيد جمع التبرعات 🟡"
                        }
                        Text(statusLabel, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryEmerald)
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (progress >= 1f) PrimaryEmerald else PrimaryOchre,
                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("المجموع:", fontSize = 10.sp, color = Color.Gray)
                            Text("${moneyFormat.format(project.collectedAmount)} ريال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("الهدف المالي:", fontSize = 10.sp, color = Color.Gray)
                            Text("${moneyFormat.format(project.requiredAmount)} ريال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    if (remaining > 0) {
                        Text(
                            text = "المبلغ المتبقي للاعتماد المالي: ${moneyFormat.format(remaining)} ريال سعودي",
                            color = DarkOchre,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Escrow details & Contract details if claimed or claimed under progress
        if (project.contractorName != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, M3Blue.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تفاصيل العقد والضمان الجاري 📋", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = M3Blue)
                            val statusLabel = if (project.status == "مكتمل_التنفيذ") "منجز بالكامل" else "تحت الضمان والعمل"
                            Text(statusLabel, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = M3Blue)
                        }
                        Text("الجهة المنفذة المعتمدة: ${project.contractorName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "مبلغ التأمين المودع بالضمان (80%): ${moneyFormat.format(project.escrowDeposited)} ريال سعودي",
                            color = M3Blue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (project.status == "قيد_التنفيذ") {
                            Text(
                                text = "مستندات التنفيذ: تخضع حالياً لمؤشرات الإنجاز والجودة.",
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        // ROLE CONTEXTUAL CTAs (DONATE OR CONTRACT escrows)
        item {
            Box(
                modifier = Modifier
                    .fillModifier()
                    .padding(top = 8.dp)
            ) {
                when {
                    activeRole == "donor" && project.status == "قيد_التمويل" -> {
                        DonorInputPanel(
                            project = project,
                            donorBalance = donorBalance,
                            remainingNeeded = remaining,
                            viewModel = viewModel
                        )
                    }
                    activeRole == "contractor" -> {
                        ContractorInteractionPanel(
                            project = project,
                            contractorBalance = contractorBalance,
                            viewModel = viewModel
                        )
                    }
                    activeRole == "citizen" -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightEmerald),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = "قوانين", tint = DarkEmerald)
                                Text(
                                    text = "بصفتك مواطن، تم رصد هذا المطلب بنجاح. يمكنك التبديل إلى دور (المتبرعون) لدفع الدعم المالي، أو (المقاولون) للمساهمة بالتنفيذ الإنشائي الفعلي.",
                                    fontSize = 11.sp,
                                    color = DarkEmerald,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    else -> {
                        // Project already completed or ready/waiting
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "لا توجد إجراءات إضافية مطلوبة حالياً لهذا الدور للمشروع.",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper to clean padded fill space elements
private fun Modifier.fillModifier(): Modifier = this.fillMaxWidth()

@Composable
fun DonorInputPanel(
    project: Project,
    donorBalance: Double,
    remainingNeeded: Double,
    viewModel: BonyanViewModel
) {
    var donorName by remember { mutableStateOf("فاعل خير") }
    var donationAmountStr by remember { mutableStateOf("") }

    val amountToPay = donationAmountStr.toDoubleOrNull() ?: 0.0

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PrimaryOchre, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("بوابة تبرع آمن للمانح المستثمر 💎", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkOchre)

        OutlinedTextField(
            value = donorName,
            onValueChange = { donorName = it },
            label = { Text("اسم المانح / اسم المؤسسة") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = donationAmountStr,
            onValueChange = { donationAmountStr = it },
            label = { Text("قيمة التبرع (ريال سعودي)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = { Text("مثال: 5000") }
        )

        // Shortcut donation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val shortcuts = listOf(1000.0, 5000.0, 10000.0)
            shortcuts.forEach { value ->
                val cappedValue = if (value > remainingNeeded) remainingNeeded else value
                Button(
                    onClick = { donationAmountStr = cappedValue.toInt().toString() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOchre.copy(alpha = 0.15f), contentColor = DarkOchre),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ $value ريال", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Button(
            onClick = {
                viewModel.donateToProject(project.id, donorName, amountToPay)
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryOchre),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("donate_confirm_button"),
            enabled = amountToPay > 0 && donorBalance >= amountToPay
        ) {
            Icon(imageVector = Icons.Default.Favorite, contentDescription = "تبرع", tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("تأكيد دفع التبرع وخصم من محفظتي", fontWeight = FontWeight.Bold, color = Color.White)
        }

        if (amountToPay > donorBalance) {
            Text(
                text = "الرصيد المتاح في محفظتك غير كافٍ لتغطية التبرع!",
                color = M3Red,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContractorInteractionPanel(
    project: Project,
    contractorBalance: Double,
    viewModel: BonyanViewModel
) {
    val moneyFormat = DecimalFormat("#,###")
    var contractorName by remember { mutableStateOf("شركة النهضة للمقاولات") }

    val requiredEscrow = project.requiredAmount * 0.80

    when (project.status) {
        "مكتمل_التمويل" -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, M3Blue, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("المطالبة الإنشائية وضمان الجدية 🏗️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = M3Blue)

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "نظام الضمان الآمن (80% Escrow):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = M3Blue
                        )
                        Text(
                            text = "لحماية أموال المانحين وضمان الجدية، يُشترط إيداع 80% من ميزانية المشروع في خزانة الضمان بالمنصة.",
                            fontSize = 10.sp,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "مبلغ التأمين المطلوب للحجز: ${moneyFormat.format(requiredEscrow)} ريال",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = M3Blue
                        )
                    }
                }

                OutlinedTextField(
                    value = contractorName,
                    onValueChange = { contractorName = it },
                    label = { Text("اسم شركة المقاولات المسؤولة") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        viewModel.claimProject(project.id, contractorName)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = M3Blue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("contractor_claim_button"),
                    enabled = contractorBalance >= requiredEscrow
                ) {
                    Icon(imageVector = Icons.Default.Gavel, contentDescription = "حجز")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إيداع التأمين وحجز المشروع للبدء", fontWeight = FontWeight.Bold, color = Color.White)
                }

                if (contractorBalance < requiredEscrow) {
                    Text(
                        text = "محفظتك تفتقر للحد الكافي لإيداع التأمين الجدي!",
                        color = M3Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        "قيد_التنفيذ" -> {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, M3Blue.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text("خيارات التحكم في العقد ومراحل الإنجاز:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = M3Blue)

                // 1. Success completion
                Button(
                    onClick = { viewModel.completeProject(project.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("contractor_complete_button")
                ) {
                    Icon(imageVector = Icons.Default.DoneAll, contentDescription = "إنجاز")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تسليم المشروع وإتمام الإعمار بنجاح", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text(
                    text = "* ستحصل على كامل عوائد المشروع تبلغ [${moneyFormat.format(project.requiredAmount)}] ريال + استرجاع مبلغ الضمان [${moneyFormat.format(project.escrowDeposited)}] ريال لمصلحة محفظتك.",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    lineHeight = 13.sp
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                // 2. Penalty Cancellation
                Button(
                    onClick = { viewModel.forfeitProject(project.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = M3Red),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("contractor_forfeit_button")
                ) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = "إلغاء عقابي")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إلغاء العقد والتراجع عن التنفيذ (عقوبة مغلظة)", fontWeight = FontWeight.Bold, color = Color.White)
                }

                Text(
                    text = "تحذير: سيتم مصادرة مبلغ الضمان المالي المودع (${moneyFormat.format(project.escrowDeposited)} ريال) بالكامل وتحويله لصندوق البلدة التضامني لإخلالك بالموعد وجدية العمل.",
                    color = M3Red,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 14.sp
                )
            }
        }
        else -> {
            // Completed
            Card(
                colors = CardDefaults.cardColors(containerColor = LightEmerald),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "منتهية", tint = DarkEmerald)
                    Text("هذا المشروع تم تشييده وتسليمه لخدمة المدينة والمنفعة العامة بالكامل بنجاح!", fontSize = 11.sp, color = DarkEmerald, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- 2. NEW REPORT GEOTAGGING FORM SECTION ----------------

@Composable
fun NewReportFormSegment(
    coordinate: Pair<Int, Int>,
    viewModel: BonyanViewModel,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var reporterName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("طرق") }
    var costEstimateStr by remember { mutableStateOf("") }

    val categories = listOf("طرق", "مستشفيات", "زراعة", "تعليم", "مساجد")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "رصد وتوثيق بلاغ جغرافي جديد 📣",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = onCancel) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق")
        }
    }

    Divider(modifier = Modifier.padding(vertical = 8.dp))

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightEmerald, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = "الموقع الجغرافي المعين بالخريطة: المربع ${indexToGridLabel(coordinate.first, coordinate.second)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = DarkEmerald,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Text("صنف المبادرة أو المشروع:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    val isCatSelected = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCatSelected) PrimaryEmerald else Color(0xFFE2E8F0))
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCatSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان المطلب الأهلي (مثال: جسر مشاة)") },
                modifier = Modifier.fillMaxWidth().testTag("form_title"),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("شرح المشكلة بالتفصيل وحاجة الأهالي لها") },
                modifier = Modifier.fillMaxWidth().testTag("form_desc"),
                maxLines = 3
            )
        }

        item {
            OutlinedTextField(
                value = reporterName,
                onValueChange = { reporterName = it },
                label = { Text("اسم المواطن المرسل (صاحب البلاغ)") },
                modifier = Modifier.fillMaxWidth().testTag("form_reporter"),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = costEstimateStr,
                onValueChange = { costEstimateStr = it },
                label = { Text("الميزانية التقديرية المطلوبة للتطوير (ريال)") },
                modifier = Modifier.fillMaxWidth().testTag("form_cost"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = { Text("مثال: 50000") }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val estAmount = costEstimateStr.toDoubleOrNull() ?: 0.0
                        val computedLat = gridToLatitude(coordinate.first)
                        val computedLon = gridToLongitude(coordinate.second)

                        viewModel.reportNewProject(
                            title = title,
                            description = description,
                            category = selectedCategory,
                            latitude = computedLat,
                            longitude = computedLon,
                            requiredAmount = estAmount,
                            reporterName = reporterName
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryEmerald),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(46.dp)
                        .testTag("submit_report_button")
                ) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "إرسال")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("رفع البلاغ الجغرافي", fontWeight = FontWeight.Bold, color = Color.White)
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text("إلغاء")
                }
            }
        }
    }
}


// ---------------- 3. TABS / LISTS DIRECTORY VIEW ----------------

@Composable
fun TabsDefaultListsSegment(
    activeRole: String,
    projects: List<Project>,
    donations: List<Donation>,
    onProjectSelected: (Project) -> Unit
) {
    var listTabSelected by remember { mutableStateOf("projects") } // "projects" or "history"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = { listTabSelected = "projects" },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (listTabSelected == "projects") PrimaryEmerald else Color(0xFFF1F5F9),
                contentColor = if (listTabSelected == "projects") Color.White else Color.DarkGray
            ),
            modifier = Modifier.weight(1.2f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("مساحات المطالب الأهلية 🔍", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        Button(
            onClick = { listTabSelected = "history" },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (listTabSelected == "history") PrimaryEmerald else Color(0xFFF1F5F9),
                contentColor = if (listTabSelected == "history") Color.White else Color.DarkGray
            ),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("سجلات الدعم 🪙", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    if (listTabSelected == "projects") {
        // Filter elements matching active role properties
        val filteredProjects = when (activeRole) {
            "contractor" -> projects.filter { it.status == "مكتمل_التمويل" || (it.status == "قيد_التنفيذ" && it.contractorName != null) }
            "donor" -> projects.filter { it.status == "قيد_التمويل" }
            else -> projects // Show all for citizens
        }

        if (filteredProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.SentimentDissatisfied, contentDescription = "فارغ", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    val noDataLabel = when (activeRole) {
                        "contractor" -> "لا توجد مشاريع جاهزة للتنفيذ حالياً. بانتظار اكتمال تمويل بعض المطالب!"
                        "donor" -> "تم تلبية وتمويل جميع مطالب الأهالي الحالية كاملة تضامنياً! جزاكم الله خيراً."
                        else -> "لا توجد مطالب مسجلة بالبلدية حتى الآن."
                    }
                    Text(
                        text = noDataLabel,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProjects) { project ->
                    ProjectRowItem(project = project, onSelect = { onProjectSelected(project) })
                }
            }
        }
    } else {
        // History of Donations log
        if (donations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "لا يوجد", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Text(
                        text = "لم يتم تسجيل أي عمليات تبرع أو دعم مالي تفاعلي بالمنصة حتى الآن.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(donations) { item ->
                    DonationHistoryRowItem(donation = item)
                }
            }
        }
    }
}

@Composable
fun ProjectRowItem(project: Project, onSelect: () -> Unit) {
    val moneyFormat = DecimalFormat("#,###")
    val progress = if (project.requiredAmount > 0) (project.collectedAmount / project.requiredAmount).toFloat() else 0f
    val completePercent = (progress * 100).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("project_row_${project.id}"),
        colors = CardDefaults.cardColors(containerColor = SlateBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val icon = when(project.category) {
                        "طرق" -> Icons.Default.Construction
                        "مستشفيات" -> Icons.Default.LocalHospital
                        "زراعة" -> Icons.Default.Agriculture
                        "تعليم" -> Icons.Default.School
                        else -> Icons.Default.Home
                    }
                    Icon(imageVector = icon, contentDescription = project.category, tint = PrimaryEmerald, modifier = Modifier.size(16.dp))
                    Text(text = project.category, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryEmerald)
                }

                val labelColor = when (project.status) {
                    "مكتمل_التنفيذ" -> DarkEmerald
                    "قيد_التنفيذ" -> M3Blue
                    "مكتمل_التمويل" -> Color(0xFF9333EA)
                    else -> DarkOchre
                }

                val textStatus = when (project.status) {
                    "مكتمل_التنفيذ" -> "منجز ومكتمل ❇️"
                    "قيد_التنفيذ" -> "قيد التشييد 👷"
                    "مكتمل_التمويل" -> "جاهز للتعهيد 🔏"
                    else -> "قيد التمويل $completePercent%"
                }

                Text(
                    text = textStatus,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
            }

            Text(
                text = project.title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تكلفة التمويل: ${moneyFormat.format(project.requiredAmount)} ريال",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
                Text(
                    text = "موقع: ${indexToGridLabel(getGridX(project.latitude), getGridY(project.longitude))}",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }

            // progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (progress >= 1f) PrimaryEmerald else PrimaryOchre,
                trackColor = Color.LightGray.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun DonationHistoryRowItem(donation: Donation) {
    val moneyFormat = DecimalFormat("#,###")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateBg),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "تبرع من: ${donation.donorName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = DarkEmerald
                )
                Text(
                    text = "+ ${moneyFormat.format(donation.amount)} ريال",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = PrimaryEmerald
                )
            }
            Text(
                text = "دعم لمشروع: ${donation.projectTitle}",
                fontSize = 10.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
