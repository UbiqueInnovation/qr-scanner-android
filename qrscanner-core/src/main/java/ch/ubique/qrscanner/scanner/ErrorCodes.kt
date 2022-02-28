package ch.ubique.qrscanner.scanner

object ErrorCodes {
	/**
	 * Happens when the image proxy input has the wrong format (YUV format is expected)
	 */
	const val INPUT_WRONG_FORMAT = 1

	/**
	 * The image decoder failed to read the image proxy input
	 */
	const val INPUT_READ_FAILED = 2
}