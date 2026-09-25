package com.vodapp.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource

object DataSourceFactory {

    @OptIn(UnstableApi::class)
    fun build(context: Context): HttpDataSource.Factory {
        return DefaultHttpDataSource.Factory()
            .setUserAgent("okhttp/3.12.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
    }
}
