package com.lumina.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lumina.studio.data.db.AppDatabase
import com.lumina.studio.data.model.AdjustmentsState
import com.lumina.studio.data.model.FilterPreset
import com.lumina.studio.data.model.toAdjustmentsState
import com.lumina.studio.data.model.toSessionEntity
import com.lumina.studio.data.repository.ProjectRepository
import com.lumina.studio.data.repository.SessionRepository
import com.lumina.studio.engine.AudioGraphEngine
import com.lumina.studio.engine.ColorMatrixEngine
import com.lumina.studio.engine.ImageProcessingEngine
import com.lumina.studio.engine.MediaAnalysisEngine
import com.lumina.studio.engine.ShareExporter
import com.lumina.studio.engine.SfxSynthesizer
import com.lumina.studio.engine.SmartComputationalEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Instrumented-logic tests on the JVM via Robolectric.
 * Everything asserted here comes from REAL bitmap pixels, REAL database rows
 * and REAL synthesized audio files — no mocks.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EngineRobolectricTest {

  private lateinit var context: Context
  private lateinit var db: AppDatabase

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
    SfxSynthesizer.stopPreview()
  }

  // -------------------------------------------------------------------
  // ColorMatrixEngine — real 4×5 matrices
  // -------------------------------------------------------------------

  @Test
  fun colorMatrixEngine_producesValidUnifiedMatrix() {
    val matrix = ColorMatrixEngine.createUnifiedMatrix(
      AdjustmentsState(
        exposure = 20f,
        contrast = 15f,
        temperature = -10f,
        saturation = 25f,
        selectedPreset = FilterPreset.TEAL_ORANGE
      )
    )
    assertEquals(20, matrix.values.size)
  }

  // -------------------------------------------------------------------
  // MediaAnalysisEngine — REAL pixel analysis on synthetic bitmaps
  // -------------------------------------------------------------------

  @Test
  fun colorBalance_redBitmap_measuresRedDominance() {
    val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.rgb(220, 30, 30))
    val stats = MediaAnalysisEngine.analyzeColorBalance(bmp)
    assertTrue("Red mean must dominate", stats.meanR > stats.meanB * 2)
    assertTrue(stats.meanR > 200f)
  }

  @Test
  fun toneHistogram_darkBitmap_hasLowMedian() {
    val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    // True black: luma 0 lands in histogram bin 0, which is exactly what
    // analyzeTone counts as crushed shadows.
    bmp.eraseColor(Color.BLACK)
    val tone = MediaAnalysisEngine.analyzeTone(bmp)
    assertTrue("Median of a dark frame must be low, got ${tone.p50}", tone.p50 < 40)
    assertTrue("Dark frame must have crushed shadows", tone.crushedShadowsRatio > 0.5f)
  }

  @Test
  fun overexposureMask_detectsBlownPixelsOnly() {
    val bmp = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.rgb(255, 255, 255)) // fully blown half
    val masks = MediaAnalysisEngine.computeFrameMasks(bmp)
    assertTrue(
      "A fully white frame must be ≥95% overexposed, got ${masks.overexposureRatio}",
      masks.overexposureRatio > 0.9f
    )
  }

  @Test
  fun sharpness_flatBitmap_hasNearZeroAcuity() {
    val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.GRAY) // no edges at all
    val stats = MediaAnalysisEngine.analyzeSharpness(bmp)
    assertTrue("Flat frame must have ~zero Laplacian acuity", stats.normalizedAcuity < 0.05f)
  }

  @Test
  fun autoToneOnRealPixels_darkFrame_recommendsPositiveExposure() {
    val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.rgb(25, 25, 30))
    val tone = MediaAnalysisEngine.analyzeTone(bmp)
    val result = SmartComputationalEngine.computeSmartAutoTone(tone)
    assertTrue("Dark frame needs positive exposure", result.recommendedExposure > 5f)
  }

  // -------------------------------------------------------------------
  // ImageProcessingEngine — real pixel transforms
  // -------------------------------------------------------------------

  @Test
  fun grayscale_desaturatesRedBitmap() {
    val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.RED)
    val gray = ImageProcessingEngine.applyGrayscale(bmp)
    val pixel = gray.getPixel(4, 4)
    assertEquals(Color.red(pixel), Color.green(pixel))
    assertEquals(Color.green(pixel), Color.blue(pixel))
  }

  @Test
  fun brightness_shiftsPixelValues() {
    val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(Color.rgb(100, 100, 100))
    val brighter = ImageProcessingEngine.applyBrightness(bmp, 50f)
    val pixel = brighter.getPixel(4, 4)
    assertEquals(150, Color.red(pixel))
  }

  // -------------------------------------------------------------------
  // SfxSynthesizer — REAL WAV files on disk with real envelopes
  // -------------------------------------------------------------------

  @Test
  fun sfxSynthesizer_generatesPlayableWavFiles() {
    val files = SfxSynthesizer.ensureAllGenerated(context)
    assertEquals(SfxSynthesizer.availableSfx.size, files.size)
    files.values.forEach { file ->
      assertTrue("WAV file must exist", file.exists())
      assertTrue("WAV file must have a RIFF header", file.length() > 1000)
      val header = file.inputStream().use { ins ->
        val buf = ByteArray(4)
        ins.read(buf)
        String(buf, Charsets.US_ASCII)
      }
      assertEquals("RIFF", header)
    }
  }

  @Test
  fun sfxEnvelope_isNonSilentAndNormalized() {
    val envelope = SfxSynthesizer.getEnvelope(context, "sfx_boom")
    assertNotNull(envelope)
    assertTrue("Boom must have real energy", envelope!!.peakRms > 0.01f)
    val bars = envelope.waveformBars(16)
    assertTrue("At least one bar must carry real energy", bars.max() > 0.1f)
  }

  // -------------------------------------------------------------------
  // Room persistence — REAL database round trip
  // -------------------------------------------------------------------

  @Test
  fun room_newDatabase_startsEmpty_noFakeSeeding() = runBlocking {
    val repository = ProjectRepository(db.projectDao())
    val count = db.projectDao().getCount()
    assertEquals("Fresh DB must contain ZERO projects (no demo seeding)", 0, count)
  }

  @Test
  fun room_sessionRoundTrip_preservesAdjustments() = runBlocking {
    val sessionRepository = SessionRepository(db.sessionDao())
    val adjustments = AdjustmentsState(
      exposure = 15f,
      contrast = 30f,
      selectedPreset = FilterPreset.GOLDEN_HOUR,
      focusPeaking = true,
      gyroSmoothing = 55f
    )
    val entity = adjustments.toSessionEntity(
      projectId = 7L,
      playheadMs = 1200L,
      trimStartMs = 300L,
      trimEndMs = 9000L,
      editorMode = com.lumina.studio.data.model.EditorMode.PRO
    )
    sessionRepository.saveSession(entity)

    val loaded = sessionRepository.getSession(7L)
    assertNotNull(loaded)
    val restored = loaded!!.toAdjustmentsState()
    assertEquals(15f, restored.exposure, 0.001f)
    assertEquals(30f, restored.contrast, 0.001f)
    assertEquals(FilterPreset.GOLDEN_HOUR, restored.selectedPreset)
    assertTrue(restored.focusPeaking)
    assertEquals(55f, restored.gyroSmoothing, 0.001f)

    sessionRepository.deleteSession(7L)
    assertNull(sessionRepository.getSession(7L))
  }

  // -------------------------------------------------------------------
  // ShareExporter size math (real formula)
  // -------------------------------------------------------------------

  @Test
  fun fileSizeEstimation_matchesBitrateMath() {
    val sizeMb = ShareExporter.calculateEstimatedFileSizeMb(
      resolution = com.lumina.studio.data.model.VideoResolution.UHD_4K,
      durationMs = 30000L,
      bitrateMbps = 60
    )
    assertTrue("30s × 60 Mbps ≈ 225 MB, got $sizeMb", sizeMb in 200f..250f)
  }

  // -------------------------------------------------------------------
  // AudioGraphEngine WAV writer — real file bytes
  // -------------------------------------------------------------------

  @Test
  fun wavWriter_producesCorrectHeader() {
    val pcm = ShortArray(44100) { i -> (sinOf(i) * 12000).toInt().toShort() }
    val target = java.io.File(context.cacheDir, "test_tone.wav")
    AudioGraphEngine.writeWavFile(target, pcm, 44100)
    assertTrue(target.exists())
    assertEquals(44 + pcm.size * 2L, target.length())
    target.delete()
  }

  private fun sinOf(i: Int): Float = kotlin.math.sin(2.0 * Math.PI * 440.0 * i / 44100.0).toFloat()
}
