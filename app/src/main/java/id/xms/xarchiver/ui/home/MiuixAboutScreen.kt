package id.xms.xarchiver.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import id.xms.xarchiver.R
import id.xms.xarchiver.core.StorageUtils
import id.xms.xarchiver.core.humanReadable
import id.xms.xarchiver.ui.theme.GradientEnd
import id.xms.xarchiver.ui.theme.GradientStart
import id.xms.xarchiver.ui.theme.ThemeMode
import id.xms.xarchiver.ui.theme.ThemePreferences
import id.xms.xarchiver.ui.theme.isAppInDarkTheme
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

@Composable
fun MiuixAboutScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val themePreferences = remember { ThemePreferences(context) }
    val currentThemeMode by themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
    val isDark = isAppInDarkTheme(context, currentThemeMode)

    val pageBgColor = if (isDark) Color.Black else Color(0xFFF2F4F7)
    val cardBgColor = if (isDark) Color(0xFF141416) else Color.White
    val cardBorderColor = if (isDark) Color(0xFF222225) else Color(0xFFE5E7EB)
    val dividerColor = if (isDark) Color(0xFF222225) else Color(0xFFF0F0F2)
    val primaryTextColor = if (isDark) Color.White else Color(0xFF1F2937)
    val secondaryTextColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6B7280)
    val chevronColor = if (isDark) Color(0xFF636366) else Color(0xFF9CA3AF)

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    val packageName = remember {
        context.packageName ?: "id.xms.xarchiver"
    }

    // Storage info
    val storageInfo = remember {
        try {
            val storages = StorageUtils.getAllStorage(context)
            storages.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    val storageText = remember(storageInfo) {
        if (storageInfo != null) {
            "${storageInfo.used.humanReadable()} / ${storageInfo.total.humanReadable()}"
        } else {
            "Internal Storage"
        }
    }

    var showLicenseDialog by remember { mutableStateOf(false) }
    var easterEggTaps by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBgColor)
    ) {
        // Ambient soft lavender/blue gradient glow in the hero area (signature HyperOS aesthetic)
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

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = "Back",
                            tint = primaryTextColor
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Header: App Logo, XArchiver Typography & Subtitle
                item {
                    val heroInteraction = remember { MutableInteractionSource() }
                    val isHeroPressed by heroInteraction.collectIsPressedAsState()
                    val heroScale by animateFloatAsState(
                        targetValue = if (isHeroPressed) 0.95f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                        label = "heroScale"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 20.dp)
                            .scale(heroScale)
                            .clickable(
                                interactionSource = heroInteraction,
                                indication = null
                            ) {
                                easterEggTaps++
                                if (easterEggTaps >= 5) {
                                    Toast.makeText(context, context.getString(R.string.about_easter_egg_format, versionName), Toast.LENGTH_SHORT).show()
                                    easterEggTaps = 0
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // App Icon Badge
                        Box(
                            modifier = Modifier
                                .size(76.dp)
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
                                modifier = Modifier.size(46.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "XArchiver",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = primaryTextColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "v$versionName",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.5.sp,
                            color = secondaryTextColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = stringResource(R.string.about_app_desc),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF4F46E5)
                        )
                    }
                }

                // Card 1: Application Information & Features
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDark) 0.dp else 1.dp,
                                shape = RoundedCornerShape(18.dp),
                                spotColor = Color.Black.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 0.5.dp,
                                color = cardBorderColor,
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        color = cardBgColor
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AboutSpecRow(
                                title = stringResource(R.string.about_application),
                                value = "XArchiver",
                                showChevron = false,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_version),
                                value = "v$versionName",
                                showChevron = false,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("v$versionName"))
                                    Toast.makeText(context, context.getString(R.string.about_copied_format, "v$versionName"), Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_package_name),
                                value = packageName,
                                showChevron = false,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(packageName))
                                    Toast.makeText(context, context.getString(R.string.about_copied_format, packageName), Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_storage),
                                value = storageText,
                                showChevron = true,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = {
                                    val path = Environment.getExternalStorageDirectory().absolutePath
                                    navController.navigate("explorer/" + Uri.encode(path))
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_supported_formats),
                                value = stringResource(R.string.about_supported_formats_desc),
                                showChevron = true,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = { navController.navigate("about/formats") }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_detailed_specs),
                                value = null,
                                showChevron = true,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = { navController.navigate("about/specs") }
                            )
                        }
                    }
                }

                // Card 2: Developer & Source Links
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = if (isDark) 0.dp else 1.dp,
                                shape = RoundedCornerShape(18.dp),
                                spotColor = Color.Black.copy(alpha = 0.04f)
                            )
                            .border(
                                width = 0.5.dp,
                                color = cardBorderColor,
                                shape = RoundedCornerShape(18.dp)
                            ),
                        shape = RoundedCornerShape(18.dp),
                        color = cardBgColor
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AboutSpecRow(
                                title = stringResource(R.string.about_developer),
                                value = "GustyxPower",
                                showChevron = false,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_source_code),
                                value = stringResource(R.string.about_github_repo),
                                showChevron = true,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Xtra-Manager-Software/XArchiver.git"))
                                    context.startActivity(intent)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_issue_tracker),
                                value = stringResource(R.string.about_report_bugs),
                                showChevron = true,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Xtra-Manager-Software/XArchiver/issues"))
                                    context.startActivity(intent)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = dividerColor
                            )
                            AboutSpecRow(
                                title = stringResource(R.string.about_license),
                                value = stringResource(R.string.about_license_mit),
                                showChevron = true,
                                primaryText = primaryTextColor,
                                secondaryText = secondaryTextColor,
                                chevronColor = chevronColor,
                                onClick = { showLicenseDialog = true }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }



    // MIT License Dialog
    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = {
                Text("MIT License", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Copyright © 2026 GustyxPower\n\n" +
                        "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                        "of this software and associated documentation files (the \"Software\"), to deal " +
                        "in the Software without restriction, including without limitation the rights " +
                        "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
                        "copies of the Software, and to permit persons to whom the Software is " +
                        "furnished to do so, subject to the following conditions:\n\n" +
                        "The above copyright notice and this permission notice shall be included in all " +
                        "copies or substantial portions of the Software.",
                        fontSize = 13.sp,
                        color = secondaryTextColor
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showLicenseDialog = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://opensource.org/licenses/MIT"))
                    context.startActivity(intent)
                }) {
                    Text("View Online")
                }
            }
        )
    }
}

@Composable
private fun AboutSpecRow(
    title: String,
    value: String? = null,
    showChevron: Boolean = false,
    primaryText: Color,
    secondaryText: Color,
    chevronColor: Color,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = primaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                color = secondaryText,
                fontSize = 14.sp,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (showChevron) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                tint = chevronColor,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SpecDialogItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = value, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
