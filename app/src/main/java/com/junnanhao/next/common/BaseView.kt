package com.junnanhao.next.common


/**
 * Created by Jonas on 2017/5/28.
 * base mView
 */
interface BaseView<T : BasePresenter> {
    fun setPresenter(presenter: T)
}