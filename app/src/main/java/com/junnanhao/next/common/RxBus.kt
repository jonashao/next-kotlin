package com.junnanhao.next.common

/**
 * Created by Jonas on 2017/5/28.
 * RxBus
 */
class RxBus private constructor() {
    private val bus = io.reactivex.processors.PublishProcessor.create<Any>().toSerialized()

    fun post(o: Any) {
        bus.onNext(o)
    }

    fun <T> toFlowable(aClass: Class<T>): io.reactivex.Flowable<T> {
        return bus.ofType(aClass)
    }

    fun hasSubscribers(): Boolean {
        return bus.hasSubscribers()
    }

    companion object {
        @Volatile private var instance: com.junnanhao.next.common.RxBus? = null

        fun get(): com.junnanhao.next.common.RxBus {
            if (com.junnanhao.next.common.RxBus.Companion.instance == null) {
                synchronized(com.junnanhao.next.common.RxBus::class.java) {
                    if (com.junnanhao.next.common.RxBus.Companion.instance == null) {
                        com.junnanhao.next.common.RxBus.Companion.instance = com.junnanhao.next.common.RxBus()
                    }
                }
            }
            return com.junnanhao.next.common.RxBus.Companion.instance!!
        }
    }
}