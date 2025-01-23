package ch.ubique.qrscanner.zxing.scanner

import ch.ubique.qrscanner.scanner.BarcodeFormat
import ch.ubique.qrscanner.zxing.extensions.toZxingFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader

internal object BarcodeScannerUtils {

	fun createBarcodeReader(
		formats: List<BarcodeFormat>,
		decodingHints: Map<DecodeHintType, *>,
	): MultiFormatReader {
		return MultiFormatReader().apply {
			if (formats.isEmpty()) {
				// If no formats are provided, the default is to scan all formats.
				// This reduces scanning performance drastically, so it is always advised to provide specific formats
				setHints(decodingHints)
			} else {
				setHints(
					decodingHints.plus(
						DecodeHintType.POSSIBLE_FORMATS to formats.map { it.toZxingFormat() }
					)
				)
			}
		}
	}

}