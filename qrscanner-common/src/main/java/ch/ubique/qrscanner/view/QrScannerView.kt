package ch.ubique.qrscanner.view

import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.view.Display
import android.view.Surface
import android.widget.FrameLayout
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import ch.ubique.qrscanner.scanner.ImageAnalyzer
import ch.ubique.qrscanner.scanner.ImageDecoder
import ch.ubique.qrscanner.scanner.QrScannerCallback
import ch.ubique.qrscanner.util.CameraUtil


class QrScannerView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	private val mainExecutor = ContextCompat.getMainExecutor(context)
	private val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

	private val previewView = PreviewView(context, attrs, defStyleAttr).apply {
		layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
	}

	private lateinit var camera: Camera
	private lateinit var preview: Preview
	private lateinit var imageAnalysis: ImageAnalysis
	private lateinit var imageAnalyzer: ImageAnalyzer

	private var viewTreeLifecycleOwner: LifecycleOwner? = null

	private var rotation = Surface.ROTATION_0
	private var isCameraActive = false

	private var imageDecoders: List<ImageDecoder> = emptyList()
	private var scannerCallback: QrScannerCallback? = null

	init {
		addView(previewView)
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		viewTreeLifecycleOwner = findViewTreeLifecycleOwner()

		DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)?.let {
			rotation = it.rotation
		}

		initializePreview()
		initializeAnalysis()

		if (CameraUtil.hasCameraPermission(context)) {
			activateCamera()
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		viewTreeLifecycleOwner = null
		deactivateCamera()
	}

	fun onPermissionGranted() {
		activateCamera()
	}

	fun setImageDecoders(vararg imageDecoders: ImageDecoder) {
		this.imageDecoders = imageDecoders.toList()

		if (this::imageAnalyzer.isInitialized) {
			imageAnalyzer.setImageDecoders(this.imageDecoders)
		}
	}

	fun setScannerCallback(scannerCallback: QrScannerCallback?) {
		this.scannerCallback = scannerCallback

		if (this::imageAnalyzer.isInitialized) {
			imageAnalyzer.setScannerCallback(this.scannerCallback)
		}
	}

	private fun initializePreview() {
		preview = Preview.Builder()
			.setTargetResolution(Size(720, 1280))
			.setTargetRotation(rotation)
			.build()
			.apply { setSurfaceProvider(previewView.surfaceProvider) }
	}

	private fun initializeAnalysis() {
		val lifecycleScope = viewTreeLifecycleOwner?.lifecycleScope ?: return
		imageAnalyzer = ImageAnalyzer(lifecycleScope)
		imageAnalyzer.setImageDecoders(imageDecoders)
		imageAnalyzer.setScannerCallback(scannerCallback)

		imageAnalysis = ImageAnalysis.Builder()
			.setTargetResolution(Size(720, 1280))
			.setTargetRotation(rotation)
			.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
			.build()
			.apply { setAnalyzer(mainExecutor, imageAnalyzer) }
	}

	private fun activateCamera() {
		val lifecycleOwner = viewTreeLifecycleOwner ?: return
		cameraProviderFuture.addListener({
			val cameraProvider = cameraProviderFuture.get()

			// TODO Add option to switch camera?
			val cameraSelector = CameraSelector.Builder()
				.requireLensFacing(CameraSelector.LENS_FACING_BACK)
				.build()

			camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
			isCameraActive = true
		}, mainExecutor)
	}

	private fun deactivateCamera() {
		cameraProviderFuture.addListener({
			val cameraProvider = cameraProviderFuture.get()
			cameraProvider.unbindAll()
			isCameraActive = false
		}, mainExecutor)
	}

}