package id.xms.xarchiver.core.root

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RootService {

    suspend fun ensureRoot(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            Shell.getShell()
            Shell.isAppGrantedRoot() == true
        }.getOrDefault(false)
    }
    fun isGranted(): Boolean =
        runCatching { Shell.isAppGrantedRoot() == true }
            .getOrDefault(false)
}
