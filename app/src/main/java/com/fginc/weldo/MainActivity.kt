package com.fginc.weldo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.fginc.weldo.notifications.NudgeDeepLink
import com.fginc.weldo.notifications.NudgeScheduler
import com.fginc.weldo.ui.WeldoNavHost
import com.fginc.weldo.ui.theme.WeldoTheme

class MainActivity : ComponentActivity() {
    // A tapped nudge notification launches (or re-enters) this activity carrying the
    // item's type + id; WeldoNavHost observes this and deep-links to the detail screen.
    private val pendingDeepLink = mutableStateOf<NudgeDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink.value = readDeepLink(intent)
        setContent {
            WeldoTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val link by pendingDeepLink
                    WeldoNavHost(
                        pendingDeepLink = link,
                        onDeepLinkConsumed = { pendingDeepLink.value = null },
                    )
                }
            }
        }
    }

    // singleTop → an already-running instance gets the tap here instead of a new activity.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readDeepLink(intent)?.let { pendingDeepLink.value = it }
    }

    private fun readDeepLink(intent: Intent?): NudgeDeepLink? {
        val type = intent?.getStringExtra(NudgeScheduler.EXTRA_TYPE) ?: return null
        val itemId = intent.getStringExtra(NudgeScheduler.EXTRA_ITEM_ID) ?: return null
        if (itemId.isBlank()) return null
        return NudgeDeepLink(type, itemId)
    }
}
