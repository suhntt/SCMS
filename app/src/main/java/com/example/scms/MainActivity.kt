package com.example.scms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.scms.ui.theme.SCMSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ RESTORE USER SESSION
        val sessionManager = SessionManager(this)
        UserSession.currentUser = sessionManager.getUser()

        setContent {
            SCMSTheme {
                MainScreen()   // ✅ ENTRY POINT
            }
        }
    }
}
