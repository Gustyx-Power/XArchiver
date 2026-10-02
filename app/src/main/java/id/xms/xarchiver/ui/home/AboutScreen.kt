package id.xms.xarchiver.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import id.xms.xarchiver.ui.theme.ThemePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val themePreferences = remember { ThemePreferences(context) }
    val isMiuixUiEnabled by themePreferences.isMiuixUiEnabled.collectAsState(initial = ThemePreferences.isMiuiOrHyperOsDevice)

    if (isMiuixUiEnabled) {
        MiuixAboutScreen(navController = navController)
        return
    }

    val scrollState = rememberScrollState()

    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    val packageName = remember {
        context.packageName ?: "id.xms.xarchiver"
    }

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



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 1500f
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    TopAppBar(
                        title = {
                            Text(
                                stringResource(R.string.about_title),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                        )
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))

                // App Icon
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(GradientStart, GradientEnd)
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "App Logo",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // App Name
                Text(
                    text = "XArchiver",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Version
                Text(
                    text = "${stringResource(R.string.about_version)} $versionName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString("v$versionName"))
                        Toast.makeText(context, context.getString(R.string.about_copied_format, "v$versionName"), Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Tagline
                Text(
                    text = stringResource(R.string.about_app_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                // App Specs & Features Card (Material 3 Style)
                AboutLinkItem(
                    icon = Icons.Outlined.Storage,
                    title = stringResource(R.string.about_storage),
                    subtitle = storageText,
                    onClick = {
                        val path = Environment.getExternalStorageDirectory().absolutePath
                        navController.navigate("explorer/" + Uri.encode(path))
                    }
                )

                AboutLinkItem(
                    icon = Icons.Outlined.FolderZip,
                    title = stringResource(R.string.about_supported_formats),
                    subtitle = stringResource(R.string.about_supported_formats_desc),
                    onClick = { navController.navigate("about/formats") }
                )

                AboutLinkItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_detailed_specs),
                    subtitle = stringResource(R.string.about_detailed_specs_desc),
                    onClick = { navController.navigate("about/specs") }
                )

                Spacer(Modifier.height(16.dp))

                // Developer Card (Material 3 Style)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.about_developer),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "GustyxPower",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Links (Material 3 Style)
                AboutLinkItem(
                    icon = Icons.Outlined.Code,
                    title = stringResource(R.string.about_source_code),
                    subtitle = stringResource(R.string.about_github_repo),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Xtra-Manager-Software/XArchiver.git"))
                        context.startActivity(intent)
                    }
                )

                AboutLinkItem(
                    icon = Icons.Outlined.BugReport,
                    title = stringResource(R.string.about_issue_tracker),
                    subtitle = stringResource(R.string.about_report_bugs),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Xtra-Manager-Software/XArchiver/issues"))
                        context.startActivity(intent)
                    }
                )

                AboutLinkItem(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.about_license),
                    subtitle = stringResource(R.string.about_license_mit),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://opensource.org/licenses/MIT"))
                        context.startActivity(intent)
                    }
                )

                Spacer(Modifier.height(16.dp))

                // License Info Card (Material 3 Style)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Gavel,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.about_license_mit),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Copyright © 2026 GustyxPower\n\n" +
                                   "Permission is hereby granted, free of charge, to any person obtaining a copy " +
                                   "of this software and associated documentation files (the \"Software\"), to deal " +
                                   "in the Software without restriction, including without limitation the rights " +
                                   "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
                                   "copies of the Software.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutLinkItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}
