package ch.ubique.qrscanner.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ch.ubique.qrscanner.example.databinding.ActivityMainBinding
import ch.ubique.qrscanner.state.DecodingState
import ch.ubique.qrscanner.util.CameraUtil
import ch.ubique.qrscanner.zxing.decoder.GlobalHistogramImageDecoder
import ch.ubique.qrscanner.zxing.decoder.HybridImageDecoder

class MainActivity : AppCompatActivity() {

	companion object {
		private const val PERMISSION_REQUEST_CAMERA = 13
	}

	private lateinit var binding: ActivityMainBinding

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		binding = ActivityMainBinding.inflate(layoutInflater)
		setContentView(binding.root)

		binding.qrScanner.setImageDecoders(GlobalHistogramImageDecoder(), HybridImageDecoder())
		binding.qrScanner.setScannerCallback { state ->
			when (state) {
				is DecodingState.NotFound -> binding.decodingState.text = "Scanning"
				is DecodingState.Decoded -> binding.decodingState.text = state.content
				is DecodingState.Error -> binding.decodingState.text = "Error: ${state.errorCode}"
			}
		}
	}

	override fun onStart() {
		super.onStart()

		if (!CameraUtil.hasCameraPermission(this)) {
			requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CAMERA)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		if (requestCode == PERMISSION_REQUEST_CAMERA) {
			val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
			if (isGranted) {
				binding.qrScanner.onPermissionGranted()
			}
		}
	}

}