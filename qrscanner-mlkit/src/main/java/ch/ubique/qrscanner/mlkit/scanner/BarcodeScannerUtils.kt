package ch.ubique.qrscanner.mlkit.scanner

import ch.ubique.qrscanner.mlkit.extensions.toMlKitFormat
import ch.ubique.qrscanner.scanner.BarcodeFormat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions

internal object BarcodeScannerUtils {

	fun createBarcodeScannerOptions(formats: List<BarcodeFormat>): BarcodeScannerOptions {
		return if (formats.isEmpty()) {
			// If no formats are provided, the default is to scan all formats.
			// This reduces scanning performance drastically, so it is always advised to provide specific formats
			BarcodeScannerOptions.Builder().build()
		} else {
			val mlKitFormats = formats.map { it.toMlKitFormat() }
			BarcodeScannerOptions.Builder()
				.setBarcodeFormats(mlKitFormats.first(), *mlKitFormats.drop(1).toIntArray())
				.build()
		}
	}

}