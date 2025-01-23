package ch.ubique.qrscanner.zxing.decoder

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import ch.ubique.qrscanner.scanner.BarcodeFormat
import ch.ubique.qrscanner.scanner.ErrorCodes
import ch.ubique.qrscanner.scanner.ImageDecoder
import ch.ubique.qrscanner.state.DecodingState
import ch.ubique.qrscanner.zxing.extensions.toUbiqueFormat
import ch.ubique.qrscanner.zxing.scanner.BarcodeScannerUtils
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class GlobalHistogramImageDecoder(
	barcodeFormats: List<BarcodeFormat>,
) : ZXingImageDecoder(barcodeFormats, binarizerFactory = { GlobalHistogramBinarizer(it) })

class HybridImageDecoder(
	barcodeFormats: List<BarcodeFormat>,
) : ZXingImageDecoder(barcodeFormats, binarizerFactory = { HybridBinarizer(it) })

abstract class ZXingImageDecoder(
	barcodeFormats: List<BarcodeFormat>,
	private val binarizerFactory: (source: LuminanceSource) -> Binarizer
) : ImageDecoder {

	private val yuvFormats = listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
	private val decodingHints = mapOf(DecodeHintType.TRY_HARDER to true)
	private val reader = BarcodeScannerUtils.createBarcodeReader(barcodeFormats, decodingHints)

	override suspend fun decodeFrame(image: ImageProxy): DecodingState = withContext(Dispatchers.IO) {
		if (image.format in yuvFormats && image.planes.size == 3) {
			val source = try {
				val buffer = image.planes[0].buffer.asReadOnlyBuffer()
				val data = buffer.toByteArray()
				PlanarYUVLuminanceSource(
					data,
					image.planes[0].rowStride,
					image.height,
					0,
					0,
					image.width,
					image.height,
					false
				)
			} catch (e: Exception) {
				return@withContext DecodingState.Error(ErrorCodes.INPUT_READ_FAILED)
			}

			return@withContext decodeLuminanceSource(source)
		} else {
			return@withContext DecodingState.Error(ErrorCodes.INPUT_WRONG_FORMAT)
		}
	}

	override suspend fun decodeBitmap(bitmap: Bitmap): DecodingState = withContext(Dispatchers.IO) {
		val intArray = IntArray(bitmap.width * bitmap.height)
		bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
		val source = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)

		return@withContext decodeLuminanceSource(source)
	}

	private fun decodeLuminanceSource(source: LuminanceSource): DecodingState {
		val binarizer = binarizerFactory.invoke(source)
		val binaryBitmap = BinaryBitmap(binarizer)
		return try {
			val result = reader.decodeWithState(binaryBitmap)
			val format = result.barcodeFormat.toUbiqueFormat()

			if (format != null) {
				DecodingState.Decoded(result.text, format)
			} else {
				DecodingState.NotFound
			}
		} catch (e: NotFoundException) {
			DecodingState.NotFound
		} catch (e: ChecksumException) {
			DecodingState.NotFound
		} catch (e: FormatException) {
			DecodingState.NotFound
		}
	}

	private fun ByteBuffer.toByteArray(): ByteArray {
		rewind()
		val data = ByteArray(remaining())
		get(data)
		rewind()
		return data
	}
}