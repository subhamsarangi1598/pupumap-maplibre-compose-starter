package com.subham.pupumap.network

import com.subham.pupumap.BuildConfig

object BackendConfig {
    val BASE_URL: String = BuildConfig.BACKEND_BASE_URL
    val STYLE_URL: String = "${BASE_URL}styles/style.json"
}
