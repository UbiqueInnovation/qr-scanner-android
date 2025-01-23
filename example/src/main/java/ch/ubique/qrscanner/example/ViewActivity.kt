package ch.ubique.qrscanner.example

import android.Manifest
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ch.ubique.qrscanner.example.databinding.ActivityViewBinding
import ch.ubique.qrscanner.mlkit.decoder.MLKitImageDecoder
import ch.ubique.qrscanner.scanner.BarcodeFormat
import ch.ubique.qrscanner.state.DecodingState
import ch.ubique.qrscanner.zxing.decoder.GlobalHistogramImageDecoder
import ch.ubique.qrscanner.zxing.decoder.HybridImageDecoder

class ViewActivity : AppCompatActivity() {

	private lateinit var binding: ActivityViewBinding
	private var isFlashEnabled = false

	private val cameraPermisisonLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
		if (isGranted) {
			binding.qrScanner.activateCamera()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		binding = ActivityViewBinding.inflate(layoutInflater)
		setContentView(binding.root)

		val formats = listOf(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_128)

		binding.qrScanner.setImageDecoders(
			MLKitImageDecoder(formats),
			GlobalHistogramImageDecoder(formats),
			HybridImageDecoder(formats)
		)

		binding.qrScanner.setScannerCallback { state ->
			when (state) {
				is DecodingState.NotFound -> binding.decodingState.text = "Scanning"
				is DecodingState.Decoded -> binding.decodingState.text = state.content
				is DecodingState.Error -> binding.decodingState.text = "Error: ${state.errorCode}"
			}
		}

		binding.cameraFlash.setOnClickListener {
			isFlashEnabled = !isFlashEnabled
			binding.qrScanner.setFlash(isFlashEnabled)
		}

		binding.cameraZoom.addOnChangeListener { _, value, _ ->
			binding.qrScanner.setLinearZoom(value)
		}
	}

	override fun onStart() {
		super.onStart()

		cameraPermisisonLauncher.launch(Manifest.permission.CAMERA)
	}

}