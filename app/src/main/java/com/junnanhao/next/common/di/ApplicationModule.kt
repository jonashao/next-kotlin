package com.junnanhao.next.common.di

import android.content.Context
import com.junnanhao.next.common.App
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by Jonas on 2017/5/28.
 * Application module
 */
@Module
class ApplicationModule(private val app: App)  {
    @Provides @Singleton
    fun provideApplication(): Context = app
}