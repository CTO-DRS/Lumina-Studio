package com.lumina.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lumina.studio.ui.MainAppScreen
import com.lumina.studio.ui.StudioViewModel
import com.lumina.studio.ui.theme.MyApplicationTheme
import com.lumina.studio.ui.theme.ObsidianBg

class MainActivity : ComponentActivity() {
  private val studioViewModel: StudioViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = ObsidianBg
        ) {
          MainAppScreen(viewModel = studioViewModel)
        }
      }
    }
  }
}

