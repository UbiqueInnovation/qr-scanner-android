package ch.ubique.qrscanner.example.compose

import android.content.ClipData
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import ch.ubique.qrscanner.compose.QrScanner
import ch.ubique.qrscanner.example.R
import ch.ubique.qrscanner.mlkit.decoder.MLKitImageDecoder
import ch.ubique.qrscanner.scanner.BarcodeFormat
import ch.ubique.qrscanner.scanner.QrScannerCallback
import ch.ubique.qrscanner.state.DecodingState
import ch.ubique.qrscanner.zxing.decoder.GlobalHistogramImageDecoder
import ch.ubique.qrscanner.zxing.decoder.HybridImageDecoder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ComposeScannerScreen(
	scannerCallback: QrScannerCallback,
	decodingState: State<DecodingState>,
	isFlashEnabled: State<Boolean>,
	zoomRatio: State<Float>,
	onFlashToggled: (Boolean) -> Unit,
	onZoomRatioChanged: (Float) -> Unit,
) {
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
			isFlashEnabled = isFlashEnabled,
			linearZoom = zoomRatio,
		)

		Column(
			modifier = Modifier
				.fillMaxWidth()
				.align(Alignment.BottomCenter)
				.navigationBarsPadding()
				.consumeWindowInsets(WindowInsets.navigationBars)
		) {
			val clipboard = LocalClipboard.current
			val coroutineScope = rememberCoroutineScope()
			Surface(
				color = Color(0x80000000),
				shape = RoundedCornerShape(8.dp),
				modifier = Modifier
					.padding(horizontal = 10.dp, vertical = 5.dp)
					.fillMaxWidth(),
				onClick = {
					val decodedContent = (decodingState.value as? DecodingState.Decoded)?.content

					if (decodedContent != null) {
						coroutineScope.launch {
							val clipData = ClipData.newPlainText(decodedContent, decodedContent)
							clipboard.setClipEntry(ClipEntry(clipData))
						}
					}
				}
			) {
				AnimatedContent(
					targetState = decodingState.value,
					transitionSpec = {
						fadeIn(animationSpec = tween(220, delayMillis = 90)) togetherWith fadeOut(animationSpec = tween(90))
					},
					modifier = Modifier.padding(5.dp),
				) { state ->
					Row(
						modifier = Modifier
							.fillMaxWidth()
							.padding(5.dp),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalAlignment = Alignment.CenterVertically,
					) {
						when (state) {
							is DecodingState.NotFound -> Text("Scanning", color = Color.White)
							is DecodingState.Decoded -> {
								Text(state.content, color = Color.White, modifier = Modifier.weight(1f))
								Spacer(Modifier.width(5.dp))
								Icon(ImageVector.vectorResource(R.drawable.ic_copy), contentDescription = null, tint = Color.White)
							}
							is DecodingState.Error -> Text("Error: ${state.errorCode}", color = Color.White)
						}
					}
				}
			}

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
			) {
				Slider(
					value = zoomRatio.value,
					onValueChange = onZoomRatioChanged,
					valueRange = 0f..1f,
					modifier = Modifier.weight(1f)
				)
				Button(
					modifier = Modifier.wrapContentWidth(),
					onClick = { onFlashToggled.invoke(!isFlashEnabled.value) }
				) {
					Text("Toggle Flash")
				}
			}
		}
	}
}