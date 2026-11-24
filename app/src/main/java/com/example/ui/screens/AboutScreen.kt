package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.TvActionButton
import com.example.ui.theme.TvColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSupportDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showFeaturesDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        val hPad = if (maxWidth >= 680.dp) 48.dp else 20.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = hPad, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TvActionButton(
                text = stringResource(R.string.action_back),
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onBack,
                tag = "about_back_button"
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "TVReader",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TvColors.TextPrimary
                )
                Text(
                    text = stringResource(R.string.app_version_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = TvColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.titleLarge,
                fontStyle = FontStyle.Italic,
                color = TvColors.PrimaryTextAccent
            )

            Text(
                text = stringResource(R.string.supported_formats_line),
                style = MaterialTheme.typography.titleMedium,
                color = TvColors.TextSecondary
            )

            Surface(
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = TvColors.Surface,
                border = BorderStroke(1.dp, TvColors.BorderSubtle)
            ) {
                Text(
                    text = stringResource(R.string.about_creator_note),
                    style = MaterialTheme.typography.titleMedium,
                    lineHeight = 26.sp,
                    color = TvColors.TextPrimary,
                    modifier = Modifier.padding(22.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TvActionButton(
                    text = stringResource(R.string.about_support_dev),
                    icon = Icons.Default.Favorite,
                    filled = true,
                    onClick = { showSupportDialog = true },
                    tag = "about_support_button"
                )
                TvActionButton(
                    text = stringResource(R.string.about_send_feedback),
                    icon = Icons.Default.Email,
                    filled = false,
                    onClick = { showFeedbackDialog = true },
                    tag = "about_feedback_button"
                )
                TvActionButton(
                    text = stringResource(R.string.about_features_whats_new),
                    icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                    filled = false,
                    onClick = { showFeaturesDialog = true },
                    tag = "about_features_button"
                )
            }
        }
    }

    if (showSupportDialog) {
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            containerColor = TvColors.Surface,
            title = { Text("Support TV Reader", color = TvColors.TextPrimary) },
            text = {
                Text(
                    text = "TV Reader is free with no feature limits and no ads. You can support continued development via GitHub Sponsors or Ko-fi.",
                    color = TvColors.TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSupportDialog = false
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Sozdan2016/TVReader-releases"))
                            )
                        }
                    }
                ) {
                    Text("Open Project Page", color = TvColors.PrimaryTextAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSupportDialog = false }) {
                    Text("Close", color = TvColors.TextSecondary)
                }
            }
        )
    }

    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            containerColor = TvColors.Surface,
            title = { Text("Send Feedback", color = TvColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Email: tvreader@proton.me", color = TvColors.TextPrimary)
                    Text("Telegram: @tvreader_bot", color = TvColors.TextSecondary)
                    Text("Issues: GitHub Discussions & Issues", color = TvColors.TextSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFeedbackDialog = false
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:tvreader@proton.me")).apply {
                                    putExtra(Intent.EXTRA_SUBJECT, "TV Reader Feedback")
                                }
                            )
                        }
                    }
                ) {
                    Text("Email Developer", color = TvColors.PrimaryTextAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Close", color = TvColors.TextSecondary)
                }
            }
        )
    }

    if (showFeaturesDialog) {
        AlertDialog(
            onDismissRequest = { showFeaturesDialog = false },
            containerColor = TvColors.Surface,
            title = { Text("Features & Supported Formats", color = TvColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Full D-pad remote & touch navigation", color = TvColors.TextPrimary)
                    Text("• Page-by-page, Continuous scroll & Alternative layout", color = TvColors.TextPrimary)
                    Text("• Side Table of Contents, Bookmarks & Focus Mode", color = TvColors.TextPrimary)
                    Text("• 5 Themes: Light, Dark, Sepia, AMOLED, Astigma + Presets", color = TvColors.TextPrimary)
                    Text("• Project Gutenberg & Standard Ebooks built in", color = TvColors.TextPrimary)
                    Text("• WebDAV, FTP, Direct URL & Local File Picker", color = TvColors.TextPrimary)
                    Text("• EPUB, FB2, PDF, TXT, MD, HTML, CBZ", color = TvColors.PrimaryTextAccent)
                }
            },
            confirmButton = {
                TextButton(onClick = { showFeaturesDialog = false }) {
                    Text("Got it", color = TvColors.PrimaryTextAccent)
                }
            }
        )
    }
}
