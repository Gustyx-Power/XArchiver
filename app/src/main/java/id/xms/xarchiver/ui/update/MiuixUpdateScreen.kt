package id.xms.xarchiver.ui.update

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.core.update.UpdateCheckResult
import id.xms.xarchiver.core.update.UpdateInfo
import id.xms.xarchiver.core.update.UpdateManager
import id.xms.xarchiver.ui.components.MarkdownContent
import id.xms.xarchiver.ui.theme.GradientEnd
import id.xms.xarchiver.ui.theme.GradientStart
import id.xms.xarchiver.ui.theme.ThemeMode
import id.xms.xarchiver.ui.theme.ThemePreferences
import id.xms.xarchiver.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiuixUpdateScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val themePreferences = remember { ThemePreferences(context) }
    val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDark = isAppInDarkTheme(context, currentThemeMode)

    // MIUIX / HyperOS Design Tokens
    val pageBgColor = if (isDark) Color.Black else Color(0xFFF2F4F7)
    val cardBgColor = if (isDark) Color(0xFF141416) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF222225) else Color(0xFFE5E7EB)
    val dividerColor = if (isDark) Color(0xFF222225) else Color(0xFFF0F0F2)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1F2937)
    val secondaryTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6B7280)
    val miuixBlue = Color(0xFF0070F0)
    val miuixBlueLight = Color(0xFF0D84FF)

    val currentVersionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "2.1.0"
        } catch (_: Exception) {
            "2.1.0"
        }
    }

    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(UpdateManager.availableUpdate) }
    var isUpToDate by remember { mutableStateOf(false) }
    var checkError by remember { mutableStateOf<String?>(null) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedBytesText by remember { mutableStateOf("") }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showMenu by remember { mutableStateOf(false) }

    fun checkUpdates(force: Boolean = false) {
        if (isChecking || isDownloading) return
        isChecking = true
        checkError = null
        isUpToDate = false

        scope.launch {
            kotlinx.coroutines.delay(500)
            when (val result = UpdateManager.checkForUpdate(currentVersionName)) {
                is UpdateCheckResult.UpdateAvailable -> {
                    isChecking = false
                    updateInfo = result.updateInfo
                    UpdateManager.availableUpdate = result.updateInfo
                    isUpToDate = false
                }
                is UpdateCheckResult.UpToDate -> {
                    isChecking = false
                    updateInfo = null
                    UpdateManager.availableUpdate = null
                    isUpToDate = true
                }
                is UpdateCheckResult.Error -> {
                    isChecking = false
                    checkError = result.message
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (updateInfo == null) {
            checkUpdates()
        }
    }

    fun startDownload(info: UpdateInfo) {
        if (isDownloading) return
        isDownloading = true
        downloadError = null
        downloadProgress = 0f

        scope.launch {
            try {
                val file = UpdateManager.downloadApk(
                    context = context,
                    downloadUrl = info.apkAsset.downloadUrl,
                    fileName = info.apkAsset.name
                ) { downloaded, total, percent ->
                    downloadProgress = percent
                    val dlMb = downloaded / (1024f * 1024f)
                    val totMb = total / (1024f * 1024f)
                    downloadedBytesText = String.format(Locale.US, "%.1f MB / %.1f MB", dlMb, totMb)
                }

                downloadedFile = file
                isDownloading = false
                UpdateManager.installApk(context, file)
            } catch (e: Exception) {
                isDownloading = false
                downloadError = e.message ?: "Gagal mengunduh berkas pembaruan."
            }
        }
    }

    val deviceModel = remember {
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.uppercase(Locale.US)
        } else {
            "${manufacturer.uppercase(Locale.US)} $model"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {
        // Ambient soft lavender/blue gradient glow in the hero area (HyperOS signature)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF261D4E).copy(alpha = 0.5f),
                                Color(0xFF16132C).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color(0xFFEDE9FE).copy(alpha = 0.65f),
                                Color(0xFFF3E8FF).copy(alpha = 0.35f),
                                Color.Transparent
                            )
                        }
                    )
                )
        )

        // Main Scaffold
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = primaryTextColor
                        )
                    }

                    Text(
                        text = stringResource(R.string.update_screen_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = primaryTextColor
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.update_check_again)) },
                                onClick = {
                                    showMenu = false
                                    checkUpdates(force = true)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.about_changelog)) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("changelog")
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.NewReleases, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Salin Info Versi") },
                                onClick = {
                                    showMenu = false
                                    clipboardManager.setText(AnnotatedString("XArchiver v$currentVersionName ($deviceModel)"))
                                    Toast.makeText(context, "Info versi disalin", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // If update is available -> Display spacious, scrollable changelog view
                if (updateInfo != null) {
                    val info = updateInfo!!
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                            .padding(bottom = 120.dp)
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // MIUIX Version Header Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = if (isDark) 0.dp else 1.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    spotColor = Color.Black.copy(alpha = 0.04f)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            color = cardBgColor,
                            border = BorderStroke(1.dp, cardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // App Icon
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(GradientStart, GradientEnd)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.logo),
                                        contentDescription = null,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "v${info.versionName}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = info.releaseTitle.ifBlank { "XArchiver ${info.versionName}" },
                                        fontSize = 13.5.sp,
                                        color = secondaryTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Size Badge
                                Surface(
                                    shape = RoundedCornerShape(100),
                                    color = miuixBlue.copy(alpha = if (isDark) 0.18f else 0.1f),
                                    border = BorderStroke(1.dp, miuixBlue.copy(alpha = 0.25f))
                                ) {
                                    Text(
                                        text = info.apkAsset.formattedSize,
                                        color = miuixBlue,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // MIUIX Spacious Changelog Card
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = cardBgColor,
                            border = BorderStroke(1.dp, cardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = if (isDark) 0.dp else 1.dp,
                                    shape = RoundedCornerShape(20.dp),
                                    spotColor = Color.Black.copy(alpha = 0.04f)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(miuixBlue.copy(alpha = if (isDark) 0.15f else 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.NewReleases,
                                            contentDescription = null,
                                            tint = miuixBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.update_changelog_label),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryTextColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = dividerColor
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val changelogText = info.changelog.ifBlank {
                                    "Pembaruan versi ${info.versionName} membawa peningkatan stabilitas, performa, dan fitur terkini."
                                }

                                MarkdownContent(
                                    markdown = changelogText,
                                    textColor = primaryTextColor.copy(alpha = 0.95f),
                                    accentColor = miuixBlue,
                                    secondaryColor = secondaryTextColor,
                                    dividerColor = dividerColor,
                                    codeBgColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                                    codeTextColor = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                                )
                            }
                        }

                        if (downloadError != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEF4444).copy(alpha = if (isDark) 0.15f else 0.1f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = downloadError!!,
                                    color = Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }

                    // Sticky Floating Bottom Action with Progress-Filling Pill (MIUIX style)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        pageBgColor.copy(alpha = 0.95f),
                                        pageBgColor
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                            .navigationBarsPadding()
                    ) {
                        val animatedProgress by animateFloatAsState(
                            targetValue = downloadProgress.coerceIn(0f, 1f),
                            animationSpec = spring(stiffness = Spring.StiffnessLow),
                            label = "miuix_pill_progress"
                        )

                        val pillTrackColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E7EB)

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(
                                    elevation = if (isDark) 4.dp else 2.dp,
                                    shape = RoundedCornerShape(28.dp),
                                    spotColor = if (downloadedFile != null) Color(0xFF10B981) else miuixBlue
                                ),
                            shape = RoundedCornerShape(28.dp),
                            color = pillTrackColor,
                            border = BorderStroke(1.dp, cardBorderColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(enabled = !isDownloading) {
                                        if (downloadedFile != null) {
                                            UpdateManager.installApk(context, downloadedFile!!)
                                        } else {
                                            startDownload(info)
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Progress fill layer inside the pill
                                if (isDownloading) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(animatedProgress)
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        miuixBlue,
                                                        miuixBlueLight
                                                    )
                                                )
                                            )
                                    )
                                } else if (downloadedFile != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF10B981))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        miuixBlue,
                                                        miuixBlueLight
                                                    )
                                                )
                                            )
                                    )
                                }

                                // Text & icon layer centered inside the pill
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when {
                                        isDownloading -> {
                                            val percent = (animatedProgress * 100).toInt()
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.5.dp,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (downloadedBytesText.isNotEmpty()) {
                                                    "Mengunduh $percent% ($downloadedBytesText)"
                                                } else {
                                                    "Mengunduh $percent%"
                                                },
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                        downloadedFile != null -> {
                                            Icon(
                                                imageVector = Icons.Default.DownloadDone,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = stringResource(R.string.update_install_btn),
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        else -> {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${stringResource(R.string.update_download_btn)} (${info.apkAsset.formattedSize})",
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // MIUIX Up-to-date or Checking state -> Hero Screen with MIUIX elegance
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.7f))

                        // MIUIX App Icon Container
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .shadow(
                                    elevation = if (isDark) 0.dp else 4.dp,
                                    shape = RoundedCornerShape(22.dp),
                                    spotColor = Color(0x334F46E5)
                                )
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(GradientStart, GradientEnd)
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(22.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "XArchiver",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = deviceModel,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = secondaryTextColor,
                            letterSpacing = 0.3.sp
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = stringResource(R.string.update_software_version),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = secondaryTextColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "v$currentVersionName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryTextColor
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        // Floating Pill at Bottom (MIUIX style)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .shadow(
                                    elevation = if (isDark) 0.dp else 2.dp,
                                    shape = RoundedCornerShape(30.dp),
                                    spotColor = Color.Black.copy(alpha = 0.05f)
                                )
                                .navigationBarsPadding(),
                            shape = RoundedCornerShape(30.dp),
                            color = cardBgColor,
                            border = BorderStroke(1.dp, cardBorderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 22.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isChecking) {
                                    Text(
                                        text = stringResource(R.string.update_checking_pill),
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = primaryTextColor
                                    )
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = miuixBlue
                                    )
                                } else if (checkError != null) {
                                    Text(
                                        text = "Gagal terhubung",
                                        fontSize = 14.sp,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { checkUpdates(force = true) }) {
                                        Text(text = "Coba Lagi", color = miuixBlue, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = stringResource(R.string.update_uptodate_pill),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryTextColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { checkUpdates(force = true) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = stringResource(R.string.update_check_again),
                                            tint = secondaryTextColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))
                    }
                }
            }
        }
    }
}
