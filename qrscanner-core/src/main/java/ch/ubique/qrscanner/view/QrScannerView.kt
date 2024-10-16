package ch.ubique.qrscanner.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.view.Display
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.Surface
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.camera.core.*
import androidx.camera.core.impl.utils.ResolutionSelectorUtil
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.core.view.GestureDetectorCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import ch.ubique.qrscanner.scanner.*
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

	private var camera: Camera? = null
	private var preview: Preview? = null
	private var imageAnalysis: ImageAnalysis? = null
	private var imageAnalyzer: ImageAnalyzer? = null

	private var viewTreeLifecycleOwner: LifecycleOwner? = null

	private val tapToFocusDetector =  GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
		override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
			return if (isFocusOnTapEnabled) {
				startAutofocus(e.x, e.y)
				true
			} else {
				false
			}
		}
	})

	private var rotation = Surface.ROTATION_0
	private var isFocusOnTapEnabled = true
	private var autoActivateOnAttach = true

	private var imageDecoders: List<ImageDecoder> = emptyList()
	private var scannerCallback: QrScannerCallback? = null
	private var scanningMode = ScanningMode.PARALLEL
	private var cameraStateCallback: CameraStateCallback? = null

	var isCameraActive = false
		private set

	init {
		addView(previewView)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun onTouchEvent(event: MotionEvent): Boolean {
		return if (tapToFocusDetector.onTouchEvent(event)) {
			true
		} else {
			super.onTouchEvent(event)
		}
	}

	override fun onAttachedToWindow() {
		super.onAttachedToWindow()
		viewTreeLifecycleOwner = findViewTreeLifecycleOwner()

		DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)?.let {
			rotation = it.rotation
		}

		initializePreview()
		initializeAnalysis()

		if (CameraUtil.hasCameraPermission(context) && autoActivateOnAttach) {
			activateCamera()
		}
	}

	override fun onDetachedFromWindow() {
		super.onDetachedFromWindow()
		viewTreeLifecycleOwner = null
		deactivateCamera()
	}

	/**
	 * Activate the camera preview if not already active.
	 * If the app already has the camera permission, this is automatically called when the view is attached to the window.
	 * If the app does not already have the camera permission, this has to be called manually after the permission is granted.
	 */
	fun activateCamera() {
		if (isCameraActive) return

		val lifecycleOwner = viewTreeLifecycleOwner ?: return
		cameraProviderFuture.addListener({
			val cameraProvider = cameraProviderFuture.get()

			val cameraSelector = CameraSelector.Builder()
				.requireLensFacing(CameraSelector.LENS_FACING_BACK)
				.build()

			// Since the CameraProvider is a singleton and will return the same instance across multiple invocations, any other view
			// that has binded to that camera provider without unbinding will cause this view to throw an exception when trying to bind to the lifecycle.
			// This might happen e.g. if the camera view is used within a RecyclerView and an old view holder has not finished
			// calling deactivateCamera() before the new view holder already calls activateCamera()
			cameraProvider.unbindAll()
			camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
			setCameraState(true)
		}, mainExecutor)
	}

	/**
	 * Deactivate the camera preview
	 */
	fun deactivateCamera() {
		if (!isCameraActive) return

		cameraProviderFuture.addListener({
			val cameraProvider = cameraProviderFuture.get()
			cameraProvider.unbindAll()
			setCameraState(false)
		}, mainExecutor)
	}

	/**
	 * Add an option to disable the automatic camera activation when the view is attached to the window.
	 * This might be desirable if the client wants to control the (de-)activation of the camera preview themselves
	 */
	fun setAutoActivateOnAttach(autoActivateOnAttach: Boolean) {
		this.autoActivateOnAttach = autoActivateOnAttach
	}

	/**
	 * Set a callback for when the camera state changes due to calls to [activateCamera] and [deactivateCamera]
	 */
	fun setCameraStateCallback(callback: CameraStateCallback) {
		this.cameraStateCallback = callback
		callback.onCameraStateChanged(isCameraActive)
	}

	/**
	 * Set any number of image decoders that should be used to analyze the incoming camera frames
	 */
	fun setImageDecoders(vararg imageDecoders: ImageDecoder) {
		this.imageDecoders = imageDecoders.toList()
		imageAnalyzer?.setImageDecoders(this.imageDecoders)
	}

	/**
	 * Set a callback to be notified of the decoding results
	 */
	fun setScannerCallback(scannerCallback: QrScannerCallback?) {
		this.scannerCallback = scannerCallback
		imageAnalyzer?.setScannerCallback(this.scannerCallback)
	}

	/**
	 * Set the scanning mode of the analyzer
	 */
	fun setScanningMode(scanningMode: ScanningMode) {
		this.scanningMode = scanningMode
		imageAnalyzer?.setScanningMode(this.scanningMode)
	}

	/**
	 * Enable or disable the focus on tap functionality
	 */
	fun setFocusOnTap(enabled: Boolean) {
		isFocusOnTapEnabled = enabled
	}

	/**
	 * Enable or disable the camera flash
	 */
	fun setFlash(enabled: Boolean) {
		if (isCameraActive) {
			camera?.cameraControl?.enableTorch(enabled)
		}
	}

	/**
	 * Set the camera zoom on a linear scale, where 0f is the minimal possible camera zoom and 1f is the maximal possible camera zoom
	 */
	fun setLinearZoom(@FloatRange(from = 0.0, to = 1.0) zoomRatio: Float) {
		if (isCameraActive) {
			val correctedZoomRatio = zoomRatio.coerceIn(0f, 1f)
			camera?.cameraControl?.setLinearZoom(correctedZoomRatio)
		}
	}

	/**
	 * Start a focus and metering action for the given focus point. This is intended for regular autofocus intervals.
	 * If you need tap to focus behavior, use [setFocusOnTap] instead.
	 */
	fun startAutofocus(focusPointX: Float, focusPointY: Float) {
		if (isCameraActive) {
			val metricPointFactory = previewView.meteringPointFactory
			val point = metricPointFactory.createPoint(focusPointX, focusPointY)
			val action = FocusMeteringAction.Builder(point).build()
			camera?.cameraControl?.startFocusAndMetering(action)
		}
	}

	/**
	 * @return The active camera's info or null if it is not yet initialized
	 */
	fun getCameraInfo() = camera?.cameraInfo

	private fun initializePreview() {
		preview = Preview.Builder()
			.setResolutionSelector(
				ResolutionSelector.Builder()
					.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
					.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
					.build()
			)
			.setTargetRotation(rotation)
			.build()
			.apply { setSurfaceProvider(previewView.surfaceProvider) }
	}

	private fun initializeAnalysis() {
		val lifecycleScope = viewTreeLifecycleOwner?.lifecycleScope ?: return
		val analyzer = ImageAnalyzer(lifecycleScope)
		analyzer.setImageDecoders(imageDecoders)
		analyzer.setScannerCallback(scannerCallback)
		analyzer.setScanningMode(scanningMode)
		this.imageAnalyzer = analyzer

		imageAnalysis = ImageAnalysis.Builder()
			.setResolutionSelector(
				ResolutionSelector.Builder()
					.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
					.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
					.build()
			)
			.setTargetRotation(rotation)
			.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
			.build()
			.apply { setAnalyzer(mainExecutor, analyzer) }
	}

	private fun setCameraState(isActive: Boolean) {
		this.isCameraActive = isActive
		cameraStateCallback?.onCameraStateChanged(isActive)
	}

}