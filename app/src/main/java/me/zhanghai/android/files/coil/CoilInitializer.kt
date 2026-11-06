/*
 * Copyright (c) 2020 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.coil

import android.os.Build
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import me.zhanghai.android.files.app.application

fun initializeCoil() {
    Coil.setImageLoader(
        ImageLoader.Builder(application)
            .components {
                add(AppIconApplicationInfoKeyer())
                add(AppIconApplicationInfoFetcherFactory(application))
                add(AppIconPackageNameKeyer())
                add(AppIconPackageNameFetcherFactory(application))
                add(PathAttributesKeyer())
                add(PathAttributesFetcher.Factory(application))
                // 69 mod: "Animated thumbnails" off = GIF / animated WebP thumbnails stay still
                if (androidx.preference.PreferenceManager.getDefaultSharedPreferences(application)
                        .getBoolean("mod69_animated_thumbnails", true)
                ) {
                    add(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            ImageDecoderDecoder.Factory()
                        } else {
                            GifDecoder.Factory()
                        }
                    )
                }
                add(SvgDecoder.Factory(false))
            }
            .build()
    )
}
