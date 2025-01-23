package ch.ubique.qrscanner.example

import android.Manifest
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.ubique.qrscanner.compose.QrScanner
import ch.ubique.qrscanner.example.databinding.ActivityComposeBinding
import ch.ubique.qrscanner.mlkit.decoder.MLKitImageDecoder
import ch.ubique.qrscanner.scanner.BarcodeFormat
import ch.ubique.qrscanner.scanner.QrScannerCallback
import ch.ubique.qrscanner.state.DecodingState
import ch.ubique.qrscanner.zxing.decoder.GlobalHistogramImageDecoder
import ch.ubique.qrscanner.zxing.decoder.HybridImageDecoder
import kotlinx.coroutines.flow.MutableStateFlow

class ComposeActivity : AppCompatActivity() {

	private lateinit var binding: ActivityComposeBinding

	private val cameraPermisisonLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
		// The composable automatically recomposes when the permission is granted
	}

	private val decodingState = MutableStateFlow<DecodingState>(DecodingState.NotFound)
	private val zoomRatio = MutableStateFlow(0f)
	private val isFlashEnabled = MutableStateFlow(false)

	private val scannerCallback = QrScannerCallback { decodingState.value = it }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		binding = ActivityComposeBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.composeView.setContent {
			Box(Modifier.fillMaxSize()) {
				val formats = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128)

				QrScanner(
					imageDecoders = listOf(
						MLKitImageDecoder(formats),
						GlobalHistogramImageDecoder(formats),
						HybridImageDecoder(formats)
					),
					scannerCallback = scannerCallback,
					modifier = Modifier.fillMaxSize(),
					isFlashEnabled = isFlashEnabled.collectAsState(),
					linearZoom = zoomRatio.collectAsState(),
				)

				Column(
					modifier = Modifier
						.fillMaxWidth()
						.align(Alignment.BottomCenter)
						.navigationBarsPadding()
						.consumeWindowInsets(WindowInsets.navigationBars)
				) {
					val decodingState = decodingState.collectAsState()
					val text = when (val state = decodingState.value) {
						is DecodingState.NotFound -> "Scanning"
						is DecodingState.Decoded -> state.content
						is DecodingState.Error -> "Error: ${state.errorCode}"
					}

					Text(
						text,
						modifier = Modifier
							.padding(start = 10.dp, end = 10.dp, bottom = 5.dp)
							.fillMaxWidth()
							.background(Color(0x80000000))
							.padding(10.dp),
						color = Color.White
					)

					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
					) {
						Slider(
							value = zoomRatio.collectAsState().value,
							onValueChange = { zoomRatio.value = it },
							valueRange = 0f..1f,
							modifier = Modifier.weight(1f)
						)
						Button(
							modifier = Modifier.wrapContentWidth(),
							onClick = { isFlashEnabled.value = !isFlashEnabled.value }
						) {
							Text("Toggle Flash")
						}
					}
				}
			}
		}
	}

	override fun onStart() {
		super.onStart()
		cameraPermisisonLauncher.launch(Manifest.permission.CAMERA)
	}

}