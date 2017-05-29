package com.junnanhao.next.di

import dagger.Component

/**
 * Created by Jonas on 2017/5/28.
 * Application component
 */

@Component(modules = arrayOf(
        ApplicationModule::class
))
interface ApplicationComponent {

}
