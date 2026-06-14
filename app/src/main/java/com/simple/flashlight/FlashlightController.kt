package com.simple.flashlight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlashlightController(private val context: Context) {

    private val cameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraId: String? = null
    private var hasFlash = false

    private val _isOn = TorchStateStore.torchOn
    val isOn: StateFlow<Boolean> = _isOn

    private val _hasFlashHardware = MutableStateFlow(false)
    val hasFlashHardware: StateFlow<Boolean> = _hasFlashHardware.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        TorchStateStore.initialize(context)
        refreshHardware()
    }

    fun refreshHardware() {
        try {
            cameraId = null
            hasFlash = false
            _hasFlashHardware.value = false
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val flashAvailable =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (flashAvailable && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    hasFlash = true
                    _hasFlashHardware.value = true
                    _errorMessage.value = null
                    break
                }
            }
            if (!hasFlash) {
                _errorMessage.value = "Keine LED-Taschenlampe gefunden"
            }
        } catch (e: SecurityException) {
            _errorMessage.value = "Kamera-Berechtigung fehlt"
        } catch (e: Exception) {
            _errorMessage.value = "Kamera-Zugriff fehlgeschlagen: ${e.message}"
        }
    }

    fun toggle(): Boolean {
        return if (_isOn.value) {
            turnOff()
        } else {
            turnOn()
        }
    }

    fun turnOn(): Boolean {
        if (!hasFlash) {
            _errorMessage.value = "Keine LED-Taschenlampe verfügbar"
            return false
        }
        return try {
            cameraId?.let { id ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(id, true)
                }
                TorchStateStore.setTorchOn(context, true)
                _errorMessage.value = null
                true
            } ?: false
        } catch (e: Exception) {
            _errorMessage.value = "LED konnte nicht eingeschaltet werden"
            false
        }
    }

    fun turnOff(): Boolean {
        return try {
            cameraId?.let { id ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(id, false)
                }
            }
            TorchStateStore.setTorchOn(context, false)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun turnOffSafe() {
        try {
            cameraId?.let { id ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(id, false)
                }
            }
        } catch (_: Exception) {
        }
        TorchStateStore.setTorchOn(context, false)
    }
}
