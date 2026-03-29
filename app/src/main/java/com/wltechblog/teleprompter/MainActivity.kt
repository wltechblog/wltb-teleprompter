// Copyright (C) 2026 Josh at WLTechBlog
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

package com.wltechblog.teleprompter

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.wltechblog.teleprompter.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var scrollHandler = Handler(Looper.getMainLooper())
    private var scrollRunnable: Runnable? = null
    private var isPlaying = false
    private var scrollSpeed = SettingsActivity.DEFAULT_SPEED
    private var scrollAccumulator = 0f
    private var currentScrollPosition = 0

    // Auto-hide controls during playback
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private lateinit var gestureDetector: GestureDetector

    private val REQUEST_PICK_FILE = 1

    // Camera & recording
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var lensFacing = CameraSelector.LENS_FACING_FRONT
    private lateinit var cameraExecutor: ExecutorService
    private var originalRecordButtonBackground: Drawable? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        gestureDetector = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean {
                    if (isPlaying) showControlsTemporarily()
                    return true
                }
            }
        )

        setupButtons()
        setupSeekBar()
        handleIntent(intent)
        checkAndRequestCameraPermissions()
    }

    override fun onResume() {
        super.onResume()
        applyPreferences()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun applyPreferences() {
        val prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val fontSize = prefs.getInt(SettingsActivity.KEY_FONT_SIZE, SettingsActivity.DEFAULT_FONT_SIZE)
        val speed    = prefs.getInt(SettingsActivity.KEY_DEFAULT_SPEED, SettingsActivity.DEFAULT_SPEED)

        binding.scriptTextView.textSize = fontSize.toFloat()

        scrollSpeed = speed
        binding.scrollSpeedSeekBar.progress = speed
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            loadScriptFromUri(uri)
        }
    }

    // ── Permissions ──────────────────────────────────────────────────────────

    private fun checkAndRequestCameraPermissions() {
        val required = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            required.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        val allGranted = required.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(required.toTypedArray())
        }
    }

    // ── Camera ───────────────────────────────────────────────────────────────

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val provider = cameraProvider ?: return

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Verify the requested lens is available; fall back to back if not
        if (!provider.hasCamera(cameraSelector)) {
            lensFacing = CameraSelector.LENS_FACING_BACK
            bindCameraUseCases()
            return
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.unbindAll()
            provider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
            binding.cameraPreview.visibility = View.VISIBLE
            binding.recordButton.isEnabled = true
            updateSwitchCameraButton(provider)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSwitchCameraButton(provider: ProcessCameraProvider) {
        val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        val hasBack = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        binding.switchCameraButton.isEnabled = hasFront && hasBack
    }

    private fun switchCamera() {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        // Stop any in-progress recording before switching
        if (activeRecording != null) {
            activeRecording?.stop()
            activeRecording = null
        }
        bindCameraUseCases()
    }

    // ── Recording ────────────────────────────────────────────────────────────

    private fun toggleRecording() {
        if (activeRecording != null) {
            activeRecording?.stop()
        } else {
            startRecording()
        }
    }

    private fun startRecording() {
        val vc = videoCapture ?: return

        val timestamp = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Teleprompter_$timestamp")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Teleprompter")
            }
        }

        val outputOptions = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()

        activeRecording = vc.output
            .prepareRecording(this, outputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(this)) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> onRecordingStarted()
                    is VideoRecordEvent.Finalize -> onRecordingFinalized(event)
                    else -> {}
                }
            }
    }

    private fun onRecordingStarted() {
        originalRecordButtonBackground = binding.recordButton.background
        binding.recordButton.text = getString(R.string.stop_recording)
        binding.recordButton.setBackgroundColor(
            ContextCompat.getColor(this, R.color.recording_red)
        )
        binding.switchCameraButton.isEnabled = false
    }

    private fun onRecordingFinalized(event: VideoRecordEvent.Finalize) {
        activeRecording = null
        binding.recordButton.text = getString(R.string.record)
        binding.recordButton.background = originalRecordButtonBackground
        cameraProvider?.let { updateSwitchCameraButton(it) }

        if (!event.hasError()) {
            Toast.makeText(this, getString(R.string.recording_saved), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                this,
                "${getString(R.string.recording_error)}: ${event.error}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // ── UI setup ─────────────────────────────────────────────────────────────

    private fun setupButtons() {
        binding.loadButton.setOnClickListener { openFilePicker() }
        binding.playPauseButton.setOnClickListener { toggleScroll() }
        binding.resetButton.setOnClickListener { resetScroll() }
        binding.recordButton.setOnClickListener { toggleRecording() }
        binding.switchCameraButton.setOnClickListener { switchCamera() }
    }

    private fun setupSeekBar() {
        binding.scrollSpeedSeekBar.setOnSeekBarChangeListener(
            object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean
                ) { scrollSpeed = progress }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                    if (isPlaying) { stopScroll(); startScroll() }
                }
            }
        )
    }

    // ── Controls visibility ──────────────────────────────────────────────────

    /** Reveal controls immediately and cancel any pending auto-hide. */
    private fun showControls() {
        uiHandler.removeCallbacks(hideControlsRunnable)
        supportActionBar?.show()
        binding.controlsPanel.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(CONTROLS_FADE_MS).start()
        }
    }

    /** Fade controls out and hide the action bar. */
    private fun hideControls() {
        supportActionBar?.hide()
        binding.controlsPanel.animate()
            .alpha(0f)
            .setDuration(CONTROLS_FADE_MS)
            .withEndAction { binding.controlsPanel.visibility = View.GONE }
            .start()
    }

    /** Show controls, then schedule an auto-hide after the timeout. */
    private fun showControlsTemporarily() {
        showControls()
        uiHandler.postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_MS)
    }

    /**
     * Feed every touch to the GestureDetector first.
     * onSingleTapUp fires only when the finger lifts without significant
     * movement — scroll drags are ignored, so manual scrolling is unaffected.
     * All events are forwarded to children unchanged via super.
     */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    // ── File picker ──────────────────────────────────────────────────────────

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.load_txt_file)),
            REQUEST_PICK_FILE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri -> loadScriptFromUri(uri) }
        }
    }

    private fun loadScriptFromUri(uri: android.net.Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                binding.scriptTextView.text = reader.readText()
                binding.playPauseButton.isEnabled = true
                binding.resetButton.isEnabled = true
                resetScroll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ── Scrolling ────────────────────────────────────────────────────────────

    private fun toggleScroll() {
        if (isPlaying) stopScroll() else startScroll()
    }

    private fun startScroll() {
        isPlaying = true
        scrollAccumulator = 0f
        binding.playPauseButton.text = getString(R.string.pause)
        hideControls()
        scrollRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    scrollAccumulator += calculateScrollAmount()
                    val pixels = scrollAccumulator.toInt()
                    if (pixels > 0) {
                        binding.scrollView.smoothScrollBy(0, pixels)
                        scrollAccumulator -= pixels
                    }
                    currentScrollPosition = binding.scrollView.scrollY
                    scrollHandler.postDelayed(this, 50)
                }
            }
        }
        scrollHandler.post(scrollRunnable!!)
    }

    private fun stopScroll() {
        isPlaying = false
        binding.playPauseButton.text = getString(R.string.play)
        scrollRunnable?.let { scrollHandler.removeCallbacks(it) }
        showControls()
    }

    private fun resetScroll() {
        stopScroll()
        binding.scrollView.fullScroll(View.FOCUS_UP)
        currentScrollPosition = 0
    }

    /**
     * Returns pixels to scroll per 50 ms tick.
     * Uses a power curve (exponent 1.5) so the lower half of the slider
     * has fine-grained slow speeds.  Max output (slider = 100) equals what
     * the old 50 % position produced — about 100 px/sec.
     * Returns a Float; the caller accumulates fractional pixels so there is
     * no dead zone at the low end.
     */
    private fun calculateScrollAmount(): Float {
        val t = scrollSpeed / 100.0
        return (Math.pow(t, 1.5) * MAX_PIXELS_PER_TICK).toFloat()
    }

    companion object {
        // Pixels scrolled per 50 ms tick at slider max (= old "medium" speed)
        private const val MAX_PIXELS_PER_TICK = 5.0
        // Controls auto-hide behaviour
        private const val CONTROLS_AUTO_HIDE_MS = 5_000L
        private const val CONTROLS_FADE_MS      = 250L
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroy() {
        super.onDestroy()
        scrollRunnable?.let { scrollHandler.removeCallbacks(it) }
        uiHandler.removeCallbacks(hideControlsRunnable)
        activeRecording?.stop()
        cameraExecutor.shutdown()
    }
}
