package com.junnanhao.next.ui.player

import dagger.Module
import dagger.Provides

/**
 * Created by Jonas on 2017/5/28.
 * This is a Dagger module. We use this to pass in the View dependency to the
 * [PlayerPresenter].
 */
@Module
class PlayerPresenterModule(val mView: PlayerContract.View) {

    @Provides
    fun providePlayerContractView(): PlayerContract.View {
        return mView
    }
}