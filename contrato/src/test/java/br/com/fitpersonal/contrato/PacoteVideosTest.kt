package br.com.fitpersonal.contrato

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PacoteVideosTest {

    private fun pasta(): File = Files.createTempDirectory("pacote").toFile().also { it.deleteOnExit() }

    @Test fun escreveELeDeVolta() {
        val dir = pasta()
        val v1 = File(dir, "a.mp4").apply { writeBytes(ByteArray(2000) { it.toByte() }) }
        val v2 = File(dir, "b.mp4").apply { writeBytes(ByteArray(500) { 7 }) }
        val zip = File(dir, "x" + PacoteVideos.EXTENSAO)
        PacoteVideos.escrever(
            zip, "{\"treino\":1}",
            listOf(
                PacoteVideos.VideoParaPacote(listOf("Supino reto", "Supino"), v1),
                PacoteVideos.VideoParaPacote(listOf("Remada"), v2),
            ),
        )

        val saida = File(dir, "out")
        val r = PacoteVideos.ler(zip.inputStream(), saida) as Leitura.Ok
        assertEquals("{\"treino\":1}", r.valor.treinoJson)
        assertEquals(2, r.valor.videos.size)
        assertEquals(listOf("Supino reto", "Supino"), r.valor.videos[0].exercicios)
        assertEquals(2000L, r.valor.videos[0].arquivo.length())
        assertEquals(500L, r.valor.videos[1].arquivo.length())
    }

    @Test fun pacoteSemVideosAindaTemOTreino() {
        val dir = pasta()
        val zip = File(dir, "x.zip")
        PacoteVideos.escrever(zip, "{}", emptyList())
        val r = PacoteVideos.ler(zip.inputStream(), File(dir, "o")) as Leitura.Ok
        assertTrue(r.valor.videos.isEmpty())
    }

    @Test fun ignoraCaminhosPerigososDoZip() {
        val bytes = ByteArrayOutputStream().also { b ->
            ZipOutputStream(b).use { z ->
                z.putNextEntry(ZipEntry(PacoteVideos.ENTRADA_TREINO)); z.write("{}".toByteArray()); z.closeEntry()
                z.putNextEntry(ZipEntry("../fora.mp4")); z.write(ByteArray(10)); z.closeEntry()
                z.putNextEntry(ZipEntry("videos/../fora2.mp4")); z.write(ByteArray(10)); z.closeEntry()
                z.putNextEntry(ZipEntry("videos/qualquer.exe")); z.write(ByteArray(10)); z.closeEntry()
            }
        }.toByteArray()
        val dir = pasta()
        val saida = File(dir, "o")
        val r = PacoteVideos.ler(ByteArrayInputStream(bytes), saida) as Leitura.Ok
        assertTrue(r.valor.videos.isEmpty())
        assertFalse(File(dir, "fora.mp4").exists())
        assertFalse(File(dir, "fora2.mp4").exists())
        assertEquals(0, saida.listFiles().orEmpty().size)
    }

    @Test fun semTreinoEhErro() {
        val bytes = ByteArrayOutputStream().also { b ->
            ZipOutputStream(b).use { z ->
                z.putNextEntry(ZipEntry("videos/v1.mp4")); z.write(ByteArray(10)); z.closeEntry()
            }
        }.toByteArray()
        assertTrue(PacoteVideos.ler(ByteArrayInputStream(bytes), pasta()) is Leitura.Erro)
    }
}
