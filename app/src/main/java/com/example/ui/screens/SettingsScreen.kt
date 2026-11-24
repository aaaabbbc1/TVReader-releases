package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AstigmaPreset
import com.example.data.model.PdfPageMode
import com.example.data.model.ReaderFontFamily
import com.example.data.model.ReaderSettings
import com.example.data.model.ReaderTheme
import com.example.data.model.ReadingMode
import com.example.ui.components.TvActionButton
import com.example.ui.components.TvFocusCard
import com.example.ui.components.TvSelectableChip
import com.example.ui.theme.TvColors
import com.example.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

private enum class SettingsCategory(val label: String) {
    TEXT("Text"),
    READING("Reading"),
    DISPLAY("Display")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: ReaderSettings,
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCategory by remember { mutableStateOf(SettingsCategory.DISPLAY) }
    var showAstigmaDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Background)
    ) {
        val isWide = maxWidth >= 680.dp

        if (isWide) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Category Rail matching 09_themes.png
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TvActionButton(
                            text = "",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = onBack,
                            tag = "settings_back_button"
                        )
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TvColors.TextPrimary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SettingsCategory.entries.forEach { cat ->
                        TvSelectableChip(
                            label = cat.label,
                            selected = activeCategory == cat,
                            usePrimaryActiveColor = true,
                            onClick = { activeCategory = cat },
                            modifier = Modifier.fillMaxWidth(),
                            tag = "settings_tab_${cat.name.lowercase()}"
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(TvColors.BorderSubtle)
                )

                SettingsDetailContent(
                    activeCategory = activeCategory,
                    settings = settings,
                    viewModel = viewModel,
                    onOpenAstigmaPresets = { showAstigmaDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 36.dp, vertical = 28.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TvActionButton(
                        text = "",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        tag = "settings_back_button"
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TvColors.TextPrimary
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SettingsCategory.entries.forEach { cat ->
                        TvSelectableChip(
                            label = cat.label,
                            selected = activeCategory == cat,
                            usePrimaryActiveColor = true,
                            onClick = { activeCategory = cat },
                            tag = "settings_tab_${cat.name.lowercase()}"
                        )
                    }
                }
                SettingsDetailContent(
                    activeCategory = activeCategory,
                    settings = settings,
                    viewModel = viewModel,
                    onOpenAstigmaPresets = { showAstigmaDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }

    if (showAstigmaDialog) {
        AlertDialog(
            onDismissRequest = { showAstigmaDialog = false },
            containerColor = TvColors.Surface,
            title = { Text("Astigma Presets", color = TvColors.TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Reduced-contrast palettes designed to minimize halation and eye strain.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary
                    )
                    AstigmaPreset.entries.forEach { preset ->
                        TvFocusCard(
                            onClick = {
                                viewModel.setAstigmaPreset(preset)
                                viewModel.setTheme(ReaderTheme.ASTIGMA)
                                showAstigmaDialog = false
                            },
                            selected = settings.astigmaPreset == preset,
                            tag = "astigma_preset_${preset.name.lowercase()}"
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.backgroundHex)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Aa", color = Color(preset.textHex))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TvColors.TextPrimary
                                    )
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TvColors.TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAstigmaDialog = false }) {
                    Text("Close", color = TvColors.PrimaryTextAccent)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDetailContent(
    activeCategory: SettingsCategory,
    settings: ReaderSettings,
    viewModel: SettingsViewModel,
    onOpenAstigmaPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        when (activeCategory) {
            SettingsCategory.DISPLAY -> {
                item {
                    SettingRowCard(title = "Theme") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderTheme.entries.forEach { theme ->
                                TvSelectableChip(
                                    label = theme.label,
                                    selected = settings.theme == theme,
                                    onClick = { viewModel.setTheme(theme) },
                                    tag = "theme_chip_${theme.name.lowercase()}"
                                )
                            }
                        }
                    }
                }
                item {
                    SettingRowCard(title = "PDF Pages") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PdfPageMode.entries.forEach { mode ->
                                TvSelectableChip(
                                    label = mode.label,
                                    selected = settings.pdfPageMode == mode,
                                    onClick = { viewModel.setPdfPageMode(mode) },
                                    tag = "pdf_mode_${mode.name.lowercase()}"
                                )
                            }
                        }
                    }
                }
                item {
                    SettingRowCard(title = "Brightness: ${settings.brightnessPercent}%") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            TvActionButton(
                                text = "−",
                                filled = true,
                                onClick = { viewModel.setBrightness(settings.brightnessPercent - 10) },
                                tag = "brightness_minus_button"
                            )
                            Text(
                                text = "${settings.brightnessPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = TvColors.TextPrimary
                            )
                            TvActionButton(
                                text = "+",
                                filled = true,
                                onClick = { viewModel.setBrightness(settings.brightnessPercent + 10) },
                                tag = "brightness_plus_button"
                            )
                        }
                    }
                }
                item {
                    SettingRowCard(
                        title = "Astigma Presets",
                        subtitle = "Active: ${settings.astigmaPreset.label}"
                    ) {
                        TvActionButton(
                            text = stringResource(R.string.action_choose_preset),
                            filled = true,
                            onClick = onOpenAstigmaPresets,
                            tag = "choose_astigma_preset_button"
                        )
                    }
                }
            }

            SettingsCategory.TEXT -> {
                item {
                    SettingRowCard(title = "Font Size: ${settings.fontSizeSp} sp") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            TvActionButton(
                                text = "A−",
                                filled = true,
                                onClick = { viewModel.setFontSize(settings.fontSizeSp - 2) },
                                tag = "font_size_minus"
                            )
                            Text(
                                text = "${settings.fontSizeSp}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TvColors.TextPrimary
                            )
                            TvActionButton(
                                text = "A+",
                                filled = true,
                                onClick = { viewModel.setFontSize(settings.fontSizeSp + 2) },
                                tag = "font_size_plus"
                            )
                        }
                    }
                }
                item {
                    SettingRowCard(title = "Font Family") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReaderFontFamily.entries.forEach { family ->
                                TvSelectableChip(
                                    label = family.label,
                                    selected = settings.fontFamily == family,
                                    onClick = { viewModel.setFontFamily(family) },
                                    tag = "font_family_${family.name.lowercase()}"
                                )
                            }
                        }
                    }
                }
                item {
                    val linePct = (settings.lineHeightMultiplier * 100).roundToInt()
                    SettingRowCard(title = "Line Spacing: ${linePct}%") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TvActionButton(
                                text = "−",
                                filled = true,
                                onClick = { viewModel.setLineHeight(settings.lineHeightMultiplier - 0.1f) }
                            )
                            Text(
                                text = String.format("%.2fx", settings.lineHeightMultiplier),
                                style = MaterialTheme.typography.titleMedium,
                                color = TvColors.TextPrimary
                            )
                            TvActionButton(
                                text = "+",
                                filled = true,
                                onClick = { viewModel.setLineHeight(settings.lineHeightMultiplier + 0.1f) }
                            )
                        }
                    }
                }
            }

            SettingsCategory.READING -> {
                item {
                    SettingRowCard(title = "Reading Mode") {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReadingMode.entries.forEach { mode ->
                                TvSelectableChip(
                                    label = mode.label,
                                    selected = settings.readingMode == mode,
                                    onClick = { viewModel.setReadingMode(mode) },
                                    tag = "reading_mode_${mode.name.lowercase()}"
                                )
                            }
                        }
                    }
                }
                item {
                    SettingToggleCard(
                        title = "Alternative Layout",
                        subtitle = "Document-style view with title, author, and full TOC as a page",
                        checked = settings.alternativeLayout,
                        onToggle = { viewModel.setAlternativeLayout(it) },
                        tag = "toggle_alternative_layout"
                    )
                }
                item {
                    SettingToggleCard(
                        title = "Focus Mode",
                        subtitle = "Hide reading progress footer and UI chrome while reading",
                        checked = settings.focusMode,
                        onToggle = { viewModel.setFocusMode(it) },
                        tag = "toggle_focus_mode"
                    )
                }
                item {
                    SettingToggleCard(
                        title = "Keep Screen On",
                        subtitle = "Prevent TV or device sleep while a book is open",
                        checked = settings.keepScreenOn,
                        onToggle = { viewModel.setKeepScreenOn(it) },
                        tag = "toggle_keep_screen_on"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingRowCard(
    title: String,
    subtitle: String? = null,
    control: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = TvColors.Surface,
        border = BorderStroke(1.dp, TvColors.BorderSubtle)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TvColors.TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TvColors.TextSecondary
                    )
                }
            }
            control()
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    tag: String
) {
    TvFocusCard(
        onClick = { onToggle(!checked) },
        tag = tag
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TvColors.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TvColors.TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = TvColors.PrimaryBright
                )
            )
        }
    }
}
