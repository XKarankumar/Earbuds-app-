package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BudsStudioViewModel
import com.example.ui.screens.KaranBudsStudioRoute
import com.example.ui.theme.KaranBudsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KaranBudsTheme {
                val studioViewModel: BudsStudioViewModel = viewModel()
                KaranBudsStudioRoute(viewModel = studioViewModel)
            }
        }
    }
}
