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

    override fun prepareTest(test: Any) = Unit

    override fun afterTest(method: Method) {
        val test = "${method.declaringClass.simpleName}.${method.name}"
        try {
            MainThreadIo.stopAndCheck(test)
        } finally {
            // The app's objects are statics of Robolectric's sandbox, shared by the tests of one Gradle fork. A test that
            // carries the bench's leak detector answers for what it leaves: red itself, the state put back (review of the
            // « CI : fuite d'état » repair, moyen 1: an ordinary test run in between put it back without a word). Any other
            // test (the ViewModel factory that keeps its Application, a save still closing, settings changed, ...) has its
            // state put back here, by the detector's own list: a bench test run after it starts on a pristine process.
            // The rule says itself that it ran (mineur 4: a field's declared type could hide it).
            if (BenchPristineRule.ranSinceLastAsked()) BenchPristine.afterATestWithTheRule(test) else BenchPristine.afterAnOrdinaryTest(test)
        }
    }
}
