package com.junnanhao.next.common

import android.app.Application
import com.facebook.stetho.Stetho
import com.junnanhao.next.data.DaggerSongsRepositoryComponent
import com.junnanhao.next.data.SongsRepositoryComponent
import com.junnanhao.next.di.ApplicationModule
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import io.realm.Realm
import io.realm.RealmConfiguration


/**
 * Created by Jonas on 2017/5/28.
 * Custom Application
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeRealm()
        initializeDagger()
    }

    private fun initializeDagger() {
        songsRepositoryComponent = DaggerSongsRepositoryComponent
                .builder()
                .applicationModule(ApplicationModule(this))
                .build()
    }

    fun initializeRealm() {
        Realm.init(this)
        val realmConfig = RealmConfiguration.Builder().build()
        Realm.deleteRealm(realmConfig) // Delete Realm between app restarts.
        Realm.setDefaultConfiguration(realmConfig)

        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(this).build())
                        .build())
    }


    lateinit var songsRepositoryComponent: SongsRepositoryComponent
        get

}

