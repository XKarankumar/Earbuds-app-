package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.PermissionHelper
import com.example.model.EqPreset
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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Karan Buds Studio", appName)
  }

  @Test
  fun `eq presets provide 5 bands`() {
    EqPreset.entries.forEach { preset ->
      assertEquals(5, preset.bandsDb.size)
    }
  }

  @Test
  fun `permission checklist is populated`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val checklist = PermissionHelper.buildChecklist(context)
    assertTrue(checklist.isNotEmpty())
  }
}
