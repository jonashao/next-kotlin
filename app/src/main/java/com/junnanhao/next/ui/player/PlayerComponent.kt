package com.junnanhao.next.ui.player

import com.junnanhao.next.data.SongsRepositoryComponent
import com.junnanhao.next.di.FragmentScoped
import dagger.Component

/**
 * Created by Jonas on 2017/5/28.
 * This is a Dagger component. Refer to [App] for the list of Dagger components
 * used in this application.
 * <P>
 * Because this component depends on the {@link TasksRepositoryComponent}, which is a singleton, a
 * scope must be specified. All fragment components use a custom scope for this purpose.
 */
@FragmentScoped
@Component(dependencies = arrayOf(SongsRepositoryComponent::class), modules = arrayOf(PlayerPresenterModule::class))
interface PlayerComponent {
    fun inject(activity: PlayerActivity)
}