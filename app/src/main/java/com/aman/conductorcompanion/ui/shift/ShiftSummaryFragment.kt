package com.aman.conductorcompanion.ui.shift

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aman.conductorcompanion.databinding.FragmentShiftSummaryBinding

class ShiftSummaryFragment : Fragment() {
    private var _binding: FragmentShiftSummaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShiftSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Summary implementation
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}