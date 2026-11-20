package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.model.UserProfile
import com.example.ui.components.WallBlockScreen
import com.example.ui.theme.BeSissyTheme

class WallBlockOverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun canShowOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun isShowing(): Boolean = overlayView != null

    fun showOverlay(
        appName: String,
        packageName: String,
        reasonTitle: String,
        unlockHint: String,
        unlockTargetMillis: Long,
        onTempUnblockPlaps: (() -> Unit)? = null
    ): Boolean {
        if (!canShowOverlay()) return false
        dismissOverlay()

        val owner = OverlayLifecycleOwner()
        owner.onCreate()
        owner.onStart()
        owner.onResume()
        lifecycleOwner = owner

        val profile = RestrictionRuntimeState.userProfile.value ?: UserProfile()
        val customBgUri = RestrictionRuntimeState.wallBlockBgImageUri.value

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent {
                BeSissyTheme(
                    fontKey = profile.wallBlockFontKey,
                    customFontPath = profile.customFontUri,
                    fontColorHex = profile.wallBlockFontColorHex
                ) {
                    WallBlockScreen(
                        blockedAppName = appName,
                        blockReasonTitle = reasonTitle,
                        unlockTimeHint = unlockHint,
                        profile = profile,
                        unlockTargetMillis = unlockTargetMillis,
                        customBgImageUri = customBgUri,
                        onTempUnblockWithPlaps = onTempUnblockPlaps?.let { cb ->
                            {
                                cb()
                                dismissOverlay()
                            }
                        },
                        onDismissBack = {
                            dismissOverlay()
                            val homeIntent = Intent(context, MainActivity::class.java).apply {
                                addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                                )
                            }
                            context.startActivity(homeIntent)
                        }
                    )
                }
            }
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        return try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            true
        } catch (_: Exception) {
            owner.onDestroy()
            lifecycleOwner = null
            false
        }
    }

    fun dismissOverlay() {
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager.removeView(view)
        } catch (_: Exception) {
        }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }

    private class OverlayLifecycleOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val lifecycleRegistry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

        fun onCreate() {
            savedStateController.performAttach()
            savedStateController.performRestore(Bundle())
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun onStart() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        fun onResume() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun onDestroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }
}
