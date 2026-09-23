package com.lumina.studio.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MediaType(val labelAr: String, val labelEn: String) {
  PHOTO("صورة", "Photo"),
  VIDEO("فيديو", "Video")
}

enum class VideoResolution(
  val label: String,
  val width: Int,
  val height: Int,
  val badge: String,
  val defaultBitrateMbps: Int
) {
  UHD_4K("4K Ultra HD", 3840, 2160, "4K UHD", 60),
  QHD_2K("2K Quad HD", 2560, 1440, "2K QHD", 35),
  FHD_1080P("1080p Full HD", 1920, 1080, "1080p", 20),
  HD_720P("720p HD", 1280, 720, "720p", 10)
}

/**
 * Codecs actually encodable by Android MediaCodec on-device.
 * (Apple ProRes is NOT encodable by Android hardware encoders, so it is intentionally absent.)
 */
enum class ExportCodec(val label: String, val description: String, val mimeType: String) {
  HEVC_H265("H.265 / HEVC", "أقصى كفاءة ضغط مع دقة فائقة 4K", "video/hevc"),
  AVC_H264("H.264 / AVC", "توافق قياسي مع كافة المنصات", "video/avc")
}

enum class FilterCategory(val labelAr: String, val labelEn: String) {
  ALL("الكل", "All"),
  CINEMATIC("سينما هوليوود", "Hollywood Cinema"),
  ANALOG_FILM("أفلام أنالوج", "Analog Film"),
  MOODY_ATMOSPHERE("أجواء سينمائية", "Moody Atmosphere"),
  MODERN_HDR("حديث و HDR", "Modern & HDR"),
  MONOCHROME("أبيض وأسود", "Monochrome Noir"),
  CREATIVE_SCIFI("خيال علمي", "Creative Sci-Fi")
}

