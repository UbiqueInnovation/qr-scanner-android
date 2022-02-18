package ch.ubique.qrscanner.scanner

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import ch.ubique.qrscanner.state.DecodingState
import kotlinx.coroutines.*

/**
 * TODO Unit/Instrumentation tests?
 * https://github.com/xiandanin/CameraX/blob/master/camera-camera2/src/androidTest/java/androidx/camera/camera2/ImageAnalysisTest.java
 * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/camera/camera-core/src/test/java/androidx/camera/core/ImageAnalysisTest.java
 * https://android.googlesource.com/platform/frameworks/support/+/refs/heads/androidx-main/camera/camera-core/src/test/java/androidx/camera/core/ImageAnalysisNonBlockingAnalyzerTest.java
 */
class ImageAnalyzer(
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

		coroutineScope.launch {
			image.use {
				when (scanningMode) {
					ScanningMode.PARALLEL -> decodeFrameInParallel(it)
					ScanningMode.SEQUENTIAL -> decodeFrameSequentially(it)
					ScanningMode.ALTERNATING -> decodeFrameAlternating(it)
				}
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

	private suspend fun decodeFrameInParallel(image: ImageProxy) {
		val deferredDecodingStates = imageDecoders.map {
			coroutineScope.async(Dispatchers.IO) { it.decodeFrame(image) }
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

	private suspend fun decodeFrameSequentially(image: ImageProxy) {
		for (decoder in imageDecoders) {
			val decodingState = decoder.decodeFrame(image)
			if (decodingState is DecodingState.Decoded || decodingState is DecodingState.Error) {
				scannerCallback?.onFrameProcessed(decodingState)
				return
			}
		}

		scannerCallback?.onFrameProcessed(DecodingState.NotFound)
	}

	private suspend fun decodeFrameAlternating(image: ImageProxy) {
		val decoder = sequentialIterator.next()
		val decodingState = decoder.decodeFrame(image)
		scannerCallback?.onFrameProcessed(decodingState)
	}

}