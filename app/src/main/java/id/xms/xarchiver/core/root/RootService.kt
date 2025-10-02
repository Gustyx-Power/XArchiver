package id.xms.xarchiver.core.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootService {
    suspend fun ensureRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            Shell.getShell()
            Shell.isAppGrantedRoot()
        } catch (_: Throwable) { false }
    }

    fun isGranted(): Boolean = try {
        Shell.isAppGrantedRoot()
    } catch (_: Throwable) { false }
}
