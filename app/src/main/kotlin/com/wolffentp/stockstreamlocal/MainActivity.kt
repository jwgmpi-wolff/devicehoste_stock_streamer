package com.wolffentp.stockstreamlocal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wolffentp.stockstreamlocal.ui.navigation.AppNavHost
import com.wolffentp.stockstreamlocal.ui.theme.StockStreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockStreamTheme {
                AppNavHost()
            }
        }
    }
}
