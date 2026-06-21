package com.henjicc.swiftformat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.henjicc.swiftformat.core.designsystem.SwiftFormatTheme
import com.henjicc.swiftformat.ui.navigation.SwiftFormatApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwiftFormatTheme {
                SwiftFormatApp()
            }
        }
    }
}
