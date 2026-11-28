package com.hotaro.brik.util

import android.graphics.Bitmap
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest

object QrUtils {

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun encodeToBitmap(text: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 2
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            for (x in 0 until sizePx) {
                pixels[y * sizePx + x] =
                    if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        return bitmap
    }

    // ZXing multi-format reader with QR + Book ISBN (EAN-13, EAN-8, UPC, Code 128) support
    private val zxingReader = MultiFormatReader().apply {
        setHints(
            mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.EAN_13,  // Standard Book ISBN format
                    BarcodeFormat.EAN_8,
                    BarcodeFormat.UPC_A,
                    BarcodeFormat.UPC_E,
                    BarcodeFormat.CODE_128
                ),
                DecodeHintType.TRY_HARDER to true
            )
        )
    }

    // Google ML Kit Barcode Scanner configured for QR Code + Book ISBN (EAN-13) & 1D barcodes
    private val mlKitScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_EAN_13,  // Standard Book ISBN
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    fun decodeImageProxy(image: ImageProxy, engine: QrEngine = QrEngine.GOOGLE_ML_KIT): String? {
        return when (engine) {
            QrEngine.GOOGLE_ML_KIT -> {
                decodeWithMlKit(image) ?: decodeWithZxing(image)
            }
            QrEngine.ZXING -> {
                decodeWithZxing(image)
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun decodeWithMlKit(image: ImageProxy): String? {
        return try {
            val mediaImage = image.image ?: return null
            val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
            val barcodes = Tasks.await(mlKitScanner.process(inputImage))
            barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
        } catch (_: Throwable) {
            null
        }
    }

    fun decodeWithZxing(image: ImageProxy): String? {
        if (image.planes.isEmpty()) return null
        val yPlane = image.planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val pixelStride = yPlane.pixelStride
        val rawWidth = image.width
        val rawHeight = image.height

        val rawData = ByteArray(rawWidth * rawHeight)
        val row = ByteArray(rowStride)
        var offset = 0
        for (y in 0 until rawHeight) {
            buffer.position(y * rowStride)
            val remaining = buffer.remaining()
            if (remaining < (rawWidth - 1) * pixelStride + 1) return null
            if (pixelStride == 1) {
                buffer.get(rawData, offset, rawWidth)
            } else {
                buffer.get(row, 0, minOf(rowStride, remaining))
                var x = 0
                while (x < rawWidth) {
                    rawData[offset + x] = row[x * pixelStride]
                    x++
                }
            }
            offset += rawWidth
        }

        // Account for sensor rotation
        val rotation = image.imageInfo.rotationDegrees
        val (data, width, height) = rotateYPlane(rawData, rawWidth, rawHeight, rotation)

        val source = PlanarYUVLuminanceSource(
            data, width, height, 0, 0, width, height, false
        )
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            synchronized(zxingReader) {
                zxingReader.decodeWithState(bitmap)
            }.text
        } catch (_: Exception) {
            // Fallback for dark mode / white-on-black inverted QR codes
            try {
                val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                synchronized(zxingReader) {
                    zxingReader.decodeWithState(invertedBitmap)
                }.text
            } catch (_: Exception) {
                null
            }
        } finally {
            zxingReader.reset()
        }
    }

    private fun rotateYPlane(data: ByteArray, width: Int, height: Int, rotationDegrees: Int): Triple<ByteArray, Int, Int> {
        return when (rotationDegrees) {
            90 -> {
                val rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[x * height + (height - y - 1)] = data[x + y * width]
                    }
                }
                Triple(rotated, height, width)
            }
            180 -> {
                val rotated = ByteArray(width * height)
                val total = width * height
                for (i in 0 until total) {
                    rotated[total - 1 - i] = data[i]
                }
                Triple(rotated, width, height)
            }
            270 -> {
                val rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[(width - x - 1) * height + y] = data[x + y * width]
                    }
                }
                Triple(rotated, height, width)
            }
            else -> Triple(data, width, height)
        }
    }
}
