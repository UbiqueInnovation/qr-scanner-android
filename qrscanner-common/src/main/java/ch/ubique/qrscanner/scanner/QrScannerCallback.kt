package ch.ubique.qrscanner.scanner

import ch.ubique.qrscanner.state.DecodingState

fun interface QrScannerCallback {
	fun onFrameProcessed(state: DecodingState)
}