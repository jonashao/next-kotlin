package com.junnanhao.next.common

import com.facebook.stetho.Stetho
import com.uphyca.stetho_realm.RealmInspectorModulesProvider

import timber.log.Timber

//import timber.log.Timber


/**
 * Created by Jonas on 2017/5/28.
 * Custom Application
 */
class DebugApp : App() {
    override fun onCreate() {
        super.onCreate()
        initializeTimberDebug()
        initializeStethoDebug()
    }

    fun initializeTimberDebug() {
        Timber.plant(Timber.DebugTree())
    }

    fun initializeStethoDebug() {
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build())
    }

}

