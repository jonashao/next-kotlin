package com.junnanhao.next.data

import com.junnanhao.next.common.di.ApplicationModule
import dagger.Component
import javax.inject.Singleton

/**
 * Created by Jonas on 2017/5/29.
 * songs repository component
 */
@Singleton
@Component(modules = arrayOf(ApplicationModule::class))
interface SongsRepositoryComponent {
    fun getSongsRepository(): SongsRepository
}