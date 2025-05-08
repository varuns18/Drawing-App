// ColorPickerBottomSheet.kt
package com.ramphal.drawingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ColorPickerBottomSheet(
    private val onColorSelected: (Int) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_color_picker, container, false)

    companion object {
        const val TAG = "ColorPicker"
    }

}
