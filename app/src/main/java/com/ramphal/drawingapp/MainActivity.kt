package com.ramphal.drawingapp

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var currentColorButton: MaterialButton? = null
    var colorDialogView: View? = null
    var colorDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        drawingView = findViewById(R.id.drawing_view)
        val btnOptions: MaterialButton = findViewById(R.id.btn_options)
        val thicknessBtn: MaterialButton = findViewById(R.id.btn_thickness)
        val dialogView = layoutInflater.inflate(R.layout.dialog_brush_size, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.dialog_radio_group)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        colorDialogView = layoutInflater.inflate(R.layout.bottom_sheet_color_picker, null)
        colorDialog = MaterialAlertDialogBuilder(this)
            .setView(colorDialogView)
            .create()

        drawingView?.setSizeForBrush(7f)
        thicknessBtn.setOnClickListener {
            showBrushSizeDialog(radioGroup, dialog)
        }

        btnOptions.setOnClickListener {
            showColorDialog(colorDialog!!)
        }


    }
    private fun showBrushSizeDialog(radioGroup: RadioGroup, dialog: AlertDialog) {

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.thickness_ultrafine -> {
                    drawingView?.setSizeForBrush(1f)
                    dialog.dismiss()
                }
                R.id.thickness_fine      -> {
                    drawingView?.setSizeForBrush(3f)
                    dialog.dismiss()
                }
                R.id.thickness_standard  -> {
                    drawingView?.setSizeForBrush(7f)
                    dialog.dismiss()
                }
                R.id.thickness_medium    -> {
                    drawingView?.setSizeForBrush(15f)
                    dialog.dismiss()
                }
                R.id.thickness_broad     -> {
                    drawingView?.setSizeForBrush(30f)
                    dialog.dismiss()
                }
                R.id.thickness_large     -> {
                    drawingView?.setSizeForBrush(50f)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showColorDialog(dialog: AlertDialog) {

        dialog.show()
    }

    fun onColorPicked(view: View) {
        val btn = view as MaterialButton
        currentColorButton?.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.outline))
        currentColorButton?.strokeWidth = 0
        val hex = btn.tag as String
        drawingView?.setColor(hex)
        currentColorButton = btn
        currentColorButton?.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary))
        currentColorButton?.strokeWidth = 4
        colorDialog?.dismiss()
    }




}