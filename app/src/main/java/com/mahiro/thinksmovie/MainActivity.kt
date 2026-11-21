package com.mahiro.thinksmovie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahiro.thinksmovie.ui.HomeScreen
import com.mahiro.thinksmovie.ui.PlayerScreen
import com.mahiro.thinksmovie.ui.ThinksMovieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThinksMovieTheme {
                Surface(Modifier.fillMaxSize()) {
                    val vm: MainViewModel = viewModel()
                    val gridState = rememberLazyGridState() // posisi gulir bertahan saat kembali dari pemutar
                    if (vm.dramaIndex >= 0) PlayerScreen(vm) else HomeScreen(vm, gridState)
                }
            }
        }
    }
}
