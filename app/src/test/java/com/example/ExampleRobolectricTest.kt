package com.example

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.example.network.ServerDiagnosticEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DNS & Server Check", appName)
  }

  @Test
  fun `read arabic string from localized context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val config = Configuration(context.resources.configuration).apply {
      setLocale(Locale("ar"))
    }
    val arContext = context.createConfigurationContext(config)
    val arAppName = arContext.getString(R.string.app_name)
    val arTabTitle = arContext.getString(R.string.tab_dns_server)
    assertEquals("DNS & فاحص الخوادم", arAppName)
    assertEquals("فحص DNS والخوادم", arTabTitle)
  }

  @Test
  fun `test server diagnostic target inspection`() = runBlocking {
    val engine = ServerDiagnosticEngine()
    val result = engine.inspectTarget("invalid-domain-notfound-999.xyz")
    assertNotNull(result)
    assertEquals("invalid-domain-notfound-999.xyz", result.host)
    assertEquals(com.example.network.DiagnosticStatus.ERROR, result.dnsResult.status)
  }
}
