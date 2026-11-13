package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.BrandRecognizer
import com.example.bluetooth.PermissionHelper
import com.example.model.CkRoutes
import com.example.model.EarbudBrand
import com.example.model.EqPreset
import com.example.model.ThemePreference
import com.example.model.UiCustomizationConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app name CK from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("CK", appName)
  }

  @Test
  fun `default theme is pure white liquid glass`() {
    val config = UiCustomizationConfig()
    assertEquals(ThemePreference.WHITE, config.themePreference)
  }

  @Test
  fun `all screen routes are unique`() {
    val routes = listOf(
      CkRoutes.HOME,
      CkRoutes.FIRMWARE_UPDATE,
      CkRoutes.BLUETOOTH_DEVICES,
      CkRoutes.BATTERY_DASHBOARD,
      CkRoutes.ANIMATION_SETTINGS,
      CkRoutes.GLASS_CUSTOMIZATION,
      CkRoutes.PERMISSIONS,
      CkRoutes.ABOUT_CK,
      CkRoutes.EQUALIZER,
      CkRoutes.CONTROLS,
    )
    assertEquals(routes.size, routes.toSet().size)
  }

  @Test
  fun `brand recognizer identifies major earbuds brands`() {
    assertEquals(EarbudBrand.VIVO, BrandRecognizer.identify("vivo TWS 4 Hi-Fi"))
    assertEquals(EarbudBrand.ONEPLUS, BrandRecognizer.identify("OnePlus Nord Buds 3 Pro"))
    assertEquals(EarbudBrand.SAMSUNG, BrandRecognizer.identify("Galaxy Buds3 Pro"))
    assertEquals(EarbudBrand.JBL, BrandRecognizer.identify("JBL Tour Pro 2"))
    assertEquals(EarbudBrand.REALME, BrandRecognizer.identify("realme Buds Air 6"))
    assertEquals(EarbudBrand.OPPO, BrandRecognizer.identify("OPPO Enco Air3 Pro"))
    assertEquals(EarbudBrand.BOAT, BrandRecognizer.identify("boAt Airdopes 141"))
  }

  @Test
  fun `eq presets provide 5 bands`() {
    EqPreset.entries.forEach { preset ->
      assertEquals(5, preset.bandsDb.size)
    }
  }

  @Test
  fun `permission checklist includes runtime rationales`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val checklist = PermissionHelper.buildChecklist(context)
    assertTrue(checklist.isNotEmpty())
    assertTrue(checklist.all { it.whyNeeded.isNotBlank() })
  }
}
