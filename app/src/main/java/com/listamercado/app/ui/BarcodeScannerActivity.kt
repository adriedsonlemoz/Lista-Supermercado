package com.listamercado.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
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

class BarcodeScannerActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var scanner: BarcodeScanner? = null
    @Volatile private var resultDelivered = false

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
        cameraExecutor = Executors.newSingleThreadExecutor()
        findViewById<ImageButton>(R.id.buttonCloseScanner).setOnClickListener { finish() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val cameraProvider = runCatching { providerFuture.get() }.getOrElse {
                Toast.makeText(this, "Não foi possível iniciar a câmera.", Toast.LENGTH_LONG).show()
                finish()
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

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

                val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                scanner?.process(input)
                    ?.addOnSuccessListener { barcodes ->
                        val value = barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf { value -> value.isNotEmpty() } }
                        if (value != null) deliverResult(value)
                    }
                    ?.addOnFailureListener {
                        // A frame inválido é ignorado; a análise continua no próximo frame.
                    }
                    ?.addOnCompleteListener { imageProxy.close() }
                    ?: imageProxy.close()
            }

            runCatching {
                val selector = when {
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> error("Nenhuma câmera disponível")
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, analysis)
            }.onFailure {
                Toast.makeText(this, "Nenhuma câmera disponível para leitura.", Toast.LENGTH_LONG).show()
                finish()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun deliverResult(value: String) {
        if (resultDelivered) return
        resultDelivered = true
        runOnUiThread {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_BARCODE, value))
            finish()
        }
    }

    override fun onDestroy() {
        scanner?.close()
        if (::cameraExecutor.isInitialized) cameraExecutor.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_BARCODE = "barcode"
    }
}
