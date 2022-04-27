package ch.ubique.qrscanner.scanner

fun interface CameraStateCallback {
	fun onCameraStateChanged(isActive: Boolean)
}