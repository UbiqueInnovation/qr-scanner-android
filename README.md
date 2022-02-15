# QR Code Scanner for Android

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](LICENSE)

QR Code Scanner for Android built by [Ubique](https://www.ubique.ch) based on AndroidX Camera2 with support for ZXing or MLKit decoding. 

## Download

This library is available on `mavenCentral()`

```kotlin
// A scanner implementation based on ZXing
implementation 'ch.ubique.android:qrscanner-zxing:1.0.0'

// A scanner implementation based on MLKit
implementation 'ch.ubique.android:qrscanner-mlkit:1.0.0'

// Jetpack Compose support
implementation 'ch.ubique.android:qrscanner-support:1.0.0'
```

## Quick Start

Check out the example app included in this repository on how to use the library.
Note: Camera permission handling is **not** handled by the library, it is the responsibility of the client app to request the permission.

### Android View

Include the view in your layout:
```xml
<ch.ubique.qrscanner.view.QrScannerView
    android:id="@+id/qrScanner"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

Define the image decoders to be used to scan for QR codes:
```kotlin
qrScanner.setImageDecoders(
    MLKitImageDecoder(),            // Available with the qrscanner-mlkit dependency
	GlobalHistogramImageDecoder(),  // Available with the qrscanner-zxing dependency
    HybridImageDecoder(),           // Available with the qrscanner-zxing dependency
)
```

Set a callback for the decoding state:
```kotlin
qrScanner.setScannercallback { state ->
    when (state) {
		is DecodingState.NotFound -> {} // None of the image decoders could detect a QR code
		is DecodingState.Decoded -> {}  // state.content contains the decoded QR code content
		is DecodingState.Error -> {}    // state.errorCode indicates the type of error that occured during the decoding
	}
}
```

Optional settings:
```kotlin
qrScanner.setFlash(false) // Activate/Deactivate the camera flash
qrScanner.setLinearZoom(0f) // Set the camera zoom on a linear scale from 0f to 1f
qrScanner.setFocusOnTap(true) // Enable/disable camera focus on tap
qrScanner.setScanningMode(ScanningMode.PARALLEL) // Change the scanning behavior when using multiple image decoders
```

### Jetpack Compose

Invoke the composable in your screen:
```kotlin
QrScanner(
    imageDecoders = listOf(
		MLKitImageDecoder(),            // Available with the qrscanner-mlkit dependency
		GlobalHistogramImageDecoder(),  // Available with the qrscanner-zxing dependency
		HybridImageDecoder(),           // Available with the qrscanner-zxing dependency
    ),
    scannerCallback = QrScannerCallback { state ->
		when (state) {
			is DecodingState.NotFound -> {} // None of the image decoders could detect a QR code
			is DecodingState.Decoded -> {}  // state.content contains the decoded QR code content
			is DecodingState.Error -> {}    // state.errorCode indicates the type of error that occured during the decoding
		}
    },
    modifier = Modifier.fillMaxSize(),
    scanningMode = ScanningMode.PARALLEL,
    isFlashEnabled = remember { mutableStateOf(false) },
    linearZoom = remember { mutableStateOf(0f) },
)
```

## License

This project is licensed under the terms of the MPL 2 license. See the [LICENSE](LICENSE) file for details.