enum class FilterPreset(
  val labelAr: String,
  val labelEn: String,
  val category: FilterCategory = FilterCategory.ALL,
  val descriptionAr: String = "",
  val descriptionEn: String = "",
  val primaryColorHex: Long = 0xFF374151,
  val secondaryColorHex: Long = 0xFF1F2937
) {
  ORIGINAL(
    labelAr = "الأصلي",
    labelEn = "Original RAW",
    category = FilterCategory.ALL,
    descriptionAr = "الألوان الطبيعية الخام بدون تدريج لوني",
    descriptionEn = "Natural raw footage profile with zero grading",
    primaryColorHex = 0xFF4B5563,
    secondaryColorHex = 0xFF1F2937
  ),
  TEAL_ORANGE(
    labelAr = "تيل وبرتقالي سينمائي",
    labelEn = "Teal & Orange Blockbuster",
    category = FilterCategory.CINEMATIC,
    descriptionAr = "درجات دافئة للبشرة مع ظلال فيروزية سينمائية عميقة",
    descriptionEn = "Warm skin tones with deep cinematic teal cyan shadows",
    primaryColorHex = 0xFF0D9488,
    secondaryColorHex = 0xFFF97316
  ),
  BLOCKBUSTER_COLD(
    labelAr = "بلوك باستر بارد",
    labelEn = "Nordic Cold Thriller",
    category = FilterCategory.CINEMATIC,
    descriptionAr = "أزرق جليدي بارد مع تباين درامي لأفلام الإثارة",
    descriptionEn = "Icy blue palette with high dramatic contrast for suspense",
    primaryColorHex = 0xFF0284C7,
    secondaryColorHex = 0xFF38BDF8
  ),
  BLEACH_BYPASS(
    labelAr = "سحب الفضة 500T",
    labelEn = "Bleach Bypass 500T",
    category = FilterCategory.CINEMATIC,
    descriptionAr = "ألوان فضية مشدودة مع تباين قوي مستوحى من أفلام الحركة",
    descriptionEn = "High contrast desaturated gritty look from bleach bypass processing",
    primaryColorHex = 0xFF9CA3AF,
    secondaryColorHex = 0xFF4B5563
  ),
  DESERT_HEAT(
    labelAr = "حرارة الصحراء",
    labelEn = "Desert Heat 35mm",
    category = FilterCategory.CINEMATIC,
    descriptionAr = "درجات كهرمانية ونحاسية دافئة تناسب المشاهد الخارجية الحارة",
    descriptionEn = "Warm amber and copper tones for intense golden sunlit landscapes",
    primaryColorHex = 0xFFD97706,
    secondaryColorHex = 0xFFB45309
  ),
  KODAK_PORTRA(
    labelAr = "كوداك بورترا 400",
    labelEn = "Kodak Portra 400",
    category = FilterCategory.ANALOG_FILM,
    descriptionAr = "درجات بشرة ناعمة وحريرية مع ألوان باستيل كلاسيكية",
    descriptionEn = "Legendary portrait film with silky skin tones and warm pastel highlights",
    primaryColorHex = 0xFFE0A96D,
    secondaryColorHex = 0xFFC07D45
  ),
  FUJI_CHROME(
    labelAr = "فوجي فيلم فيلفيا 50",
    labelEn = "Fujifilm Velvia Chrome",
    category = FilterCategory.ANALOG_FILM,
    descriptionAr = "تشبع مذهل للأخضر الزمردي وسماء زرقاء عميقة نابضة",
    descriptionEn = "Ultra-saturated emerald greens, vivid cobalt skies and deep punchy contrast",
    primaryColorHex = 0xFF0284C7,
    secondaryColorHex = 0xFF059669
  ),
  RETRO_70S(
    labelAr = "سوبر 8 ريترو 1977",
    labelEn = "Super 8 Vintage 1977",
    category = FilterCategory.ANALOG_FILM,
    descriptionAr = "ظلال مرتفعة مع لمسة دافئة تعيد زمن السينما القديمة",
    descriptionEn = "Raised milky blacks and golden-green organic vintage tint",
    primaryColorHex = 0xFFFBBF24,
    secondaryColorHex = 0xFF78350F
  ),
  KODACHROME_64(
    labelAr = "كوداكروم 64",
    labelEn = "Kodachrome 64 Classic",
    category = FilterCategory.ANALOG_FILM,
    descriptionAr = "أحمر عميق وأزرق مشبع مع تباين وثائقي سينمائي أسطوري",
    descriptionEn = "Iconic rich reds, intense blues, and timeless documentary film contrast",
    primaryColorHex = 0xFFDC2626,
    secondaryColorHex = 0xFF1D4ED8
  ),
  POLAROID_WARM(
    labelAr = "بولارويد دافئ فوري",
    labelEn = "Instant Warm Polaroid",
    category = FilterCategory.ANALOG_FILM,
    descriptionAr = "ألوان فورية دافئة مع تعتيم فني ناعم",
    descriptionEn = "Warm nostalgic hues with gentle dynamic range and soft highlights",
    primaryColorHex = 0xFFF59E0B,
    secondaryColorHex = 0xFFD97706
  ),
  EMERALD(
    labelAr = "زمردي سينمائي كئيب",
    labelEn = "Moody Emerald Forest",
    category = FilterCategory.MOODY_ATMOSPHERE,
    descriptionAr = "خضرة سينمائية غنية مع ظلال داكنة غامضة للمشاهد الطبيعية",
    descriptionEn = "Deep pine and emerald hues with moody shadows for mystery films",
    primaryColorHex = 0xFF10B981,
    secondaryColorHex = 0xFF047857
  ),
  GOLDEN_HOUR(
    labelAr = "الساعة الذهبية",
    labelEn = "Golden Hour Sunset",
    category = FilterCategory.MOODY_ATMOSPHERE,
    descriptionAr = "وهج غروب الشمس الناري وإضاءة دافئة سينمائية حالمة",
    descriptionEn = "Radiant sunset glow and rich amber illumination",
    primaryColorHex = 0xFFF59E0B,
    secondaryColorHex = 0xFFDC2626
  ),
  MIDNIGHT_BLUE(
    labelAr = "أزرق منتصف الليل",
    labelEn = "Midnight Moonlight",
    category = FilterCategory.MOODY_ATMOSPHERE,
    descriptionAr = "أزرق ليلي عميق يحاكي ضوء القمر السينمائي مع ظلال نقية",
    descriptionEn = "Deep moonlight indigo wash with preserved shadow details",
    primaryColorHex = 0xFF1E3A8A,
    secondaryColorHex = 0xFF312E81
  ),
  AUTUMN_VIBE(
    labelAr = "خريفي دافئ",
    labelEn = "Autumn Amber Foliage",
    category = FilterCategory.MOODY_ATMOSPHERE,
    descriptionAr = "أوراق شجر ذهبية دافئة وألوان خريفية حية ومريحة للعين",
    descriptionEn = "Warm golden leaves and cozy saturated terracotta earth tones",
    primaryColorHex = 0xFFEA580C,
    secondaryColorHex = 0xFF9A3412
  ),
  VIVID_HDR(
    labelAr = "HDR سينمائي نقي",
    labelEn = "Vivid 4K Cinema HDR",
    category = FilterCategory.MODERN_HDR,
    descriptionAr = "أقصى مدى ديناميكي بألوان مشبعة وتفاصيل فائقة الوضوح",
    descriptionEn = "Boosted dynamic range with vivid micro-contrast and punchy clarity",
    primaryColorHex = 0xFF06B6D4,
    secondaryColorHex = 0xFF8B5CF6
  ),
  CLEAN_COMMERCIAL(
    labelAr = "إعلاني استوديو ناصع",
    labelEn = "Commercial Studio Clean",
    category = FilterCategory.MODERN_HDR,
    descriptionAr = "ألوان حيادية نقية وإضاءة بيضاء مشرقة تناسب الإعلانات والمحتوى العصري",
    descriptionEn = "Neutral balanced tones and pristine whites for crisp commercial aesthetics",
    primaryColorHex = 0xFF38BDF8,
    secondaryColorHex = 0xFFE2E8F0
  ),
  PASTEL_DREAM(
    labelAr = "باستيل سينمائي ناعم",
    labelEn = "Pastel Cinematic Dream",
    category = FilterCategory.MODERN_HDR,
    descriptionAr = "ألوان وردية وبنفسجية حالمة وناعمة مع تباين خفيف",
    descriptionEn = "Dreamy soft peach and lavender highlights with lowered contrast",
    primaryColorHex = 0xFFF472B6,
    secondaryColorHex = 0xFFA78BFA
  ),
  SUNSET_CORAL(
    labelAr = "مرجاني دافئ مشرق",
    labelEn = "Vibrant Sunset Coral",
    category = FilterCategory.MODERN_HDR,
    descriptionAr = "درجات مرجانية متوهجة تعطي حيوية للمشاهد الصيفية والشاطئية",
    descriptionEn = "Glowing coral and peach highlights with energized warmth",
    primaryColorHex = 0xFFFB7185,
    secondaryColorHex = 0xFFF43F5E
  ),
  FILM_NOIR(
    labelAr = "أبيض وأسود نوار",
    labelEn = "Film Noir High Contrast",
    category = FilterCategory.MONOCHROME,
    descriptionAr = "أبيض وأسود كلاسيكي مع تباين درامي وظلال سواد عميقة",
    descriptionEn = "Classic monochrome with dramatic crushed blacks and luminous highlights",
    primaryColorHex = 0xFFE5E7EB,
    secondaryColorHex = 0xFF111827
  ),
  SILVER_MONO(
    labelAr = "فضي استوديو ناعم",
    labelEn = "Silver Gelatin Soft Mono",
    category = FilterCategory.MONOCHROME,
    descriptionAr = "تدرجات رمادية غنية وناعمة تحاكي طباعة الفضة الاحترافية",
    descriptionEn = "Smooth continuous-tone greyscale with rich midtone separation",
    primaryColorHex = 0xFFD1D5DB,
    secondaryColorHex = 0xFF4B5563
  ),
  SEPIA_ANTIQUE(
    labelAr = "سيبيا معتق تاريخي",
    labelEn = "Antique Warm Sepia",
    category = FilterCategory.MONOCHROME,
    descriptionAr = "درجات بني نحاسية معتقة تعطي طابع الصور والمخطوطات التاريخية",
    descriptionEn = "Warm brown sepia wash inspired by turn-of-the-century cinematography",
    primaryColorHex = 0xFF92400E,
    secondaryColorHex = 0xFF451A03
  ),
  CYBERPUNK(
    labelAr = "سايبربانك 2027",
    labelEn = "Cyberpunk Neo Tokyo",
    category = FilterCategory.CREATIVE_SCIFI,
    descriptionAr = "أضواء نيون سايان وماجنتا فائقة التوهج للمشاهد المستقبلية",
    descriptionEn = "Hyper-saturated neon cyan and magenta glow for futuristic cityscapes",
    primaryColorHex = 0xFF00F0FF,
    secondaryColorHex = 0xFFFF007F
  ),
  INFRARED(
    labelAr = "أشعة تحت الحمراء نيوني",
    labelEn = "Aerochrome Infrared",
    category = FilterCategory.CREATIVE_SCIFI,
    descriptionAr = "ألوان حرارية معكوسة تحول الخضرة إلى أحمر ووردي صارخ",
    descriptionEn = "Surreal false-color infrared that turns greens into striking crimson",
    primaryColorHex = 0xFFEC4899,
    secondaryColorHex = 0xFF8B5CF6
  ),
  MATRIX_GREEN(
    labelAr = "شفرة ماتريكس الخضراء",
    labelEn = "Matrix Terminal Green",
    category = FilterCategory.CREATIVE_SCIFI,
    descriptionAr = "صبغة خضراء رقمية غامرة مستوحاة من عوالم الخيال العلمي والسيبرانية",
    descriptionEn = "Digital phosphorescent green wash inspired by sci-fi cyberpunk worlds",
    primaryColorHex = 0xFF22C55E,
    secondaryColorHex = 0xFF14532D
  )
}

