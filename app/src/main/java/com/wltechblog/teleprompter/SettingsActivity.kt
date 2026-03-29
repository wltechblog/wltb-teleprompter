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

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.wltechblog.teleprompter.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings)

        loadPreferences()
        setupFontSizeSeekBar()
        setupSpeedSeekBar()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ── Prefs ────────────────────────────────────────────────────────────────

    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fontSize = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        val speed    = prefs.getInt(KEY_DEFAULT_SPEED, DEFAULT_SPEED)

        binding.fontSizeSeekBar.progress = fontSize - MIN_FONT_SIZE_SP
        applyFontSize(fontSize)

        binding.defaultSpeedSeekBar.progress = speed
        updateSpeedLabel(speed)
    }

    private fun save(fontSize: Int, speed: Int) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_FONT_SIZE, fontSize)
            .putInt(KEY_DEFAULT_SPEED, speed)
            .apply()
    }

    // ── Font size seekbar ─────────────────────────────────────────────────────

    private fun setupFontSizeSeekBar() {
        binding.fontSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyFontSize(progress + MIN_FONT_SIZE_SP)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                commitAll()
            }
        })
    }

    private fun applyFontSize(fontSizeSp: Int) {
        binding.fontSizeValueLabel.text = getString(R.string.font_size_value, fontSizeSp)
        binding.fontSizePreview.textSize = fontSizeSp.toFloat()
    }

    // ── Speed seekbar ─────────────────────────────────────────────────────────

    private fun setupSpeedSeekBar() {
        binding.defaultSpeedSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSpeedLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                commitAll()
            }
        })
    }

    private fun updateSpeedLabel(speed: Int) {
        binding.speedValueLabel.text = speed.toString()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun currentFontSize() =
        binding.fontSizeSeekBar.progress + MIN_FONT_SIZE_SP

    private fun currentSpeed() =
        binding.defaultSpeedSeekBar.progress

    private fun commitAll() = save(currentFontSize(), currentSpeed())

    // ── Constants ─────────────────────────────────────────────────────────────

    companion object {
        const val PREFS_NAME        = "teleprompter_prefs"
        const val KEY_FONT_SIZE     = "font_size"
        const val KEY_DEFAULT_SPEED = "default_speed"
        const val DEFAULT_FONT_SIZE = 24
        const val DEFAULT_SPEED     = 30
        const val MIN_FONT_SIZE_SP  = 12
    }
}

