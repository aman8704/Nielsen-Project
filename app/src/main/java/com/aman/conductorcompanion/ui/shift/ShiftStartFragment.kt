package com.aman.conductorcompanion.ui.shift

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aman.conductorcompanion.R
import com.aman.conductorcompanion.databinding.FragmentShiftStartBinding

class ShiftStartFragment : Fragment() {
    private var _binding: FragmentShiftStartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShiftStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnStartShift.setOnClickListener {
            findNavController().navigate(R.id.action_start_to_ticket)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}