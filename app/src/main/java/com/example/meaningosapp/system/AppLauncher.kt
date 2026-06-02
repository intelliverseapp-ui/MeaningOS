package com.example.meaningosapp.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object AppLauncher {

    private val knownApps = mapOf(
        // High-frequency apps
        "youtube" to "com.google.android.youtube",
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "camera" to "com.sec.android.app.camera",
        "gmail" to "com.google.android.gm",
        "email" to "com.google.android.gm",
        "maps" to "com.google.android.apps.maps",
        "google maps" to "com.google.android.apps.maps",

        // Social
        "facebook" to "com.facebook.katana",
        "fb" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "insta" to "com.instagram.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "snapchat" to "com.snapchat.android",
        "snap" to "com.snapchat.android",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "whatsapp" to "com.whatsapp",
        "whats app" to "com.whatsapp",
        "messenger" to "com.facebook.orca",
        "facebook messenger" to "com.facebook.orca",
        "reddit" to "com.reddit.frontpage",
        "pinterest" to "com.pinterest",
        "discord" to "com.discord",

        // Media
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "hulu" to "com.hulu.plus",

        // Shopping / services
        "amazon" to "com.amazon.mShop.android.shopping",
        "uber" to "com.ubercab",
        "lyft" to "me.lyft.android",
        "doordash" to "com.dd.doordash",
        "door dash" to "com.dd.doordash",
        "uber eats" to "com.ubercab.eats",
        "ubereats" to "com.ubercab.eats",

        // Finance
        "venmo" to "com.venmo",
        "cash app" to "com.squareup.cash",
        "cashapp" to "com.squareup.cash",
        "paypal" to "com.paypal.android.p2pmobile",

        // HelloFresh
        "hellofresh" to "com.hellofresh.android",
        "hello fresh" to "com.hellofresh.android",
        "hello-fresh" to "com.hellofresh.android"
    )

    fun openAppByName(context: Context, rawName: String): String {
        val appName = rawName.trim().lowercase()
        val pm = context.packageManager

        // --- Deep-link priority (fastest path) ---

        if (appName.contains("youtube"))
            if (tryDeepLink(context, "vnd.youtube://home")) return "Okay — opening $rawName."

        if (appName.contains("facebook") || appName == "fb")
            if (tryDeepLink(context, "fb://feed")) return "Okay — opening $rawName."

        if (appName.contains("instagram") || appName == "insta")
            if (tryDeepLink(context, "instagram://mainfeed")) return "Okay — opening $rawName."

        if (appName.contains("tiktok"))
            if (tryDeepLink(context, "snssdk1128://feed")) return "Okay — opening $rawName."

        if (appName.contains("snapchat") || appName == "snap")
            if (tryDeepLink(context, "snapchat://open")) return "Okay — opening $rawName."

        if (appName == "twitter" || appName == "x" || appName.contains("twitter"))
            if (tryDeepLink(context, "twitter://timeline")) return "Okay — opening $rawName."

        if (appName.contains("whatsapp"))
            if (tryDeepLink(context, "whatsapp://send")) return "Okay — opening $rawName."

        if (appName.contains("messenger"))
            if (tryDeepLink(context, "fb-messenger://")) return "Okay — opening $rawName."

        if (appName.contains("reddit"))
            if (tryDeepLink(context, "reddit://home")) return "Okay — opening $rawName."

        if (appName.contains("pinterest"))
            if (tryDeepLink(context, "pinterest://home")) return "Okay — opening $rawName."

        if (appName.contains("discord"))
            if (tryDeepLink(context, "discord://discord")) return "Okay — opening $rawName."

        if (appName.contains("spotify"))
            if (tryDeepLink(context, "spotify://home")) return "Okay — opening $rawName."

        if (appName.contains("netflix"))
            if (tryDeepLink(context, "nflx://www.netflix.com/browse")) return "Okay — opening $rawName."

        if (appName.contains("hulu"))
            if (tryDeepLink(context, "hulu://home")) return "Okay — opening $rawName."

        if (appName.contains("amazon"))
            if (tryDeepLink(context, "amazon://home")) return "Okay — opening $rawName."

        if (appName.contains("uber") && !appName.contains("eats"))
            if (tryDeepLink(context, "uber://")) return "Okay — opening $rawName."

        if (appName.contains("lyft"))
            if (tryDeepLink(context, "lyft://")) return "Okay — opening $rawName."

        if (appName.contains("doordash"))
            if (tryDeepLink(context, "doordash://")) return "Okay — opening $rawName."

        if (appName.contains("uber eats") || appName.contains("ubereats"))
            if (tryDeepLink(context, "ubereats://")) return "Okay — opening $rawName."

        if (appName.contains("venmo"))
            if (tryDeepLink(context, "venmo://paycharge")) return "Okay — opening $rawName."

        if (appName.contains("cash app") || appName.contains("cashapp"))
            if (tryDeepLink(context, "cashapp://")) return "Okay — opening $rawName."

        if (appName.contains("paypal"))
            if (tryDeepLink(context, "paypal://")) return "Okay — opening $rawName."

        // --- Package-based fallback ---

        val mappedPackage = knownApps[appName]
        val packageName = mappedPackage ?: findPackageByLabel(pm, appName)

        if (packageName == null) {
            openPlayStoreSearch(context, appName)
            return "Okay — opening $rawName."
        }

        val launchIntent = pm.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            return "Okay — opening $rawName."
        }

        // --- HelloFresh special case ---
        if (packageName == "com.hellofresh.android") {
            try {
                val intent = Intent().apply {
                    setClassName(
                        "com.hellofresh.android",
                        "com.hellofresh.android.ui.MainActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return "Okay — opening $rawName."
            } catch (_: Exception) {
                openWebFallback(context, "https://www.hellofresh.com")
                return "Okay — opening $rawName."
            }
        }

        // --- Play Store fallback ---
        openPlayStoreDetails(context, packageName)
        return "Okay — opening $rawName."
    }

    private fun tryDeepLink(context: Context, uri: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun findPackageByLabel(pm: PackageManager, appName: String): String? {
        val apps = pm.getInstalledApplications(0)
        return apps.firstOrNull { appInfo ->
            appInfo.loadLabel(pm).toString().equals(appName, ignoreCase = true)
        }?.packageName
    }

    private fun openPlayStoreSearch(context: Context, appName: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://search?q=$appName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openPlayStoreDetails(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openWebFallback(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
