package id.xms.xarchiver.ui.home

import androidx.lifecycle.ViewModel
import id.xms.xarchiver.core.*

class HomeViewModel : ViewModel() {
    var storage = StorageInfo(
        used = 185L * 1024 * 1024 * 1024,
        total = 256L * 1024 * 1024 * 1024
    )

    var categories = listOf(
        Category("Images", "image", 1704),
        Category("Videos", "videocam", 64),
        Category("Audio", "music_note", 320),
        Category("Documents", "description", 120),
        Category("APK", "android", 45),
        Category("Archives", "folder_zip", 33)
    )

    var shortcuts = listOf(
        Shortcut("Bluetooth", "bluetooth"),
        Shortcut("Downloads", "download"),
        Shortcut("WhatsApp", "chat"),
        Shortcut("Telegram", "send")
    )
}
