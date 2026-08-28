package com.graygate.controller

import com.graygate.data.model.ManualOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GrayscaleRulesTest {

    private val self = "com.graygate"
    private val selected = setOf("com.instagram.android", "com.zhiliaoapp.musically")

    private fun decide(
        packageName: String,
        override: ManualOverride = ManualOverride.AUTO,
        isEnabled: Boolean = true,
        packages: Set<String> = selected
    ) = GrayscaleRules.desiredState(packageName, self, isEnabled, override, packages)

    // --- Per-app rules: selected apps go gray, everything else stays color ---

    @Test
    fun `grayscales selected app`() {
        assertEquals(true, decide("com.instagram.android"))
    }

    @Test
    fun `keeps color for unselected app`() {
        assertEquals(false, decide("com.android.camera"))
    }

    // --- Manual overrides take precedence over per-app rules ---

    @Test
    fun `force gray overrides per-app rules`() {
        assertEquals(true, decide("com.android.camera", override = ManualOverride.FORCE_GRAY))
    }

    @Test
    fun `force color overrides per-app rules`() {
        assertEquals(false, decide("com.instagram.android", override = ManualOverride.FORCE_COLOR))
    }

    // --- Ignored packages never change state ---

    @Test
    fun `system ui is ignored`() {
        assertNull(decide("com.android.systemui"))
    }

    @Test
    fun `keyboards are ignored`() {
        assertNull(decide("com.google.android.inputmethod.latin"))
        assertNull(decide("com.touchtype.swiftkey"))
    }

    @Test
    fun `own package is ignored`() {
        assertNull(decide(self))
    }

    @Test
    fun `blank package is ignored`() {
        assertNull(decide(""))
    }

    @Test
    fun `ignored packages ignore overrides too`() {
        assertNull(decide("com.android.systemui", override = ManualOverride.FORCE_GRAY))
    }

    // --- Master toggle ---

    @Test
    fun `disabled app never changes state`() {
        assertNull(decide("com.instagram.android", isEnabled = false))
        assertNull(decide("com.instagram.android", isEnabled = false, override = ManualOverride.FORCE_GRAY))
    }

    // --- Returning to AUTO from an override ---
    //
    // These pin down why GrayscaleController.applyOverride(AUTO) has to recompute
    // from the last NON-ignored package instead of the live foreground app.
    // Deferring to the next poll left the screen stuck gray after Force Grayscale.

    @Test
    fun `auto cannot be resolved from graygate's own package`() {
        // The regression: when the user flips the override in GrayGate's Settings
        // screen, the foreground app IS GrayGate — so the poll has nothing to act
        // on and the override change is silently dropped.
        assertNull(decide(self, override = ManualOverride.AUTO))
    }

    @Test
    fun `auto resolves from the owning app instead`() {
        // ...so AUTO must be recomputed against whichever real app last decided
        // the state. Both directions have to come out right.
        assertEquals(true, decide("com.instagram.android", override = ManualOverride.AUTO))
        assertEquals(false, decide("com.android.camera", override = ManualOverride.AUTO))
    }

    // --- isIgnored helper ---

    @Test
    fun `isIgnored matches ignore list and self`() {
        assertTrue(GrayscaleRules.isIgnored("com.android.systemui", self))
        assertTrue(GrayscaleRules.isIgnored(self, self))
        assertTrue(GrayscaleRules.isIgnored("", self))
        assertFalse(GrayscaleRules.isIgnored("com.instagram.android", self))
    }
}
