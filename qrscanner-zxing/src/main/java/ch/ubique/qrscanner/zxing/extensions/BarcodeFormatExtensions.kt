package ch.ubique.qrscanner.zxing.extensions

import ch.ubique.qrscanner.scanner.BarcodeFormat
import com.google.zxing.BarcodeFormat as ZxingBarcodeFormat

internal fun BarcodeFormat.toZxingFormat(): ZxingBarcodeFormat {
	return when (this) {
		BarcodeFormat.QR_CODE -> ZxingBarcodeFormat.QR_CODE
		BarcodeFormat.AZTEC -> ZxingBarcodeFormat.AZTEC
		BarcodeFormat.CODE_128 -> ZxingBarcodeFormat.CODE_128
		BarcodeFormat.CODE_39 -> ZxingBarcodeFormat.CODE_39
		BarcodeFormat.CODE_93 -> ZxingBarcodeFormat.CODE_93
		BarcodeFormat.CODABAR -> ZxingBarcodeFormat.CODABAR
		BarcodeFormat.DATA_MATRIX -> ZxingBarcodeFormat.DATA_MATRIX
		BarcodeFormat.EAN_13 -> ZxingBarcodeFormat.EAN_13
		BarcodeFormat.EAN_8 -> ZxingBarcodeFormat.EAN_8
		BarcodeFormat.ITF -> ZxingBarcodeFormat.ITF
		BarcodeFormat.PDF_417 -> ZxingBarcodeFormat.PDF_417
		BarcodeFormat.UPC_A -> ZxingBarcodeFormat.UPC_A
		BarcodeFormat.UPC_E -> ZxingBarcodeFormat.UPC_E
	}
}

internal fun ZxingBarcodeFormat.toUbiqueFormat(): BarcodeFormat? {
	return when (this) {
		ZxingBarcodeFormat.AZTEC -> BarcodeFormat.AZTEC
		ZxingBarcodeFormat.CODABAR -> BarcodeFormat.CODABAR
		ZxingBarcodeFormat.CODE_39 -> BarcodeFormat.CODE_39
		ZxingBarcodeFormat.CODE_93 -> BarcodeFormat.CODE_93
		ZxingBarcodeFormat.CODE_128 -> BarcodeFormat.CODE_128
		ZxingBarcodeFormat.DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
		ZxingBarcodeFormat.EAN_8 -> BarcodeFormat.EAN_8
		ZxingBarcodeFormat.EAN_13 -> BarcodeFormat.EAN_13
		ZxingBarcodeFormat.ITF -> BarcodeFormat.ITF
		ZxingBarcodeFormat.PDF_417 -> BarcodeFormat.PDF_417
		ZxingBarcodeFormat.QR_CODE -> BarcodeFormat.QR_CODE
		ZxingBarcodeFormat.UPC_A -> BarcodeFormat.UPC_A
		ZxingBarcodeFormat.UPC_E -> BarcodeFormat.UPC_E
		else -> null
	}
}