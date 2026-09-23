package com.lumina.studio.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lumina.studio.R
import com.lumina.studio.data.model.AdjustmentsState
import com.lumina.studio.data.model.FilterPreset
import com.lumina.studio.engine.ColorMatrixEngine
import com.lumina.studio.engine.SfxSynthesizer
import com.lumina.studio.ui.StudioViewModel
import com.lumina.studio.ui.theme.AudioTrackColor
import com.lumina.studio.ui.theme.CyberGold
import com.lumina.studio.ui.theme.ElectricCyan
import com.lumina.studio.ui.theme.EmeraldGreen
import com.lumina.studio.ui.theme.NeonViolet
import com.lumina.studio.ui.theme.ObsidianBg
import com.lumina.studio.ui.theme.ObsidianBorder
import com.lumina.studio.ui.theme.ObsidianSurface
import com.lumina.studio.ui.theme.ObsidianSurfaceElevated
import com.lumina.studio.ui.theme.SunsetCoral
import com.lumina.studio.ui.theme.TextMuted
import com.lumina.studio.ui.theme.TextPrimary
import com.lumina.studio.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Data representation of a Cinematic 3D LUT preset in the AI Cine Lab.
 */
data class CineLutPreset(
  val id: String,
  val nameAr: String,
  val nameEn: String,
  val categoryAr: String,
  val descriptionAr: String,
  val colorPalette: List<Color>,
  val basePreset: FilterPreset,
  val defaultAdjustments: AdjustmentsState
)

/**
 * Data representation of an AI One-Tap Smart Enhancer.
 */
data class AiEnhancerItem(
  val id: String,
  val titleAr: String,
  val titleEn: String,
  val icon: ImageVector,
  val descriptionAr: String,
  val accentColor: Color,
  val adjustments: AdjustmentsState
)

/**
 * Data representation of a royalty-free SFX / Music track.
 */
data class SfxAudioItem(
  val id: String,
  val titleAr: String,
  val titleEn: String,
  val categoryAr: String,
  val durationMs: Long,
  val icon: ImageVector,
  val accentColor: Color
)

