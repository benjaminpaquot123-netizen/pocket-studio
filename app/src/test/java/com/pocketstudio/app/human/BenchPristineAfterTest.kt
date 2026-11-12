package com.pocketstudio.app.human

import androidx.test.core.app.ApplicationProvider
import com.pocketstudio.app.StrictTestApplication
import com.pocketstudio.model.i18n.I18n
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Review of the « CI : fuite d'état » repair (moyen 1): after an ordinary test the state is put back
 * (`StrictTestApplication.afterTest`), so a bench test that leaves something, followed by an ordinary test, went unseen
 * (its victim, the next bench test, started pristine). Now the test that carries [BenchPristineRule] answers for what it
 * leaves itself: red, naming it, and the state put back (one leak, one red). Each test here calls the application's
 * afterTest as Robolectric does once a test is over.
 */
@RunWith(RobolectricTestRunner::class)
class BenchPristineAfterTest {
    @get:Rule
    val pristine = BenchPristineRule()

    private val app: StrictTestApplication get() = ApplicationProvider.getApplicationContext()

    @Test
    fun aBenchTestThatLeavesSomethingIsRedItselfAndPutsItBack() {
        I18n.everyday = true
        val e = runCatching { app.afterTest(javaClass.getMethod("aBenchTestThatLeavesSomethingIsRedItselfAndPutsItBack")) }.exceptionOrNull()
        assertTrue("the bench test that leaves I18n.everyday is red itself: $e", e is AssertionError && "I18n.everyday" in e.message.orEmpty())
        assertTrue("it names that test: ${e?.message}", "aBenchTestThatLeavesSomethingIsRedItselfAndPutsItBack" in e?.message.orEmpty())
        assertEquals("and the state is put back", emptyList<String>(), BenchPristine.leaks())
    }

    @Test
    fun aBenchTestThatLeavesNothingIsGreen() {
        app.afterTest(javaClass.getMethod("aBenchTestThatLeavesNothingIsGreen"))
    }
}

/**
 * The same review (mineur 4): the rule was recognised by the declared type of a field; declared as a plain [TestRule], a
 * test that carries it was taken for an ordinary one, its leaks put back without a word. The rule now says it ran.
 */
@RunWith(RobolectricTestRunner::class)
class BenchPristineAfterLooseTypeTest {
    @get:Rule
    val pristine: TestRule = BenchPristineRule()

    private val app: StrictTestApplication get() = ApplicationProvider.getApplicationContext()

    @Test
    fun aRuleDeclaredAsATestRuleStillAnswersForItsLeaks() {
        I18n.everyday = true
        val e = runCatching { app.afterTest(javaClass.getMethod("aRuleDeclaredAsATestRuleStillAnswersForItsLeaks")) }.exceptionOrNull()
        assertTrue("red, though the rule is declared as a TestRule: $e", e is AssertionError && "I18n.everyday" in e.message.orEmpty())
        assertEquals(emptyList<String>(), BenchPristine.leaks())
    }
}
