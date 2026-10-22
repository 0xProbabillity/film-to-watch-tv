package com.filmtowatch.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.filmtowatch.tv.ui.AppViewModel
import com.filmtowatch.tv.ui.DetailsScreen
import com.filmtowatch.tv.ui.FilmTheme
import com.filmtowatch.tv.ui.HomeScreen
import com.filmtowatch.tv.ui.Screen
import com.filmtowatch.tv.ui.SupportScreen
import com.filmtowatch.tv.ui.SwipeScreen
import com.filmtowatch.tv.ui.Palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FilmTheme {
                App()
            }
        }
    }
}

@Composable
private fun App(vm: AppViewModel = viewModel()) {
    BackHandler(enabled = vm.backStack.size > 1) { vm.back() }
    Box(Modifier.fillMaxSize().background(Palette.Background)) {
        when (val screen = vm.backStack[vm.backStack.lastIndex]) {
            Screen.Home -> HomeScreen(vm)
            Screen.Swipe -> SwipeScreen(vm)
            Screen.Support -> SupportScreen(vm)
            is Screen.Details -> DetailsScreen(vm, screen)
        }
    }
}
