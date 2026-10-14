package org.nagrikvoice.app

import android.app.Application
import android.content.Context
import org.nagrikvoice.app.data.repository.AuthRepository

class NagrikVoiceApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Initialize AuthRepository singleton with app context for SharedPreferences
        AuthRepository.init(this)
    }

    companion object {
        lateinit var instance: NagrikVoiceApp
            private set

        val appContext: Context get() = instance.applicationContext
    }
}
