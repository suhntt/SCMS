package com.example.scms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.scms.ui.theme.SCMSTheme
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ RESTORE USER SESSION
        val sessionManager = SessionManager(this)
        UserSession.currentUser = sessionManager.getUser()

        // ✅ SUBSCRIBE TO ALERTS TOPIC
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")

        setContent {
            SCMSTheme {
                MainScreen()   // ✅ ENTRY POINT
            }
        }
    }
}
