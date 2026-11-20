package com.example.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class BeSissyDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "Be Sissy Device Protection Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        val active = RestrictionRuntimeState.activeSession.value?.status == "ACTIVE"
        return if (active) {
            "A Be Sissy restriction session is currently active. End or finish your session before disabling protection."
        } else {
            "Disable Be Sissy device admin protection?"
        }
    }
}
