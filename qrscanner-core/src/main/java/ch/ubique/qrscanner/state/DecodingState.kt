package ch.ubique.qrscanner.state

sealed class DecodingState {
	object NotFound : DecodingState()
	data class Decoded(val content: String) : DecodingState()
	data class Error(val errorCode: Int): DecodingState()
}