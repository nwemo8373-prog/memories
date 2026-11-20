package com.example.gestureactions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.view.KeyEvent
import com.example.gesturerecognition.GestureActionType

data class LaunchableAppInfo(
    val packageName: String,
    val label: String
)

data class ActionExecutionResult(
    val succeeded: Boolean,
    val detailAr: String,
    val detailEn: String
)

class DeviceActionExecutor(private val context: Context) {

    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @Volatile
    private var torchCurrentlyOn = false

    fun queryLaunchableApps(): List<LaunchableAppInfo> {
        val pm = appContext.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolveInfos
            .asSequence()
            .filter { it.activityInfo.packageName != appContext.packageName }
            .map {
                LaunchableAppInfo(
                    packageName = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString()
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    fun execute(
        action: GestureActionType,
        targetAppPackage: String? = null,
        activeCameraTorchSetter: ((Boolean) -> Boolean)? = null,
        onInAppNavigate: ((String) -> Unit)? = null,
        onPauseRequested: (() -> Unit)? = null,
        onStageControl: ((String) -> Unit)? = null
    ): ActionExecutionResult {
        return when (action) {
            GestureActionType.FLASH_ON -> setTorch(true, activeCameraTorchSetter)
            GestureActionType.FLASH_OFF -> setTorch(false, activeCameraTorchSetter)
            GestureActionType.TOGGLE_FLASH -> setTorch(!torchCurrentlyOn, activeCameraTorchSetter)

            GestureActionType.VOLUME_UP -> adjustVolume(AudioManager.ADJUST_RAISE)
            GestureActionType.VOLUME_DOWN -> adjustVolume(AudioManager.ADJUST_LOWER)

            GestureActionType.MEDIA_PLAY_PAUSE -> {
                onStageControl?.invoke("PLAY_PAUSE")
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "تشغيل/إيقاف الوسائط", "Toggled media playback")
            }
            GestureActionType.MEDIA_NEXT -> {
                onStageControl?.invoke("NEXT")
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "الانتقال للعنصر التالي", "Moved to next media item")
            }
            GestureActionType.MEDIA_PREV -> {
                onStageControl?.invoke("PREV")
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "الرجوع للعنصر السابق", "Moved to previous media item")
            }

            GestureActionType.OPEN_APP -> launchExternalApp(targetAppPackage)

            GestureActionType.PAUSE_RECOGNITION -> {
                onPauseRequested?.invoke()
                ActionExecutionResult(true, "تم إيقاف التعرّف مؤقتاً", "Recognition paused")
            }

            GestureActionType.OPEN_MEDIA_STAGE -> {
                onInAppNavigate?.invoke("media_stage")
                ActionExecutionResult(true, "تم فتح منصة العرض اللالمسية", "Opened Touchless Media Stage")
            }

            GestureActionType.OPEN_HISTORY -> {
                onInAppNavigate?.invoke("history")
                ActionExecutionResult(true, "تم فتح سجل النشاطات", "Opened Activity History")
            }

            GestureActionType.OPEN_SETTINGS -> {
                onInAppNavigate?.invoke("settings")
                ActionExecutionResult(true, "تم فتح الإعدادات", "Opened Settings")
            }

            GestureActionType.GLOBAL_BACK -> {
                val svc = NegmAccessibilityService.currentInstance()
                if (svc != null && svc.executeGlobalBack()) {
                    ActionExecutionResult(true, "تم تنفيذ أمر الرجوع العالمي", "Executed Global Back")
                } else {
                    ActionExecutionResult(
                        false,
                        "فعّل خدمة إمكانية الوصول من الإعدادات لاستخدام الرجوع العالمي",
                        "Enable Accessibility Service in Settings for Global Back"
                    )
                }
            }

            GestureActionType.GLOBAL_HOME -> {
                val svc = NegmAccessibilityService.currentInstance()
                if (svc != null && svc.executeGlobalHome()) {
                    ActionExecutionResult(true, "تم الانتقال للشاشة الرئيسية", "Executed Global Home")
                } else {
                    ActionExecutionResult(
                        false,
                        "فعّل خدمة إمكانية الوصول من الإعدادات لاستخدام الشاشة الرئيسية",
                        "Enable Accessibility Service in Settings for Global Home"
                    )
                }
            }

            GestureActionType.NONE -> ActionExecutionResult(false, "لم يتم ربط أمر بهذه الإشارة", "No action assigned")
        }
    }

    private fun setTorch(
        enable: Boolean,
        activeCameraTorchSetter: ((Boolean) -> Boolean)?
    ): ActionExecutionResult {
        // First try active CameraX rear camera control if currently bound
        if (activeCameraTorchSetter != null && activeCameraTorchSetter(enable)) {
            torchCurrentlyOn = enable
            return ActionExecutionResult(
                true,
                if (enable) "تم تشغيل الفلاش" else "تم إيقاف الفلاش",
                if (enable) "Flashlight turned ON" else "Flashlight turned OFF"
            )
        }

        return try {
            val rearFlashId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (rearFlashId == null) {
                ActionExecutionResult(false, "الجهاز لا يحتوي على فلاش متاح", "No camera flash hardware available")
            } else {
                cameraManager.setTorchMode(rearFlashId, enable)
                torchCurrentlyOn = enable
                ActionExecutionResult(
                    true,
                    if (enable) "تم تشغيل الفلاش" else "تم إيقاف الفلاش",
                    if (enable) "Flashlight turned ON" else "Flashlight turned OFF"
                )
            }
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "تعذر التحكم في الفلاش: ${e.localizedMessage ?: "الكاميرا قيد الاستخدام"}",
                "Torch unavailable: ${e.localizedMessage ?: "camera busy"}"
            )
        }
    }

    private fun adjustVolume(direction: Int): ActionExecutionResult {
        return try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            ActionExecutionResult(
                true,
                if (direction == AudioManager.ADJUST_RAISE) "تم رفع الصوت ($current/$max)" else "تم خفض الصوت ($current/$max)",
                if (direction == AudioManager.ADJUST_RAISE) "Volume raised ($current/$max)" else "Volume lowered ($current/$max)"
            )
        } catch (e: Exception) {
            ActionExecutionResult(
                false,
                "تعذر تعديل الصوت (تحقق من وضع عدم الإزعاج)",
                "Volume adjustment failed: ${e.localizedMessage}"
            )
        }
    }

    private fun dispatchMediaKey(keyCode: Int, arMsg: String, enMsg: String): ActionExecutionResult {
        return try {
            val now = SystemClock.uptimeMillis()
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
            audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
            ActionExecutionResult(true, arMsg, enMsg)
        } catch (e: Exception) {
            ActionExecutionResult(false, "تعذر إرسال أمر الوسائط", "Media command failed")
        }
    }

    private fun launchExternalApp(packageName: String?): ActionExecutionResult {
        val pkg = packageName?.takeIf { it.isNotBlank() }
            ?: return ActionExecutionResult(
                false,
                "يرجى اختيار تطبيق من شاشة تخصيص الحركات أولاً",
                "Select a target app in My Gestures first"
            )
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(pkg)
            ?: return ActionExecutionResult(
                false,
                "تعذر العثور على التطبيق: $pkg",
                "App not launchable: $pkg"
            )
        return try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(launchIntent)
            ActionExecutionResult(true, "تم فتح التطبيق ($pkg)", "Launched app ($pkg)")
        } catch (e: Exception) {
            ActionExecutionResult(false, "تعذر فتح التطبيق: ${e.localizedMessage}", "Launch failed")
        }
    }
}
