package ch.ubique.qrscanner.scanner

fun interface CameraErrorCallback {
	fun onCameraError(throwable: Throwable)
}
