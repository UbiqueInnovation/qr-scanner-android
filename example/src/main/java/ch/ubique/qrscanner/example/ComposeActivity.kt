package ch.ubique.qrscanner.example

import android.Manifest
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.ubique.qrscanner.example.compose.ComposeScannerScreen
import ch.ubique.qrscanner.example.databinding.ActivityComposeBinding
import ch.ubique.qrscanner.scanner.QrScannerCallback
import ch.ubique.qrscanner.state.DecodingState
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
			ComposeScannerScreen(
				scannerCallback = scannerCallback,
				decodingState = decodingState.collectAsStateWithLifecycle(),
				isFlashEnabled = isFlashEnabled.collectAsStateWithLifecycle(),
				zoomRatio = zoomRatio.collectAsStateWithLifecycle(),
				onFlashToggled = { isFlashEnabled.value = it },
				onZoomRatioChanged = { zoomRatio.value = it },
			)
		}
	}

	override fun onStart() {
		super.onStart()
		cameraPermisisonLauncher.launch(Manifest.permission.CAMERA)
	}

}