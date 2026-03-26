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

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wltechblog.teleprompter.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var scrollHandler = Handler(Looper.getMainLooper())
    private var scrollRunnable: Runnable? = null
    private var isPlaying = false
    private var scrollSpeed = 50
    private var currentScrollPosition = 0

    private val REQUEST_PICK_FILE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupButtons()
        setupSeekBar()

        handleIntent(intent)
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

    private fun setupButtons() {
        binding.loadButton.setOnClickListener {
            openFilePicker()
        }

        binding.playPauseButton.setOnClickListener {
            toggleScroll()
        }

        binding.resetButton.setOnClickListener {
            resetScroll()
        }
    }

    private fun setupSeekBar() {
        binding.scrollSpeedSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                scrollSpeed = progress
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                if (isPlaying) {
                    stopScroll()
                    startScroll()
                }
            }
        })
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, getString(R.string.load_txt_file)), REQUEST_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                loadScriptFromUri(uri)
            }
        }
    }

    private fun loadScriptFromUri(uri: android.net.Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val script = reader.readText()
                binding.scriptTextView.text = script
                binding.playPauseButton.isEnabled = true
                binding.resetButton.isEnabled = true
                resetScroll()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun toggleScroll() {
        if (isPlaying) {
            stopScroll()
        } else {
            startScroll()
        }
    }

    private fun startScroll() {
        isPlaying = true
        binding.playPauseButton.text = getString(R.string.pause)
        
        scrollRunnable = object : Runnable {
            override fun run() {
                if (isPlaying) {
                    val scrollAmount = calculateScrollAmount()
                    binding.scrollView.smoothScrollBy(0, scrollAmount)
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
    }

    private fun resetScroll() {
        stopScroll()
        binding.scrollView.fullScroll(View.FOCUS_UP)
        currentScrollPosition = 0
    }

    private fun calculateScrollAmount(): Int {
        return (scrollSpeed / 10.0).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollRunnable?.let { scrollHandler.removeCallbacks(it) }
    }
}