enum class CropAspect(val label: String, val ratio: Float, val iconName: String) {
  ORIGINAL("أصلي", 0f, "original"),
  RATIO_16_9("16:9", 16f / 9f, "cinema"),
  RATIO_9_16("9:16", 9f / 16f, "reels"),
  RATIO_1_1("1:1", 1f, "square"),
  RATIO_4_5("4:5", 4f / 5f, "portrait"),
  RATIO_21_9("21:9", 21f / 9f, "wide")
}

enum class EditorMode {
  BEGINNER,
  PRO
}

enum class EditorStudioType(val labelAr: String, val labelEn: String) {
  VIDEO("محرر الفيديو", "Video Editor"),
  PHOTO("محرر الصور", "Photo Editor")
}

data class AdjustmentsState(
  val exposure: Float = 0f,       // -100..100
  val contrast: Float = 0f,       // -100..100
  val brightness: Float = 0f,     // -100..100
  val saturation: Float = 0f,     // -100..100
  val temperature: Float = 0f,    // -100..100
  val tint: Float = 0f,           // -100..100
  val highlights: Float = 0f,     // -100..100
  val shadows: Float = 0f,        // -100..100
  val vignette: Float = 0f,       // 0..100
  val sharpness: Float = 0f,      // 0..100
  val filmGrain: Float = 0f,      // 0..100
  val glitchRgb: Float = 0f,      // 0..100
  val bloomGlow: Float = 0f,      // 0..100
  val blur: Float = 0f,           // 0..100
  val autoEnhance: Float = 0f,    // 0..100
  val selectedPreset: FilterPreset = FilterPreset.ORIGINAL,
  val selectedCrop: CropAspect = CropAspect.ORIGINAL,
  val lutIntensity: Float = 100f,
  // Smart Non-AI Algorithmic Photography & Video State
  val focusPeaking: Boolean = false,
  val zebraStripes: Boolean = false,
  val horizonTiltDeg: Float = 0f,
  val gyroSmoothing: Float = 0f
)

