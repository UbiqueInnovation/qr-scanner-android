package ch.ubique.qrscanner.scanner

enum class ScanningMode {
	/**
	 * Parallel scanning mode tries to decode every frame with each image decoder in parallel
	 */
	PARALLEL,

	/**
	 * Sequential scanning mode tries to decode every frame with each image decoder sequentially, in the order they were set
	 */
	SEQUENTIAL,

	/**
	 * Alternating scanning mode tries to decode every frame with another image decoder, in the order they were set
	 */
	ALTERNATING,
}