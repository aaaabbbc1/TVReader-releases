package com.example.data.model

enum class ReaderTheme(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SEPIA("Sepia"),
    AMOLED("AMOLED"),
    ASTIGMA("Astigma")
}

enum class AstigmaPreset(
    val label: String,
    val description: String,
    val backgroundHex: Long,
    val textHex: Long,
    val secondaryTextHex: Long
) {
    SOFT_AMBER(
        label = "Soft Amber",
        description = "Warm amber ink on deep charcoal for minimal halation",
        backgroundHex = 0xFF161614,
        textHex = 0xFFC5A87E,
        secondaryTextHex = 0xFF8C7759
    ),
    WARM_SLATE(
        label = "Warm Slate",
        description = "Low-glare stone gray text on matte slate surface",
        backgroundHex = 0xFF1B1D20,
        textHex = 0xFFB8B4AC,
        secondaryTextHex = 0xFF7B7973
    ),
    MUTED_SAGE(
        label = "Muted Sage",
        description = "Calm sage tint engineered to ease evening eye fatigue",
        backgroundHex = 0xFF131917,
        textHex = 0xFF9EB5A9,
        secondaryTextHex = 0xFF6B7F75
    ),
    DEEP_TAUPE(
        label = "Deep Taupe",
        description = "Reduced-contrast warm taupe on dark espresso background",
        backgroundHex = 0xFF191513,
        textHex = 0xFFBFAEA2,
        secondaryTextHex = 0xFF83746B
    )
}

enum class ReadingMode(val label: String) {
    PAGINATED("Page-by-page"),
    CONTINUOUS("Continuous scroll")
}

enum class PdfPageMode(val label: String) {
    ORIGINAL("Original"),
    ADAPTIVE("Adaptive")
}

enum class ReaderFontFamily(val label: String) {
    INTER("Inter"),
    LITERATA("Literata"),
    JETBRAINS_MONO("JetBrains Mono"),
    SYSTEM_SANS("System Sans"),
    SYSTEM_SERIF("System Serif")
}

enum class LibrarySort(val label: String) {
    TITLE("Title"),
    AUTHOR("Author"),
    DATE_ADDED("Date Added"),
    LAST_OPENED("Last Opened")
}

data class ReaderSettings(
    val theme: ReaderTheme = ReaderTheme.AMOLED,
    val astigmaPreset: AstigmaPreset = AstigmaPreset.SOFT_AMBER,
    val fontSizeSp: Int = 22,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.INTER,
    val lineHeightMultiplier: Float = 1.45f,
    val readingMode: ReadingMode = ReadingMode.PAGINATED,
    val alternativeLayout: Boolean = false,
    val focusMode: Boolean = false,
    val pdfPageMode: PdfPageMode = PdfPageMode.ORIGINAL,
    val brightnessPercent: Int = 80,
    val keepScreenOn: Boolean = true
)
