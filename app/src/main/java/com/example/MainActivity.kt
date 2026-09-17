package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppLanguage
import com.example.ui.MainScreen
import com.example.ui.NetworkViewModel
import com.example.ui.theme.NetDiagTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: NetworkViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle initial deep link if app launched via URI
        intent?.data?.let { uri ->
            viewModel.handleDeepLink(uri)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LocalizedApp(language = uiState.selectedLanguage) {
                NetDiagTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            viewModel.handleDeepLink(uri)
        }
    }
}

@Composable
fun LocalizedApp(
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val currentConfig = LocalConfiguration.current
    val currentContext = LocalContext.current

    val targetLocale = when (language) {
        AppLanguage.ENGLISH -> Locale.ENGLISH
        AppLanguage.CHINESE -> Locale.SIMPLIFIED_CHINESE
        AppLanguage.SYSTEM -> null
    }

    if (targetLocale != null) {
        val localizedConfig = remember(targetLocale, currentConfig) {
            Configuration(currentConfig).apply {
                setLocale(targetLocale)
                setLayoutDirection(targetLocale)
            }
        }
        val localizedContext = remember(targetLocale, currentContext) {
            currentContext.createConfigurationContext(localizedConfig)
        }

        CompositionLocalProvider(
            LocalConfiguration provides localizedConfig,
            LocalContext provides localizedContext
        ) {
            content()
        }
    } else {
        content()
    }
}
