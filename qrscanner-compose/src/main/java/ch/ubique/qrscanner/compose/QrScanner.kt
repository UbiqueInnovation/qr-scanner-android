package ch.ubique.qrscanner.compose

import android.view.Display
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import ch.ubique.qrscanner.scanner.ImageAnalyzer
import ch.ubique.qrscanner.scanner.ImageDecoder
import ch.ubique.qrscanner.scanner.QrScannerCallback
import ch.ubique.qrscanner.scanner.ScanningMode
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun QrScanner(
	imageDecoders: List<ImageDecoder>,
	scannerCallback: QrScannerCallback,
	modifier: Modifier = Modifier,
	scanningMode: ScanningMode = ScanningMode.PARALLEL,
	isFlashEnabled: State<Boolean> = remember { mutableStateOf(false) },
	linearZoom: State<Float> = remember { mutableFloatStateOf(0f) },
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	val mainExecutor = ContextCompat.getMainExecutor(context)

	val rotation = DisplayManagerCompat.getInstance(context).getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0

	val preview = Preview.Builder()
		.setResolutionSelector(
			ResolutionSelector.Builder()
				.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
				.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
				.build()
		)
		.setTargetRotation(rotation)
		.build()

	val cameraSelector = CameraSelector.Builder()
		.requireLensFacing(CameraSelector.LENS_FACING_BACK)
		.build()

	val imageAnalysis = ImageAnalysis.Builder()
		.setResolutionSelector(
			ResolutionSelector.Builder()
				.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
				.setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
				.build()
		)
		.setTargetRotation(rotation)
		.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
		.build()

	val analyzer = ImageAnalyzer(lifecycleOwner.lifecycleScope)
	analyzer.setImageDecoders(imageDecoders)
	analyzer.setScannerCallback(scannerCallback)
	analyzer.setScanningMode(scanningMode)
	imageAnalysis.setAnalyzer(mainExecutor, analyzer)

	val camera = remember { mutableStateOf<Camera?>(null) }
	val previewView = remember { PreviewView(context) }

	LaunchedEffect(previewView) {
		val cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
			val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
			cameraProviderFuture.addListener({
				continuation.resume(cameraProviderFuture.get())
			}, mainExecutor)
		}
		cameraProvider.unbindAll()
		camera.value = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
		preview.setSurfaceProvider(previewView.surfaceProvider)
	}

	LaunchedEffect(isFlashEnabled.value) {
		camera.value?.cameraControl?.enableTorch(isFlashEnabled.value)
	}

	LaunchedEffect(linearZoom.value) {
		camera.value?.cameraControl?.setLinearZoom(linearZoom.value)
	}

	AndroidView(
		modifier = modifier,
		factory = { previewView },
		update = {},
	)
}