package com.example.meaningosapp.core

import android.content.Context

fun loadPTPJson(context: Context): String {
    return context.assets.open("PTP.json")
        .bufferedReader()
        .use { it.readText() }
}
