package com.example.mymemo.util

import android.app.Application
import android.content.Context

// 전역 context 사용가능하도록 하는 클래스
// 사용하려면 AndroidManifest 파일 <application>에 android:name=".App" 을 추가해야됨
class App: Application() {
    // context 를 singleton 으로 생성
    companion object {
        lateinit var instance: App
            private set

        fun context() : Context {
            return instance.applicationContext
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}