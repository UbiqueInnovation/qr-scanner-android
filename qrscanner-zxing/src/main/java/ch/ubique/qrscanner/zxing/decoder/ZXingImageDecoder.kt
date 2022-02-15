package ch.ubique.qrscanner.zxing.decoder

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageProxy
import ch.ubique.qrscanner.scanner.ErrorCodes
import ch.ubique.qrscanner.scanner.ImageDecoder
import ch.ubique.qrscanner.state.DecodingState
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class GlobalHistogramImageDecoder : ZXingImageDecoder(binarizerFactory = { GlobalHistogramBinarizer(it) })
class HybridImageDecoder : ZXingImageDecoder(binarizerFactory = { HybridBinarizer(it) })

abstract class ZXingImageDecoder(
	private val binarizerFactory: (source: LuminanceSource) -> Binarizer
) : ImageDecoder {

	private val yuvFormats = listOf(ImageFormat.YUV_420_888, ImageFormat.YUV_422_888, ImageFormat.YUV_444_888)
	private val reader = MultiFormatReader().apply {
		val map = mapOf(
			DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE),
			DecodeHintType.TRY_HARDER to true,
		)
		setHints(map)
	}

	override suspend fun decodeFrame(image: ImageProxy): DecodingState = withContext(Dispatchers.IO) {
		if (image.format in yuvFormats && image.planes.size == 3) {
			val buffer = image.planes[0].buffer
			val data = buffer.toByteArray()
			val source = PlanarYUVLuminanceSource(
				data,
				image.planes[0].rowStride,
				image.height,
				0,
				0,
				image.width,
				image.height,
				false
			)

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
			DecodingState.Decoded(result.text)
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
		return data
	}
}