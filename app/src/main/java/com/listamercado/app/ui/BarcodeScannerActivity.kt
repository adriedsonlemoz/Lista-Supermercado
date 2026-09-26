package com.listamercado.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.listamercado.app.R
import com.listamercado.app.util.InsetsHelper
import com.listamercado.app.util.ThemeController
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.hypot

class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var buttonTorch: MaterialButton
    private lateinit var scannerHint: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var scanner: BarcodeScanner? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var boundCamera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null
    @Volatile private var resultDelivered = false
    private var torchEnabled = false
    private var lastCandidateValue: String? = null
    private var lastCandidateHits = 0
    private var analysisFailures = 0

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            Toast.makeText(this, "A câmera é necessária para ler o código de barras.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.applySavedMode(this)
        setContentView(R.layout.activity_barcode_scanner)
        InsetsHelper.applyScaffold(this, findViewById(R.id.rootBarcodeScanner))

        previewView = findViewById(R.id.previewBarcode)
        buttonTorch = findViewById(R.id.buttonTorch)
        scannerHint = findViewById(R.id.textScannerHint)
        cameraExecutor = Executors.newSingleThreadExecutor()
        findViewById<ImageButton>(R.id.buttonCloseScanner).setOnClickListener { finish() }
        buttonTorch.setOnClickListener { toggleTorch() }
        updateTorchUi(enabled = false, supported = false)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            if (isFinishing || isDestroyed) return@addListener
            val provider = runCatching { providerFuture.get() }.getOrElse {
                showFatalScannerError("Não foi possível iniciar a câmera.")
                return@addListener
            }
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis = analysis

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_93,
                    Barcode.FORMAT_ITF,
                    Barcode.FORMAT_CODABAR
                )
                .build()
            scanner = BarcodeScanning.getClient(options)

            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null || resultDelivered) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val input = runCatching {
                    InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                }.getOrElse {
                    imageProxy.close()
                    return@setAnalyzer
                }

                val activeScanner = scanner
                if (activeScanner == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                activeScanner.process(input)
                    .addOnSuccessListener { barcodes ->
                        analysisFailures = 0
                        val candidate = chooseBestBarcode(barcodes)
                        if (candidate != null) {
                            if (candidate.value == lastCandidateValue) {
                                lastCandidateHits += 1
                            } else {
                                lastCandidateValue = candidate.value
                                lastCandidateHits = 1
                            }
                            if (lastCandidateHits >= REQUIRED_STABLE_FRAMES) {
                                deliverResult(candidate.value)
                            }
                        } else {
                            lastCandidateValue = null
                            lastCandidateHits = 0
                        }
                    }
                    .addOnFailureListener {
                        analysisFailures += 1
                        if (analysisFailures == 3) {
                            runOnUiThread {
                                if (!resultDelivered && !isFinishing) {
                                    scannerHint.text = "Não consegui confirmar o código. Afaste um pouco e mantenha a câmera firme."
                                }
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }

            runCatching {
                val selector = when {
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> error("Nenhuma câmera disponível")
                }
                provider.unbindAll()
                boundCamera = provider.bindToLifecycle(this, selector, preview, analysis)
                val hasFlash = boundCamera?.cameraInfo?.hasFlashUnit() == true
                updateTorchUi(enabled = false, supported = hasFlash)
            }.onFailure {
                showFatalScannerError("Nenhuma câmera disponível para leitura.")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun chooseBestBarcode(barcodes: List<Barcode>): ScanCandidate? {
        if (barcodes.isEmpty()) return null
        val centerX = previewView.width / 2f
        val centerY = previewView.height / 2f
        return barcodes
            .mapNotNull { barcode ->
                val value = normalizeCandidate(barcode) ?: return@mapNotNull null
                val box = barcode.boundingBox
                val score = if (box == null) {
                    Float.MAX_VALUE
                } else {
                    val distance = hypot(box.exactCenterX() - centerX, box.exactCenterY() - centerY)
                    val areaBoost = (box.width() * box.height()) * 0.0001f
                    distance - areaBoost
                }
                ScanCandidate(value = value, score = score)
            }
            .minByOrNull { it.score }
    }

    private fun normalizeCandidate(barcode: Barcode): String? {
        val raw = barcode.rawValue?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val compact = raw.replace(Regex("\\s+"), "")
        return when (barcode.format) {
            Barcode.FORMAT_EAN_13 -> compact.takeIf { it.length == 13 && it.all { char -> char.isDigit() } && hasValidGtinChecksum(it) }
            Barcode.FORMAT_EAN_8 -> compact.takeIf { it.length == 8 && it.all { char -> char.isDigit() } && hasValidGtinChecksum(it) }
            Barcode.FORMAT_UPC_A -> compact.takeIf { it.length == 12 && it.all { char -> char.isDigit() } && hasValidGtinChecksum(it) }
            Barcode.FORMAT_UPC_E -> compact.takeIf { it.length == 8 && it.all { char -> char.isDigit() } }
            Barcode.FORMAT_ITF -> compact.takeIf { it.length in 6..32 && it.length % 2 == 0 && it.all { char -> char.isDigit() } }
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_CODABAR -> raw.takeIf { it.length in 3..80 }
            else -> null
        }
    }

    private fun hasValidGtinChecksum(value: String): Boolean {
        if (value.length < 2 || !value.all { char -> char.isDigit() }) return false
        val body = value.dropLast(1)
        var sum = 0
        body.indices.reversed().forEach { index ->
            val distanceFromRight = body.lastIndex - index
            val weight = if (distanceFromRight % 2 == 0) 3 else 1
            sum += body[index].digitToInt() * weight
        }
        val expected = (10 - (sum % 10)) % 10
        return expected == value.last().digitToInt()
    }

    private fun toggleTorch() {
        val camera = boundCamera ?: return
        if (camera.cameraInfo.hasFlashUnit() != true) {
            Toast.makeText(this, "Esta câmera não possui flash.", Toast.LENGTH_SHORT).show()
            return
        }
        val nextState = !torchEnabled
        runCatching {
            camera.cameraControl.enableTorch(nextState)
            torchEnabled = nextState
            updateTorchUi(enabled = torchEnabled, supported = true)
        }.onFailure {
            Toast.makeText(this, "Não foi possível alterar a luz da câmera.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTorchUi(enabled: Boolean, supported: Boolean) {
        buttonTorch.isEnabled = supported
        buttonTorch.alpha = if (supported) 1f else 0.5f
        buttonTorch.text = when {
            !supported -> "Sem flash"
            enabled -> "Luz ligada"
            else -> "Luz"
        }
    }

    private fun deliverResult(value: String) {
        if (resultDelivered || value.isBlank()) return
        resultDelivered = true
        imageAnalysis?.clearAnalyzer()
        runOnUiThread {
            if (isFinishing || isDestroyed) return@runOnUiThread
            setResult(RESULT_OK, Intent().putExtra(EXTRA_BARCODE, value))
            scannerHint.text = "Código $value reconhecido"
            vibrateSuccessSafely()
            previewView.postDelayed({
                if (!isFinishing && !isDestroyed) finish()
            }, RESULT_FEEDBACK_DELAY_MS)
        }
    }

    private fun vibrateSuccessSafely() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
                    ?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        // Háptico é apenas feedback: falha de vibração nunca pode derrubar a leitura.
    }

    private fun showFatalScannerError(message: String) {
        if (isFinishing || isDestroyed) return
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        setResult(RESULT_CANCELED, Intent().putExtra(EXTRA_ERROR, message))
        finish()
    }

    override fun onDestroy() {
        runCatching { imageAnalysis?.clearAnalyzer() }
        runCatching { cameraProvider?.unbindAll() }
        runCatching { scanner?.close() }
        if (::cameraExecutor.isInitialized) runCatching { cameraExecutor.shutdown() }
        super.onDestroy()
    }

    private data class ScanCandidate(
        val value: String,
        val score: Float
    )

    companion object {
        const val EXTRA_BARCODE = "barcode"
        const val EXTRA_ERROR = "barcode_error"
        private const val REQUIRED_STABLE_FRAMES = 3
        private const val RESULT_FEEDBACK_DELAY_MS = 220L
    }
}
