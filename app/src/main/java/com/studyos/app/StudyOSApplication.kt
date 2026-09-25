package com.studyos.app

import android.app.Application
import com.studyos.app.core.StudyOSAppContainer

class StudyOSApplication : Application() {
    lateinit var container: StudyOSAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = StudyOSAppContainer(this)
    }
}
