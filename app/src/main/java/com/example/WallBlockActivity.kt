package com.example

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.UserProfile
import com.example.service.RestrictionRuntimeState
import com.example.ui.components.WallBlockScreen
import com.example.ui.theme.BeSissyTheme

class WallBlockActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val blockedApp = intent.getStringExtra(EXTRA_APP_NAME) ?: "Restricted App"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: "Usage Limit Reached"
        val unlockHint = intent.getStringExtra(EXTRA_UNLOCK_HINT) ?: "Resets at 00:00"
        val unlockTargetMillis = intent.getLongExtra(
            EXTRA_UNLOCK_TARGET_MILLIS,
            RestrictionRuntimeState.computeNextMidnightMillis()
        )

        setContent {
            BackHandler {
                // High-priority Wall Block prevents casual Back dismissal; user must return to Be Sissy
            }
            val profile by RestrictionRuntimeState.userProfile.collectAsStateWithLifecycle()
            val customBgUri by RestrictionRuntimeState.wallBlockBgImageUri.collectAsStateWithLifecycle()
            val safeProfile = profile ?: UserProfile()

            BeSissyTheme(
                fontKey = safeProfile.wallBlockFontKey,
                customFontPath = safeProfile.customFontUri,
                fontColorHex = safeProfile.wallBlockFontColorHex
            ) {
                WallBlockScreen(
                    blockedAppName = blockedApp,
                    blockReasonTitle = reason,
                    unlockTimeHint = unlockHint,
                    profile = safeProfile,
                    unlockTargetMillis = unlockTargetMillis,
                    customBgImageUri = customBgUri,
                    onDismissBack = {
                        val homeIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(homeIntent)
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_UNLOCK_HINT = "extra_unlock_hint"
        const val EXTRA_UNLOCK_TARGET_MILLIS = "extra_unlock_target_millis"
    }
}
