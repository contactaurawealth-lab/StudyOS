package com.studyos.app

import android.app.Application
import com.studyos.app.core.StudyOSAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyOSApplication : Application() {
    lateinit var container: StudyOSAppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = StudyOSAppContainer(this)
        CoroutineScope(Dispatchers.IO).launch {
            container.preferencesDataSource.syncAppBlockerManager()
        }
    }
}
