package ch.ubique.qrscanner.scanner

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import ch.ubique.qrscanner.state.DecodingState

interface ImageDecoder {
	/**
	 * Decode a frame coming from the camera preview as an [ImageProxy]
	 */
	suspend fun decodeFrame(image: ImageProxy): DecodingState

	/**
	 * Decode a bitmap which may come from any source such as a file
	 */
	suspend fun decodeBitmap(bitmap: Bitmap): DecodingState
}