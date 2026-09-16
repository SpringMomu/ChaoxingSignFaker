package org.aquamarine5.brainspark.chaoxingsignfaker.components

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.ReaderException
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** QR decoding runs entirely on the device and does not initialize a network SDK. */
object LocalQRCodeDecoder {
    fun decodePixels(width: Int, height: Int, pixels: IntArray): String? =
        decode(RGBLuminanceSource(width, height, pixels))

    fun decodeLuminance(width: Int, height: Int, pixels: ByteArray): String? =
        decode(PlanarYUVLuminanceSource(pixels, width, height, 0, 0, width, height, false))

    private fun decode(source: LuminanceSource): String? {
        for (candidate in listOf(source, source.invert())) {
            try {
                return QRCodeReader().decode(
                    BinaryBitmap(HybridBinarizer(candidate)),
                    mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")
                ).text
            } catch (_: ReaderException) {
                // A camera frame may contain no QR code or only part of one.
            }
        }
        return null
    }
}
