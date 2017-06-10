package com.junnanhao.next.common

import android.app.Application
import com.junnanhao.next.data.DaggerSongsRepositoryComponent
import com.junnanhao.next.data.SongsRepositoryComponent
import com.junnanhao.next.common.di.ApplicationModule
import io.realm.Realm
import io.realm.RealmConfiguration

//import timber.log.Timber


/**
 * Created by Jonas on 2017/5/28.
 * Custom Application
 */
open class App : Application() {
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
//        Realm.deleteRealm(realmConfig) // Delete Realm between app restarts.
        Realm.setDefaultConfiguration(realmConfig)
    }


    lateinit var songsRepositoryComponent: SongsRepositoryComponent
        get

}

