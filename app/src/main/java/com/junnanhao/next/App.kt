package com.junnanhao.next

import android.app.Application
import com.facebook.stetho.Stetho
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
}