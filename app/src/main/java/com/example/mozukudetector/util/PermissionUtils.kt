package com.example.mozukudetector.util

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

object PermissionUtils {

    fun isAllPermissionsGranted(context: Context): Boolean {
        for (permission in getRequiredPermissions(context)) {
            if (!isPermissionGranted(context, permission)) {
                return false
            }
        }
        return true
    }

    fun getRequiredPermissions(context: Context): List<String> =
        try {
            val info = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps.toMutableList()
            } else {
                mutableListOf()
            }
        } catch (e: Exception) {
            mutableListOf()
        }

    fun requestRequiredPermissionsIfNeeded(activity: Activity, requestCode: Int) {
        val allNeededPermissions: MutableList<String?> = ArrayList()
        for (permission in getRequiredPermissions(activity)) {
            if (!isPermissionGranted(activity, permission)) {
                allNeededPermissions.add(permission)
            }
        }
        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                allNeededPermissions.toTypedArray(),
                requestCode
            )
        }
    }

    fun isPermissionGranted(
        context: Context, permission: String?
    ): Boolean = ContextCompat.checkSelfPermission(
        context,
        permission!!
    ) == PackageManager.PERMISSION_GRANTED
}
