package ch.ubique.qrscanner.mlkit.decoder

import android.graphics.Bitmap
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import ch.ubique.qrscanner.scanner.ImageDecoder
import ch.ubique.qrscanner.state.DecodingState
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MLKitImageDecoder : ImageDecoder {

	private val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
	private val scanner = BarcodeScanning.getClient(options)

	@ExperimentalGetImage
	override suspend fun decodeFrame(image: ImageProxy): DecodingState = withContext(Dispatchers.IO) {
		return@withContext image.image?.let {
			val inputImage = InputImage.fromMediaImage(it, image.imageInfo.rotationDegrees)
			decodeInputImage(inputImage)
		} ?: DecodingState.NotFound
	}

	override suspend fun decodeBitmap(bitmap: Bitmap): DecodingState = withContext(Dispatchers.IO) {
		val inputImage = InputImage.fromBitmap(bitmap, 0)
		return@withContext decodeInputImage(inputImage)
	}

	private fun decodeInputImage(inputImage: InputImage): DecodingState {
		val task = scanner.process(inputImage)
		val barcodes = Tasks.await(task)

		return barcodes.firstOrNull()?.displayValue?.let { content ->
			DecodingState.Decoded(content)
		} ?: DecodingState.NotFound
	}
}