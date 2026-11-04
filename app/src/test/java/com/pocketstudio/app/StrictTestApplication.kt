package com.pocketstudio.app

import org.robolectric.TestLifecycleApplication
import java.lang.reflect.Method

/**
 * The app as every Robolectric test runs it (`robolectric.properties`), with StrictMode's rule on: no
 * disk nor network on the main thread ([MainThreadIo]); a new place that does it fails its test.
 * `-Pstrict=on` turns the watch on (the audit's rounds); off otherwise.
 */
class StrictTestApplication : PocketStudioApplication(), TestLifecycleApplication {
    override fun onCreate() {
        // Before the app's own start (its settings are read there).
        MainThreadIo.start()
        super.onCreate()
    }

    override fun beforeTest(method: Method) = MainThreadIo.start()

    override fun prepareTest(test: Any) = Unit

    override fun afterTest(method: Method) = MainThreadIo.stopAndCheck("${method.declaringClass.simpleName}.${method.name}")
}