data class HistoryEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val adjustments: AdjustmentsState,
  val descriptionAr: String,
  val descriptionEn: String,
  val timestampMs: Long = System.currentTimeMillis()
)

enum class TrackType(val titleAr: String, val colorHex: Long) {
  VIDEO("مسار الفيديو 4K", 0xFF0284C7),
  AUDIO("مسار الصوت والمؤثرات", 0xFF10B981),
  OVERLAY("النصوص والطبقات", 0xFFF59E0B),
  FX("تأثيرات بصرية", 0xFF8B5CF6)
}

data class TimelineClip(
  val id: String,
  val trackType: TrackType,
  val title: String,
  val startMs: Long,
  val durationMs: Long,
  val speed: Float = 1.0f,
  val volume: Float = 1.0f,
  val isMuted: Boolean = false,
  val filterPreset: FilterPreset = FilterPreset.ORIGINAL,
  val filterIntensity: Float = 100f,
  /**
   * Real media source identifier for AUDIO clips:
   * - "sfx:<id>"  → a procedurally synthesized SFX from SfxSynthesizer
   * - "uri:<...>" → an imported audio/document URI
   * null for VIDEO/OVERLAY/FX clips that do not carry their own audio source.
   */
  val sourceId: String? = null
)

/**
 * A real editing project backed by an actual media file on the device.
 * The fake "cloud" fields were removed: this app performs 100% local
 * on-device editing, and `fileSizeBytes` stores the REAL size of the
 * source media file (0 until media is imported or captured).
 */
