package ch.ubique.qrscanner.state

import ch.ubique.qrscanner.scanner.BarcodeFormat

sealed class DecodingState {
	data object NotFound : DecodingState()
	data class Decoded(val content: String, val format: BarcodeFormat) : DecodingState()
	data class Error(val errorCode: Int): DecodingState()
}