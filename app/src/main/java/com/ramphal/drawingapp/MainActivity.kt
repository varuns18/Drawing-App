package com.ramphal.drawingapp

import android.Manifest
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private var drawingView: DrawingView? = null
    private var currentColorButton: MaterialButton? = null
    var colorDialog: AlertDialog? = null
    var btnUndo: MaterialButton? = null
    var btnRedo: MaterialButton? = null

    val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result->
            if (result.resultCode == RESULT_OK && result.data != null){
                val imageBackground: ImageView = findViewById(R.id.iv_background)
                imageBackground.setImageURI(result.data?.data)
            }
        }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                val view = findViewById<View>(R.id.main)
                if (isGranted){
                    val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)
                }else{
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Snackbar.make(view,
                            "Oops you just denied the permission",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_brush_size, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.dialog_radio_group)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()
        colorDialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.bottom_sheet_color_picker)
            .create()

        drawingView?.setSizeForBrush(7f)

        val thicknessBtn: MaterialButton = findViewById(R.id.btn_thickness)
        thicknessBtn.setOnClickListener {
            showBrushSizeDialog(radioGroup, dialog)
        }

        val addImage: MaterialButton = findViewById(R.id.btn_add_image)
        addImage.setOnClickListener {
            requestStoragePermission()
        }

        val btnColor: MaterialButton = findViewById(R.id.btn_color)
        btnColor.setOnClickListener {
            showColorDialog(colorDialog!!)
        }

        updateUndoRedoButtonState()
        btnUndo = findViewById(R.id.btn_undo)
        btnUndo?.setOnClickListener {
            drawingView?.onClickUndo()
            updateUndoRedoButtonState()
        }

        btnRedo = findViewById(R.id.btn_redo)
        btnRedo?.setOnClickListener {
            drawingView?.onClickRedo()
            updateUndoRedoButtonState()
        }

        drawingView?.onStateChangeListener = {
            updateUndoRedoButtonState()
        }

    }

    private fun updateUndoRedoButtonState() {
        btnUndo?.isEnabled = drawingView?.canUndo() ?: false
        btnRedo?.isEnabled = drawingView?.canRedo() ?: false
    }

    private fun requestStoragePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
            ){
            showRationalDialog(
                title = "Storage Permission Required",
                message = "Storage permission is necessary for this feature. Please enable it in app settings."
            )
        }else{
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            )
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

    private fun showRationalDialog(
        title: String,
        message: String
    ) {

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }




}