package com.junnanhao.next

import android.app.Application
import com.github.ajalt.timberkt.Timber
import com.junnanhao.next.data.model.MyObjectBox
import io.objectbox.BoxStore
import timber.log.Timber.DebugTree

/**
 * Created by jonashao on 2017/9/10.
 * Custom Application class
 */

class App : Application() {
    lateinit var boxStore: BoxStore
        set
        get

    override fun onCreate() {
        super.onCreate()

        Timber.plant(DebugTree())
        boxStore = MyObjectBox.builder().androidContext(this).build()
    }


}
