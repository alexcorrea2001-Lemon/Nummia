package io.github.alexcorrea2001lemon.nummia.data.actualizar

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.security.MessageDigest

class ActualizadorTest {
    @get:Rule val tmp = TemporaryFolder()
    private lateinit var servidor: MockWebServer
    private val cliente = OkHttpClient()

    @Before fun arrancar() { servidor = MockWebServer().apply { start() } }
    @After fun parar() { runCatching { servidor.shutdown() } }

    private fun actualizador(actual: String = "1.0.0") =
        Actualizador(cliente, actual, tmp.newFolder("updates"), api = servidor.url("/").toString())

    private fun release(tag: String) = """
        {"tag_name":"$tag","body":"Novedades","assets":[
          {"name":"nummia-$tag.apk","browser_download_url":"${servidor.url("/apk")}","size":5},
          {"name":"nummia-$tag.apk.sha256","browser_download_url":"${servidor.url("/sha")}","size":64}
        ]}
    """.trimIndent()

    private fun sha(b: ByteArray) = MessageDigest.getInstance("SHA-256").digest(b).joinToString("") { "%02x".format(it) }

    @Test fun comparaVersiones() {
        assertTrue(esMasNueva("v1.0.1", "1.0.0"))
        assertFalse(esMasNueva("v1.0.0", "1.0.0"))
        assertTrue(esMasNueva("v1.10.0", "1.9.0"))
        assertFalse(esMasNueva("v1.9.9", "1.10.0"))
        assertTrue(esMasNueva("v2.0.0", "1.99.99"))
        assertFalse(esMasNueva("v1.0.0", "1.0.0-debug"))
    }

    @Test fun detectaVersionNueva() = runTest {
        servidor.enqueue(MockResponse().setBody(release("v1.1.0")))
        val e = actualizador().comprobar()
        assertTrue(e is EstadoActualizacion.Disponible)
        assertEquals("1.1.0", (e as EstadoActualizacion.Disponible).lanzamiento.version)
    }

    @Test fun mismaVersionEstaAlDia() = runTest {
        servidor.enqueue(MockResponse().setBody(release("v1.0.0")))
        assertEquals(EstadoActualizacion.AlDia, actualizador().comprobar())
    }

    @Test fun errorDelServidorNoRompeNada() = runTest {
        servidor.enqueue(MockResponse().setResponseCode(500))
        assertTrue(actualizador().comprobar() is EstadoActualizacion.NoSePudoComprobar)
    }

    @Test fun sinConexionNoRompeNada() = runTest {
        val a = actualizador()
        servidor.shutdown()
        assertTrue(a.comprobar() is EstadoActualizacion.NoSePudoComprobar)
    }

    @Test fun descargaCorrectaPasaLaHuella() = runTest {
        val apk = "hola!".toByteArray()
        servidor.enqueue(MockResponse().setBody(release("v1.1.0")))
        servidor.enqueue(MockResponse().setBody("${sha(apk)}  nummia-v1.1.0.apk\n"))
        servidor.enqueue(MockResponse().setBody(okio.Buffer().write(apk)))
        val a = actualizador()
        val l = (a.comprobar() as EstadoActualizacion.Disponible).lanzamiento
        val f = a.descargar(l).getOrThrow()
        assertTrue(f.exists())
        assertEquals(5L, f.length())
    }

    @Test fun huellaDistintaBorraElArchivo() = runTest {
        servidor.enqueue(MockResponse().setBody(release("v1.1.0")))
        servidor.enqueue(MockResponse().setBody("0".repeat(64)))
        servidor.enqueue(MockResponse().setBody("manipulado"))
        val a = actualizador()
        val l = (a.comprobar() as EstadoActualizacion.Disponible).lanzamiento
        val r = a.descargar(l)
        assertTrue(r.isFailure)
        assertTrue(r.exceptionOrNull() is DescargaInvalida)
        assertFalse(tmp.root.resolve("updates/nummia-v1.1.0.apk").exists())
    }
}
