package com.pocketstudio.app

import com.pocketstudio.app.human.BenchPristine
import com.pocketstudio.app.human.BenchPristineRule
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

    override fun prepareTest(test: Any) {
        watched = BenchPristineRule.carriedBy(test.javaClass)
    }

    override fun afterTest(method: Method) {
        try {
            MainThreadIo.stopAndCheck("${method.declaringClass.simpleName}.${method.name}")
        } finally {
            // The app's objects are statics of Robolectric's sandbox, shared by the tests of one Gradle fork. A test that
            // carries the bench's leak detector answers for what it leaves (the detector makes the next one red). Any other
            // test (the ViewModel factory that keeps its Application, a save still closing, settings changed, ...) has its
            // state put back here, by the detector's own list: a bench test run after it starts on a pristine process.
            if (!(watched ?: BenchPristineRule.carriedBy(method.declaringClass))) BenchPristine.afterAnOrdinaryTest()
        }
    }

    /** Whether the test being run carries [BenchPristineRule] (null: not told, its method's class is asked). */
    private var watched: Boolean? = null
}
