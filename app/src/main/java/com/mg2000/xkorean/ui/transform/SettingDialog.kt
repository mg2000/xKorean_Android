package com.mg2000.xkorean.ui.transform

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.fragment.app.DialogFragment
import com.mg2000.xkorean.databinding.SettingDialogBinding

class SettingDialog : DialogFragment() {
    private var _binding: SettingDialogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = SettingDialogBinding.inflate(LayoutInflater.from(context))
        return AlertDialog.Builder(requireContext()).setView(binding.root).create()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}