@Entity(tableName = "projects")
data class ProjectEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val mediaType: String, // "PHOTO" or "VIDEO"
  val durationMs: Long,
  val width: Int = 0,
  val height: Int = 0,
  val fps: Int = 0,
  val fileSizeBytes: Long = 0L,
  val lastModified: Long = System.currentTimeMillis(),
  /** Real media location (content:// or file:// URI). Null until media is attached. */
  val mediaUri: String? = null,
  /** Display label like "UHD_4K" / "FHD_1080P" or "1920x1080" for photos. */
  val resolution: String = "FHD_1080P"
)

sealed interface AutoSaveStatus {
  data object Idle : AutoSaveStatus
  data object Saving : AutoSaveStatus
  data class Saved(val timestampMs: Long) : AutoSaveStatus
  data class Error(val message: String) : AutoSaveStatus
}

@Entity(tableName = "editing_sessions")
data class EditingSessionEntity(
  @PrimaryKey val projectId: Long,
  val exposure: Float = 0f,
  val contrast: Float = 0f,
  val brightness: Float = 0f,
  val saturation: Float = 0f,
  val temperature: Float = 0f,
  val tint: Float = 0f,
  val highlights: Float = 0f,
  val shadows: Float = 0f,
  val vignette: Float = 0f,
  val sharpness: Float = 0f,
  val filmGrain: Float = 0f,
  val glitchRgb: Float = 0f,
  val bloomGlow: Float = 0f,
  val blur: Float = 0f,
  val autoEnhance: Float = 0f,
  val selectedPreset: String = "ORIGINAL",
  val selectedCrop: String = "ORIGINAL",
  val lutIntensity: Float = 100f,
  val focusPeaking: Boolean = false,
  val zebraStripes: Boolean = false,
  val horizonTiltDeg: Float = 0f,
  val gyroSmoothing: Float = 0f,
  val playheadMs: Long = 0L,
  val trimStartMs: Long = 0L,
  val trimEndMs: Long = 30000L,
  val editorMode: String = "BEGINNER",
  val lastSavedMs: Long = System.currentTimeMillis()
)

fun EditingSessionEntity.toAdjustmentsState(): AdjustmentsState {
  return AdjustmentsState(
    exposure = exposure,
    contrast = contrast,
    brightness = brightness,
    saturation = saturation,
    temperature = temperature,
    tint = tint,
    highlights = highlights,
    shadows = shadows,
    vignette = vignette,
    sharpness = sharpness,
    filmGrain = filmGrain,
    glitchRgb = glitchRgb,
    bloomGlow = bloomGlow,
    blur = blur,
    autoEnhance = autoEnhance,
    selectedPreset = try { FilterPreset.valueOf(selectedPreset) } catch (e: Exception) { FilterPreset.ORIGINAL },
    selectedCrop = try { CropAspect.valueOf(selectedCrop) } catch (e: Exception) { CropAspect.ORIGINAL },
    lutIntensity = lutIntensity,
    focusPeaking = focusPeaking,
    zebraStripes = zebraStripes,
    horizonTiltDeg = horizonTiltDeg,
    gyroSmoothing = gyroSmoothing
  )
}

fun AdjustmentsState.toSessionEntity(
  projectId: Long,
  playheadMs: Long,
  trimStartMs: Long,
  trimEndMs: Long,
  editorMode: EditorMode
): EditingSessionEntity {
  return EditingSessionEntity(
    projectId = projectId,
    exposure = exposure,
    contrast = contrast,
    brightness = brightness,
    saturation = saturation,
    temperature = temperature,
    tint = tint,
    highlights = highlights,
    shadows = shadows,
    vignette = vignette,
    sharpness = sharpness,
    filmGrain = filmGrain,
    glitchRgb = glitchRgb,
    bloomGlow = bloomGlow,
    blur = blur,
    autoEnhance = autoEnhance,
    selectedPreset = selectedPreset.name,
    selectedCrop = selectedCrop.name,
    lutIntensity = lutIntensity,
    focusPeaking = focusPeaking,
    zebraStripes = zebraStripes,
    horizonTiltDeg = horizonTiltDeg,
    gyroSmoothing = gyroSmoothing,
    playheadMs = playheadMs,
    trimStartMs = trimStartMs,
    trimEndMs = trimEndMs,
    editorMode = editorMode.name,
    lastSavedMs = System.currentTimeMillis()
  )
}