@Composable
fun AiCineLabScreen(
  viewModel: StudioViewModel,
  onNavigateToStudio: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedSectionIndex by remember { mutableIntStateOf(0) } // 0: LUTs, 1: AI Enhancers, 2: SFX Library
  val sectionTitles = listOf("فلاتر 3D LUTs", "محسنات الذكاء", "مكتبة المؤثرات SFX")

  // Compare Dialog state for live preview
  var previewLut by remember { mutableStateOf<CineLutPreset?>(null) }
  var currentlyPlayingSfxId by remember { mutableStateOf<String?>(null) }

  // Stop REAL SFX playback when leaving the lab
  androidx.compose.runtime.DisposableEffect(Unit) {
    onDispose { SfxSynthesizer.stopPreview() }
  }

  // High-Grade Curated Cinematic 3D LUTs Library
  val cineLuts = remember {
    listOf(
      CineLutPreset(
        id = "lut_teal_orange",
        nameAr = "تيل وبرتقالي هوليوود",
        nameEn = "Teal & Orange Hollywood 4K",
        categoryAr = "سينمائي هوليوود",
        descriptionAr = "التباين الأسطوري لأفلام الخيال العلمي والأكشن، سماء زرقاء عميقة مع ألوان بشرة دافئة غنية.",
        colorPalette = listOf(Color(0xFF005F73), Color(0xFF0A9396), Color(0xFFEE9B00), Color(0xFFCA6702)),
        basePreset = FilterPreset.TEAL_ORANGE,
        defaultAdjustments = AdjustmentsState(
          temperature = -15f,
          tint = 8f,
          contrast = 24f,
          saturation = 18f,
          highlights = -12f,
          shadows = 15f,
          vignette = 30f,
          selectedPreset = FilterPreset.TEAL_ORANGE
        )
      ),
      CineLutPreset(
        id = "lut_kodak_portra",
        nameAr = "كوداك بورترا 400 فينتاج",
        nameEn = "Kodak Portra 400 Vintage",
        categoryAr = "فنتدج ريترو",
        descriptionAr = "دفء الفيلم الكلاسيكي الأنالوج، مع تدريج ناعم لألوان البشرة وحبيبات فيلم 35mm أصيلة.",
        colorPalette = listOf(Color(0xFFE9D8A6), Color(0xFFEE9B00), Color(0xFFBB3E03), Color(0xFF9B2226)),
        basePreset = FilterPreset.KODAK_PORTRA,
        defaultAdjustments = AdjustmentsState(
          temperature = 22f,
          tint = -6f,
          contrast = 14f,
          saturation = -10f,
          highlights = -18f,
          shadows = 20f,
          filmGrain = 38f,
          vignette = 25f,
          selectedPreset = FilterPreset.KODAK_PORTRA
        )
      ),
      CineLutPreset(
        id = "lut_cyberpunk_neon",
        nameAr = "سايبربانك نيون طوكيو",
        nameEn = "Tokyo Cyberpunk Neon",
        categoryAr = "الخيال العلمي",
        descriptionAr = "توهج النيون البنفسجي والسيان الساطع لليل المدينة المستقبلي مع تأثيرات وميض RGB.",
        colorPalette = listOf(Color(0xFF7209B7), Color(0xFFF72585), Color(0xFF4CC9F0), Color(0xFF4361EE)),
        basePreset = FilterPreset.CYBERPUNK,
        defaultAdjustments = AdjustmentsState(
          temperature = -28f,
          tint = 32f,
          contrast = 35f,
          saturation = 32f,
          bloomGlow = 40f,
          glitchRgb = 18f,
          vignette = 35f,
          selectedPreset = FilterPreset.CYBERPUNK
        )
      ),
      CineLutPreset(
        id = "lut_golden_hour",
        nameAr = "الساعة الذهبية وسحر الغروب",
        nameEn = "Golden Hour Sahara Glow",
        categoryAr = "سينمائي هوليوود",
        descriptionAr = "إضاءة شمس الغروب الدافئة، تدفق برونزي على التفاصيل وألوان ذهبية مشبعة تنبض بالحياة.",
        colorPalette = listOf(Color(0xFFFFB703), Color(0xFFFB8500), Color(0xFFD4A373), Color(0xFFFAEDCD)),
        basePreset = FilterPreset.GOLDEN_HOUR,
        defaultAdjustments = AdjustmentsState(
          temperature = 36f,
          tint = 12f,
          contrast = 18f,
          saturation = 22f,
          brightness = 10f,
          bloomGlow = 25f,
          vignette = 20f,
          selectedPreset = FilterPreset.GOLDEN_HOUR
        )
      ),
      CineLutPreset(
        id = "lut_film_noir",
        nameAr = "سينما نوار كلاسيك مونوكروم",
        nameEn = "Classic Film Noir B&W",
        categoryAr = "فنتدج ريترو",
        descriptionAr = "أبيض وأسود عالي التباين مستوحى من كلاسيكيات هوليوود الأسطورية مع ظلال داكنة وحبيبات فضية.",
        colorPalette = listOf(Color(0xFF000000), Color(0xFF495057), Color(0xFFCED4DA), Color(0xFFFFFFFF)),
        basePreset = FilterPreset.FILM_NOIR,
        defaultAdjustments = AdjustmentsState(
          contrast = 45f,
          saturation = -100f,
          brightness = -8f,
          highlights = 15f,
          shadows = -25f,
          filmGrain = 42f,
          vignette = 50f,
          sharpness = 28f,
          selectedPreset = FilterPreset.FILM_NOIR
        )
      ),
      CineLutPreset(
        id = "lut_emerald_forest",
        nameAr = "غابات الزمرد الوثائقية",
        nameEn = "Emerald Documentary 4K",
        categoryAr = "الطبيعة والبورتريه",
        descriptionAr = "تدرجات خضراء عميقة ونقية مستوحاة من الأفلام الوثائقية العالمية تبرز جمال الطبيعة والمسطحات.",
        colorPalette = listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF52B788), Color(0xFF74C69D)),
        basePreset = FilterPreset.EMERALD,
        defaultAdjustments = AdjustmentsState(
          temperature = -10f,
          tint = -14f,
          contrast = 18f,
          saturation = 15f,
          sharpness = 30f,
          highlights = -10f,
          shadows = 12f,
          selectedPreset = FilterPreset.EMERALD
        )
      ),
      CineLutPreset(
        id = "lut_fuji_velvia",
        nameAr = "فوجي فيلم فيلفيا ألوان حية",
        nameEn = "Fuji Velvia 50 Vivid",
        categoryAr = "الطبيعة والبورتريه",
        descriptionAr = "تشبع لوني مذهل مع نقاء فائق لدرجات السماء والزهور، المحبب لدى مصوري الطبيعة في اليابان.",
        colorPalette = listOf(Color(0xFF023E8A), Color(0xFF0096C7), Color(0xFF48CAE4), Color(0xFFADE8F4)),
        basePreset = FilterPreset.FUJI_CHROME,
        defaultAdjustments = AdjustmentsState(
          contrast = 20f,
          saturation = 28f,
          brightness = 6f,
          temperature = 4f,
          sharpness = 22f,
          selectedPreset = FilterPreset.FUJI_CHROME
        )
      ),
      CineLutPreset(
        id = "lut_pastel_dream",
        nameAr = "أنمي دريم باستيل حالم",
        nameEn = "Anime Pastel Dreamscape",
        categoryAr = "الخيال العلمي",
        descriptionAr = "ألوان الباستيل الناعمة والدرجات الوردية الساحرة المستوحاة من لوحات أفلام الأنمي الياباني الفنية.",
        colorPalette = listOf(Color(0xFFFFAFCC), Color(0xFFFFC8DD), Color(0xFFBDE0FE), Color(0xFFA2D2FF)),
        basePreset = FilterPreset.PASTEL_DREAM,
        defaultAdjustments = AdjustmentsState(
          temperature = 10f,
          tint = 18f,
          contrast = -10f,
          saturation = 12f,
          brightness = 15f,
          bloomGlow = 32f,
          blur = 8f,
          selectedPreset = FilterPreset.PASTEL_DREAM
        )
      )
    )
  }

  // AI Smart Enhancers
  val aiEnhancers = remember {
    listOf(
      AiEnhancerItem(
        id = "ai_hdr_vivid",
        titleAr = "تحسين المدى الديناميكي AI Dynamic HDR",
        titleEn = "AI Smart Dynamic Range",
        icon = Icons.Default.AutoAwesome,
        descriptionAr = "إعادة التوازن الذكي بين المناطق المضيئة والظلال العميقة لإنتاج فيديو فائق الوضوح.",
        accentColor = ElectricCyan,
        adjustments = AdjustmentsState(
          autoEnhance = 80f,
          contrast = 20f,
          highlights = -20f,
          shadows = 28f,
          sharpness = 26f,
          saturation = 12f
        )
      ),
      AiEnhancerItem(
        id = "ai_relighting",
        titleAr = "إضاءة استوديو ثلاثية الأبعاد AI 3D Relighting",
        titleEn = "AI Cinematic Studio Relighting",
        icon = Icons.Default.MovieFilter,
        descriptionAr = "محاكاة الإضاءة الجانبية وتوهج الحواف السينمائي لإبراز العناصر في الإطار بجاذبية احترافية.",
        accentColor = CyberGold,
        adjustments = AdjustmentsState(
          brightness = 12f,
          bloomGlow = 35f,
          contrast = 18f,
          vignette = 28f,
          temperature = 8f
        )
      ),
      AiEnhancerItem(
        id = "ai_face_glow",
        titleAr = "تجميل البورتريه وتنعيم البشرة AI Face Glow",
        titleEn = "AI Portrait Glamour & Clarity",
        icon = Icons.Default.Visibility,
        descriptionAr = "تنعيم ذكي لملامح الوجه وإبراز بريق العينين مع الحفاظ على تفاصيل الشعر والإضاءة.",
        accentColor = SunsetCoral,
        adjustments = AdjustmentsState(
          temperature = 12f,
          tint = 8f,
          bloomGlow = 22f,
          sharpness = 18f,
          brightness = 8f,
          saturation = 10f
        )
      ),
      AiEnhancerItem(
        id = "ai_denoise",
        titleAr = "إزالة التشويش الليلي AI Crystal De-Noise",
        titleEn = "AI Low-Light Noise Suppression",
        icon = Icons.Default.Compare,
        descriptionAr = "تنقية اللقطات الليلية ذات الحساسية العالية (High ISO) مع الحفاظ التام على حدة الحواف.",
        accentColor = EmeraldGreen,
        adjustments = AdjustmentsState(
          sharpness = 38f,
          contrast = 12f,
          brightness = 4f,
          filmGrain = 0f,
          blur = 0f
        )
      ),
      AiEnhancerItem(
        id = "ai_cinematic_mood",
        titleAr = "التلوين السينمائي التلقائي AI Color Match",
        titleEn = "AI Color Harmony & Mood",
        icon = Icons.Default.AutoAwesome,
        descriptionAr = "مطابقة درجات الألوان وتوزيع التدرج اللوني سينمائياً تلقائياً وفقاً لمعايير هوليوود.",
        accentColor = NeonViolet,
        adjustments = AdjustmentsState(
          selectedPreset = FilterPreset.TEAL_ORANGE,
          contrast = 22f,
          saturation = 15f,
          vignette = 30f,
          highlights = -14f,
          shadows = 16f
        )
      )
    )
  }

  // Royalty-Free Sound Effects & Audio Tracks
  val sfxAudioItems = remember {
    listOf(
      SfxAudioItem(
        id = "sfx_whoosh",
        titleAr = "انتقال سويش سينمائي فائق السرعة",
        titleEn = "Cinematic Whoosh Transition FX",
        categoryAr = "انتقالات",
        durationMs = 2500L,
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        accentColor = ElectricCyan
      ),
      SfxAudioItem(
        id = "sfx_boom",
        titleAr = "ضربة بيس سفلية عميقة Sub Boom",
        titleEn = "Deep Sub Bass Impact Drop",
        categoryAr = "درامي سينمائي",
        durationMs = 4200L,
        icon = Icons.Default.GraphicEq,
        accentColor = SunsetCoral
      ),
      SfxAudioItem(
        id = "sfx_film_reel",
        titleAr = "صوت بكرة فيلم ريترو 35mm",
        titleEn = "Vintage 35mm Film Reel Ambient",
        categoryAr = "محيطي فنتدج",
        durationMs = 8000L,
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        accentColor = CyberGold
      ),
      SfxAudioItem(
        id = "sfx_shutter",
        titleAr = "مصراع كاميرا احترافية كلاسيكية",
        titleEn = "Pro Mechanical Camera Shutter",
        categoryAr = "مؤثرات واقعية",
        durationMs = 1200L,
        icon = Icons.Default.GraphicEq,
        accentColor = EmeraldGreen
      ),
      SfxAudioItem(
        id = "sfx_rain",
        titleAr = "مطر ورعد سينمائي هادئ",
        titleEn = "Cinematic Rain & Distant Thunder",
        categoryAr = "محيطي فنتدج",
        durationMs = 15000L,
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        accentColor = ElectricCyan
      ),
      SfxAudioItem(
        id = "sfx_cyber_riser",
        titleAr = "تصاعد سايبربانك سينث رايزر 80s",
        titleEn = "80s Cyberpunk Synth Riser FX",
        categoryAr = "درامي سينمائي",
        durationMs = 5500L,
        icon = Icons.Default.GraphicEq,
        accentColor = NeonViolet
      ),
      SfxAudioItem(
        id = "sfx_vinyl",
        titleAr = "شوشرة أسطوانة فينيل دافئة",
        titleEn = "Warm Vinyl Crackle & Lo-Fi Hiss",
        categoryAr = "محيطي فنتدج",
        durationMs = 10000L,
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        accentColor = CyberGold
      )
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ObsidianBg)
      .testTag("ai_cine_lab_screen")
  ) {
    // 1. Top Header Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(ObsidianSurface)
        .border(1.dp, ObsidianBorder)
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                Brush.linearGradient(
                  colors = listOf(NeonViolet, ElectricCyan)
                )
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesome,
              contentDescription = "أيقونة مختبر الذكاء",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "مختبر الذكاء وفلاتر السينما",
              color = TextPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "فلاتر 3D LUTs • محسنات الذكاء • مكتبة الصوتيات SFX",
              color = TextMuted,
              fontSize = 11.sp
            )
          }
        }

        // Action button to go directly to Studio
        Button(
          onClick = onNavigateToStudio,
          colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("open_studio_from_lab_button")
        ) {
          Text(
            text = "فتح الاستوديو",
            color = Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 2. Section Selector Tabs (LUTs, AI Enhancers, SFX)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(ObsidianSurfaceElevated)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      sectionTitles.forEachIndexed { index, title ->
        val isSelected = selectedSectionIndex == index
        val bgBrush = if (isSelected) {
          Brush.horizontalGradient(listOf(NeonViolet, ElectricCyan))
        } else {
          Brush.linearGradient(listOf(ObsidianSurface, ObsidianSurface))
        }
        val border = if (isSelected) ElectricCyan else ObsidianBorder

        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgBrush)
            .border(1.dp, border, RoundedCornerShape(8.dp))
            .clickable { selectedSectionIndex = index }
            .padding(vertical = 10.dp)
            .testTag("cine_lab_tab_$index"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = title,
            color = if (isSelected) Color.White else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
          )
        }
      }
    }

    // 3. Section Content
    Box(modifier = Modifier.weight(1f)) {
      when (selectedSectionIndex) {
        0 -> {
          // Section 0: 3D LUTs Presets
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            item {
              CineLabHeroBanner(
                title = "كتالوج تلوين هوليوود 3D LUTs",
                description = "مستوحى من أشهر أفلام السينما العالمية، يمكنك معاينة التدرج اللوني حياً وتطبيقه بلمسة واحدة على مشروعك الحالي في الاستوديو.",
                gradientColors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
              )
            }

            items(cineLuts) { lut ->
              CineLutCard(
                lut = lut,
                onLivePreview = { previewLut = lut },
                onApply = {
                  viewModel.applyAiEnhancementPreset(lut.nameAr, lut.defaultAdjustments)
                  Toast.makeText(
                    context,
                    "تم تطبيق فلتر «${lut.nameAr}» بنجاح على مشروع الاستوديو!",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
          }
        }

        1 -> {
          // Section 1: AI Smart Enhancers
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            item {
              CineLabHeroBanner(
                title = "محركات التحسين الذكي فائق الدقة",
                description = "خوارزميات معالجة الصورة الذكية لتحسين الإضاءة والمدى الديناميكي HDR وإزالة التشويش وتجميل البشرة دون تشويه التفاصيل الأصلية.",
                gradientColors = listOf(Color(0xFF134E4A), Color(0xFF064E3B))
              )
            }

            items(aiEnhancers) { enhancer ->
              AiEnhancerCard(
                enhancer = enhancer,
                onApply = {
                  viewModel.applyAiEnhancementPreset(enhancer.titleAr, enhancer.adjustments)
                  Toast.makeText(
                    context,
                    "تم تفعيل «${enhancer.titleAr}» في الاستوديو!",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
          }
        }

        2 -> {
          // Section 2: SFX Audio Library
          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            item {
              CineLabHeroBanner(
                title = "مكتبة المؤثرات الصوتية والموسيقى SFX",
                description = "مؤثرات صوتية هوليوودية مجانية للإنتاج، استمع للمعاينة وأضف المقاطع الصوتية مباشرة إلى المسار الزمني (Timeline) لمشروعك.",
                gradientColors = listOf(Color(0xFF4C1D95), Color(0xFF1E1B4B))
              )
            }

            items(sfxAudioItems) { sfx ->
              val isPlaying = currentlyPlayingSfxId == sfx.id

              SfxAudioCard(
                item = sfx,
                isPlaying = isPlaying,
                onTogglePlay = {
                  if (isPlaying) {
                    // Stop the real playback
                    SfxSynthesizer.stopPreview()
                    currentlyPlayingSfxId = null
                  } else {
                    // REAL audio preview through MediaPlayer
                    SfxSynthesizer.playPreview(context, sfx.id)
                    currentlyPlayingSfxId = sfx.id
                  }
                },
                onAddToTimeline = {
                  // Adds a REAL audio clip bound to the synthesized WAV source
                  viewModel.addAudioTrackClip(sfx.titleAr, sfx.durationMs, "sfx:${sfx.id}")
                  Toast.makeText(
                    context,
                    "تمت إضافة «${sfx.titleAr}» إلى مسار الصوت في التايم لاين!",
                    Toast.LENGTH_SHORT
                  ).show()
                }
              )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
          }
        }
      }
    }
  }

  // 4. Live Before / After Comparison Dialog
  previewLut?.let { lut ->
    LiveCompareDialog(
      lut = lut,
      onDismiss = { previewLut = null },
      onApplyAndClose = {
        viewModel.applyAiEnhancementPreset(lut.nameAr, lut.defaultAdjustments)
        previewLut = null
        Toast.makeText(
          context,
          "تم تطبيق فلتر «${lut.nameAr}» على مشروع الاستوديو!",
          Toast.LENGTH_SHORT
        ).show()
        onNavigateToStudio()
      }
    )
  }
}

/**
 * Hero Banner for each Cine Lab section.
 */
@Composable
private fun CineLabHeroBanner(
  title: String,
  description: String,
  gradientColors: List<Color>,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Brush.linearGradient(gradientColors))
      .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
      .padding(16.dp)
  ) {
    Column {
      Text(
        text = title,
        color = TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = description,
        color = TextSecondary,
        fontSize = 12.sp,
        lineHeight = 18.sp
      )
    }
  }
}

/**
 * Card for 3D Cinematic LUT Preset.
 */
@Composable
private fun CineLutCard(
  lut: CineLutPreset,
  onLivePreview: () -> Unit,
  onApply: () -> Unit,
  modifier: Modifier = Modifier
) {
  var intensity by remember { mutableFloatStateOf(1.0f) }

  Card(
    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
    modifier = modifier
      .fillMaxWidth()
      .testTag("lut_card_${lut.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = lut.nameAr,
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(ObsidianSurfaceElevated)
                .border(0.5.dp, ObsidianBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = lut.categoryAr,
                color = CyberGold,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
          Text(
            text = lut.nameEn,
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
          )
        }

        // Color Palette Swatches
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          lut.colorPalette.forEach { color ->
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = lut.descriptionAr,
        color = TextSecondary,
        fontSize = 12.sp,
        lineHeight = 17.sp
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Intensity Slider
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "كثافة الفلتر (LUT Strength): ${(intensity * 100).roundToInt()}%",
          color = TextMuted,
          fontSize = 11.sp
        )
      }
      Slider(
        value = intensity,
        onValueChange = { intensity = it },
        valueRange = 0.2f..1.0f,
        colors = SliderDefaults.colors(
          thumbColor = ElectricCyan,
          activeTrackColor = ElectricCyan,
          inactiveTrackColor = ObsidianBorder
        ),
        modifier = Modifier.height(28.dp)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onLivePreview,
          colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .testTag("preview_lut_${lut.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Compare,
            contentDescription = "معاينة حية",
            tint = ElectricCyan,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "معاينة حية قبل/بعد", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onApply,
          colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("apply_lut_${lut.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "تطبيق الفلتر",
            tint = Color.Black,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(text = "تطبيق على الاستوديو", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

/**
 * Card for AI Smart Enhancer.
 */
@Composable
private fun AiEnhancerCard(
  enhancer: AiEnhancerItem,
  onApply: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder),
    modifier = modifier
      .fillMaxWidth()
      .testTag("enhancer_card_${enhancer.id}")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(46.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(enhancer.accentColor.copy(alpha = 0.18f))
          .border(1.dp, enhancer.accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = enhancer.icon,
          contentDescription = enhancer.titleAr,
          tint = enhancer.accentColor,
          modifier = Modifier.size(24.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = enhancer.titleAr,
          color = TextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = enhancer.descriptionAr,
          color = TextSecondary,
          fontSize = 11.sp,
          lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = onApply,
        colors = ButtonDefaults.buttonColors(containerColor = enhancer.accentColor),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.testTag("apply_enhancer_${enhancer.id}")
      ) {
        Text(
          text = "تفعيل",
          color = Color.Black,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}

/**
 * Card for Royalty-Free SFX item with interactive playback waveform.
 */
@Composable
private fun SfxAudioCard(
  item: SfxAudioItem,
  isPlaying: Boolean,
  onTogglePlay: () -> Unit,
  onAddToTimeline: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaying) item.accentColor else ObsidianBorder),
    modifier = modifier
      .fillMaxWidth()
      .testTag("sfx_card_${item.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          // Play / Pause Circle Button
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(if (isPlaying) item.accentColor else ObsidianSurfaceElevated)
              .border(1.dp, item.accentColor, CircleShape)
              .clickable { onTogglePlay() }
              .testTag("play_sfx_${item.id}"),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "إيقاف" else "تشغيل المعاينة",
              tint = if (isPlaying) Color.Black else item.accentColor,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          Column {
            Text(
              text = item.titleAr,
              color = TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${item.durationMs / 1000f} ثانية • ${item.categoryAr}",
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }
        }

        // Add to timeline button
        Button(
          onClick = onAddToTimeline,
          colors = ButtonDefaults.buttonColors(containerColor = AudioTrackColor),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("add_sfx_timeline_${item.id}")
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "إضافة للتايم لاين",
            tint = Color.Black,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "إضافة للتايم لاين", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }

      // REAL waveform preview rendered from the synthesized PCM envelope
      Spacer(modifier = Modifier.height(8.dp))
      AudioWaveformPreview(
        sfxId = item.id,
        isPlaying = isPlaying,
        accentColor = item.accentColor,
        modifier = Modifier
          .fillMaxWidth()
          .height(26.dp)
      )
    }
  }
}

/**
 * Waveform bars computed from the REAL PCM envelope of the generated SFX.
 * While playing, a play-head cursor sweeps across the real bars.
 */
@Composable
private fun AudioWaveformPreview(
  sfxId: String,
  isPlaying: Boolean,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  val context = androidx.compose.ui.platform.LocalContext.current
  val bars = remember(sfxId) {
    SfxSynthesizer.getEnvelope(context, sfxId)?.waveformBars(28)
  }
  var playProgress by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(isPlaying, sfxId) {
    if (isPlaying && bars != null) {
      while (playProgress < 1f) {
        delay(120)
        playProgress = (playProgress + 0.12f).coerceAtMost(1f)
      }
      playProgress = 0f
    }
  }

  Canvas(
    modifier = modifier
      .clip(RoundedCornerShape(6.dp))
      .background(ObsidianSurfaceElevated)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    val barCount = 28
    val barWidth = (size.width / (barCount * 1.6f)).coerceAtLeast(3f)
    val spacing = (size.width - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

    if (bars == null) {
      // Synthesis not finished yet — honest placeholder line
      drawLine(
        color = TextMuted.copy(alpha = 0.5f),
        start = Offset(0f, size.height / 2f),
        end = Offset(size.width, size.height / 2f),
        strokeWidth = 2f
      )
    } else {
      for (i in 0 until barCount) {
        val x = i * (barWidth + spacing)
        val barHeight = size.height * bars[i].coerceIn(0.06f, 1f)
        val y = (size.height - barHeight) / 2f
        val passed = isPlaying && (i.toFloat() / barCount) <= playProgress
        drawRoundRect(
          color = when {
            passed -> accentColor
            isPlaying -> accentColor.copy(alpha = 0.35f)
            else -> TextMuted.copy(alpha = 0.5f)
          },
          topLeft = Offset(x, y),
          size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
        )
      }
    }
  }
}

/**
 * Fullscreen Interactive Split-Screen Compare Dialog (Before / After).
 */
@Composable
private fun LiveCompareDialog(
  lut: CineLutPreset,
  onDismiss: () -> Unit,
  onApplyAndClose: () -> Unit
) {
  var splitRatio by remember { mutableFloatStateOf(0.5f) }
  val colorMatrix = remember(lut) {
    ColorMatrixEngine.createUnifiedMatrix(lut.defaultAdjustments)
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      color = ObsidianBg,
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
        .testTag("live_compare_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(14.dp)
      ) {
        // Dialog Top Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "المقارنة التفاعلية قبل / بعد",
              color = TextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "فلتر: ${lut.nameAr} (${lut.nameEn})",
              color = ElectricCyan,
              fontSize = 12.sp
            )
          }

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("close_compare_dialog_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "إغلاق",
              tint = TextSecondary
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Split Comparison Canvas
        BoxWithConstraints(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
            .testTag("compare_split_canvas")
        ) {
          val widthPx = constraints.maxWidth.toFloat()
          val heightPx = constraints.maxHeight.toFloat()
          val splitX = (widthPx * splitRatio).coerceIn(0f, widthPx)

          // 1. Raw Layer (Left side)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .drawWithContent {
                clipRect(left = 0f, top = 0f, right = splitX, bottom = heightPx) {
                  this@drawWithContent.drawContent()
                }
              }
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_sample_cinematic),
              contentDescription = "الصورة الأصلية قبل المعالجة",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }

          // 2. Graded Layer (Right side)
          Box(
            modifier = Modifier
              .fillMaxSize()
              .drawWithContent {
                clipRect(left = splitX, top = 0f, right = widthPx, bottom = heightPx) {
                  this@drawWithContent.drawContent()
                }
              }
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_sample_cinematic),
              contentDescription = "الصورة بعد تطبيق فلتر LUT",
              contentScale = ContentScale.Crop,
              colorFilter = ColorFilter.colorMatrix(colorMatrix),
              modifier = Modifier.fillMaxSize()
            )
          }

          // 3. Glowing Divider Line
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
              color = ElectricCyan,
              start = Offset(splitX, 0f),
              end = Offset(splitX, heightPx),
              strokeWidth = 3.dp.toPx()
            )
          }

          // 4. Interactive Drag Handle
          Box(
            modifier = Modifier
              .offset {
                IntOffset(
                  (splitX - 18.dp.roundToPx()).roundToInt(),
                  (heightPx / 2f - 18.dp.roundToPx()).roundToInt()
                )
              }
              .size(36.dp)
              .clip(CircleShape)
              .background(ElectricCyan)
              .border(2.dp, Color.White, CircleShape)
              .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                  change.consume()
                  val newRatio = (splitX + dragAmount.x) / widthPx
                  splitRatio = newRatio.coerceIn(0.05f, 0.95f)
                }
              },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.SwapHoriz,
              contentDescription = "سحب المقارنة",
              tint = Color.Black,
              modifier = Modifier.size(22.dp)
            )
          }

          // Floating Badges: "قبل (Original)" & "بعد (Graded)"
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(10.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color.Black.copy(alpha = 0.75f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "الأصل (قبل المعالجة)",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(10.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(NeonViolet.copy(alpha = 0.85f))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              text = "بعد التلوين (3D LUT)",
              color = Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dialog Bottom Actions
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceElevated),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(text = "إلغاء", color = TextSecondary, fontSize = 13.sp)
          }

          Button(
            onClick = onApplyAndClose,
            colors = ButtonDefaults.buttonColors(containerColor = CyberGold),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
              .weight(1.5f)
              .testTag("apply_and_open_studio_button")
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "تطبيق وفتح الاستوديو",
              tint = Color.Black,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "تطبيق وفتح في الاستوديو",
              color = Color.Black,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}
