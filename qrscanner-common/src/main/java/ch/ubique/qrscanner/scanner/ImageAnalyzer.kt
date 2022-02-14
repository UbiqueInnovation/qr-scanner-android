package ch.ubique.qrscanner.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ch.ubique.qrscanner.state.DecodingState
import kotlinx.coroutines.*

internal class ImageAnalyzer(
	private val coroutineScope: CoroutineScope,
) : ImageAnalysis.Analyzer {

	private var imageDecoders: List<ImageDecoder> = emptyList()
	private var scannerCallback: QrScannerCallback? = null

	private var scanningMode = ScanningMode.PARALLEL
	private var sequentialIterator = object : Iterator<ImageDecoder> {
		private var index = 0

		override fun hasNext() = imageDecoders.isNotEmpty()

		override fun next(): ImageDecoder {
			val decoder = imageDecoders[index]

			index++
			if (index >= imageDecoders.size) {
				index = 0
			}

			return decoder
		}
	}

	override fun analyze(image: ImageProxy) {
		if (imageDecoders.isEmpty()) return

		when (scanningMode) {
			ScanningMode.PARALLEL -> {
				coroutineScope.launch {
					image.use { decodeFrameInParallel(it) }
				}
			}
			ScanningMode.SEQUENTIAL -> {
				image.use { decodeFrameSequentially(it) }
			}
			ScanningMode.ALTERNATING -> {
				image.use { decodeFrameAlternating(it) }
			}
		}
	}

	fun setImageDecoders(imageDecoders: List<ImageDecoder>) {
		this.imageDecoders = imageDecoders
	}

	fun setScannerCallback(scannerCallback: QrScannerCallback?) {
		this.scannerCallback = scannerCallback
	}

	fun setScanningMode(scanningMode: ScanningMode) {
		this.scanningMode = scanningMode
	}

	private suspend fun decodeFrameInParallel(image: ImageProxy) = withContext(Dispatchers.IO) {
		val deferredDecodingStates = imageDecoders.map {
			coroutineScope.async { it.decodeFrame(image) }
		}

		val decodingStates = deferredDecodingStates.awaitAll()

		if (coroutineScope.isActive) {
			val combinedDecodingState = decodingStates.firstOrNull { it is DecodingState.Decoded }
				?: decodingStates.firstOrNull { it is DecodingState.Error }
				?: DecodingState.NotFound

			coroutineScope.launch(Dispatchers.Main) {
				scannerCallback?.onFrameProcessed(combinedDecodingState)
			}
		}
	}

	private fun decodeFrameSequentially(image: ImageProxy) {
		for (decoder in imageDecoders) {
			val decodingState = decoder.decodeFrame(image)
			if (decodingState is DecodingState.Decoded || decodingState is DecodingState.Error) {
				scannerCallback?.onFrameProcessed(decodingState)
				return
			}
		}

		scannerCallback?.onFrameProcessed(DecodingState.NotFound)
	}

	private fun decodeFrameAlternating(image: ImageProxy) {
		val decoder = sequentialIterator.next()
		val decodingState = decoder.decodeFrame(image)
		scannerCallback?.onFrameProcessed(decodingState)
	}

}