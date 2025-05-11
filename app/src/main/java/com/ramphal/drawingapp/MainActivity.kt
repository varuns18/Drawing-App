package com.ramphal.drawingapp

import android.Manifest
import android.R.attr.bitmap
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

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
                    Snackbar.make(view,
                        "Permission granted now you can add image",
                        Snackbar.LENGTH_LONG
                    ).show()
                }else{
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE){
                        Snackbar.make(view,
                            "Oops you just denied the permission",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }else if (permissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE){
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
            if (isReadStorageAllowed()){
                val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            }else{
                requestStoragePermission()
            }

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

        val btnCleanAll: MaterialButton = findViewById(R.id.btn_clean_all)
        btnCleanAll.setOnClickListener {
            val imageBackground: ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageDrawable(null)
            drawingView?.clearDrawing()
        }

        val btnSave: MaterialButton = findViewById(R.id.btn_save)
        btnSave.setOnClickListener {
            if (isReadStorageAllowed()) {
                lifecycleScope.launch {
                    val drawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                    val bitmap = getBitmapFromView(drawingView)
                    download(bitmap)
                }
            }else{
                requestStoragePermission()
            }
        }

        drawingView?.onStateChangeListener = {
            updateUndoRedoButtonState()
        }

    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun updateUndoRedoButtonState() {
        btnUndo?.visibility = if (drawingView?.canUndo() == true) View.VISIBLE else View.GONE
        btnRedo?.visibility = if (drawingView?.canRedo() == true) View.VISIBLE else View.GONE
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
        }else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ){
            showRationalDialog(
                title = "Storage Permission Required",
                message = "Storage permission is necessary for this feature. Please enable it in app settings."
            )
        }else{
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
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

    private fun getBitmapFromView(view: View): Bitmap{
        val returnedBitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        }else {
            canvas.drawColor(ContextCompat.getColor(this, R.color.background))
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun download(mBitmap: Bitmap?): String{
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val drawingsDir = File(downloadsDir, "My Drawings")
                    if (!drawingsDir.exists()) {
                        drawingsDir.mkdirs()
                    }
                    val timestamp = System.currentTimeMillis() / 1000
                    val fileName = "$timestamp.png"
                    val f = File(drawingsDir, fileName)
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread {
                        if (result.isNotEmpty()){
                            Snackbar.make(findViewById(R.id.main), "Image saved successfully : $result", Snackbar.LENGTH_LONG).show()
                        }else{
                            Snackbar.make(findViewById(R.id.main), "Something went wrong while saving the image", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }catch (e: Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }


}