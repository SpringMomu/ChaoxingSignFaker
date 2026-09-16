package org.aquamarine5.brainspark.chaoxingsignfaker

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.aquamarine5.brainspark.chaoxingsignfaker.components.LocalQRCodeDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocalQRCodeDecoderTest {
    private val size = 360
    private val signUrl = "https://mobilelearn.chaoxing.com/widget/sign/e?id=123&enc=test-token"
    private val shareUrl = "cxsignfaker://import?phone=13800000000&pwd=a%2Bb%3D&name=%E6%B5%8B%E8%AF%95&face=id1,id2"

    private fun pixels(value: String): IntArray {
        val matrix = QRCodeWriter().encode(
            value, BarcodeFormat.QR_CODE, size, size,
            mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
        )
        return IntArray(size * size) { if (matrix[it % size, it / size]) 0xFF000000.toInt() else -1 }
    }

    @Test fun decodesSignUrlFromGalleryPixels() {
        assertEquals(signUrl, LocalQRCodeDecoder.decodePixels(size, size, pixels(signUrl)))
    }

    @Test fun preservesEncodedCredentialsInLocalImportLink() {
        assertEquals(shareUrl, LocalQRCodeDecoder.decodePixels(size, size, pixels(shareUrl)))
    }

    @Test fun decodesCameraLuminanceAtAllRightAngleRotations() {
        var frame = pixels(signUrl)
        repeat(4) {
            val luminance = ByteArray(frame.size) { (frame[it] and 0xFF).toByte() }
            assertEquals(signUrl, LocalQRCodeDecoder.decodeLuminance(size, size, luminance))
            val previous = frame
            frame = IntArray(previous.size) { index ->
                val x = index % size
                val y = index / size
                previous[(size - 1 - x) * size + y]
            }
        }
    }

    @Test fun decodesInvertedQrCode() {
        val inverted = pixels(shareUrl).map { it xor 0x00FFFFFF }.toIntArray()
        assertEquals(shareUrl, LocalQRCodeDecoder.decodePixels(size, size, inverted))
    }

    @Test fun rejectsFrameWithoutQrCode() {
        assertNull(LocalQRCodeDecoder.decodeLuminance(size, size, ByteArray(size * size)))
    }
}
