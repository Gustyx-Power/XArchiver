package id.xms.xarchiver.core.root

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object ShizukuService {

    private val _isAvailable = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isAvailableFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isAvailable

    private val _isGranted = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isGrantedFlow: kotlinx.coroutines.flow.StateFlow<Boolean> = _isGranted

    init {
        val binderReceivedListener = Shizuku.OnBinderReceivedListener {
            _isAvailable.value = true
            _isGranted.value = isGranted()
        }
        val binderDeadListener = Shizuku.OnBinderDeadListener {
            _isAvailable.value = false
            _isGranted.value = false
        }
        val permissionResultListener = Shizuku.OnRequestPermissionResultListener { _, _ ->
            _isGranted.value = isGranted()
        }

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        
        // Initial state
        _isAvailable.value = isAvailable()
        _isGranted.value = isGranted()
    }

    fun isAvailable(): Boolean {
        return Shizuku.pingBinder()
    }

    fun isGranted(): Boolean {
        if (!isAvailable()) return false
        return if (Shizuku.isPreV11()) {
            false
        } else {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermission(requestCode: Int) {
        if (isAvailable() && !Shizuku.isPreV11() && !isGranted()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    suspend fun ensureShizuku(): Boolean = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        if (isGranted()) {
            cont.resume(true) { }
            return@suspendCancellableCoroutine
        }
        
        if (!isAvailable() || Shizuku.isPreV11()) {
            cont.resume(false) { }
            return@suspendCancellableCoroutine
        }
        
        val requestCode = 9999
        val listener = object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(rc: Int, grantResult: Int) {
                if (rc == requestCode) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    cont.resume(grantResult == PackageManager.PERMISSION_GRANTED) { }
                }
            }
        }
        
        Shizuku.addRequestPermissionResultListener(listener)
        Shizuku.requestPermission(requestCode)
        
        cont.invokeOnCancellation {
            Shizuku.removeRequestPermissionResultListener(listener)
        }
    }
}
