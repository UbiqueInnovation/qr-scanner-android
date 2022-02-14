package ch.ubique.qrscanner.scanner

import androidx.camera.core.ImageProxy
import ch.ubique.qrscanner.state.DecodingState

fun interface ImageDecoder {
	fun decodeFrame(image: ImageProxy): DecodingState
}