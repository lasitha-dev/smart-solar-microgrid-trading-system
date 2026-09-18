/**
 * Description: BottomSheet dialog providing defensive decimal input validation for actual
 * metered energy (0.01 - 999.99 kWh) and dispatching energy transfer finalization (FR-M4-07.1).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.DialogTransferFinalizeBinding

/**
 * Dialog prompting the Grid Operator to input and confirm actual delivered power readings.
 */
class TransferFinalizeDialog : BottomSheetDialogFragment() {

    private var _binding: DialogTransferFinalizeBinding? = null
    val binding get() = _binding!!

    var onFinalizeConfirmed: ((reservationId: String, meteredKwh: Double, notes: String?) -> Unit)? = null
    var onCancelClicked: (() -> Unit)? = null

    private var isConfirmed = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransferFinalizeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reservationId = arguments?.getString(ARG_RESERVATION_ID) ?: ""
        binding.tvFinalizeReservationId.text =
            getString(R.string.handshake_reservation_id, reservationId)

        binding.etMeteredKwh.doAfterTextChanged {
            binding.tilMeteredKwh.error = null
        }

        binding.btnConfirmFinalize.setOnClickListener {
            validateAndSubmit(reservationId)
        }

        binding.btnCancelFinalize.setOnClickListener {
            dismiss()
        }
    }

    /**
     * Validates input power reading defensively against 0.01 to 999.99 kWh constraints.
     *
     * @param reservationId The reservation being finalized.
     */
    private fun validateAndSubmit(reservationId: String) {
        val rawInput = binding.etMeteredKwh.text?.toString()?.trim().orEmpty()
        val parsedKwh = rawInput.toDoubleOrNull()

        if (parsedKwh == null || parsedKwh.isNaN() || parsedKwh < MIN_KWH || parsedKwh > MAX_KWH) {
            binding.tilMeteredKwh.error = getString(R.string.error_invalid_kwh_range)
            return
        }

        binding.tilMeteredKwh.error = null
        val notes = binding.etOperatorNotes.text?.toString()?.trim()?.ifBlank { null }

        isConfirmed = true
        onFinalizeConfirmed?.invoke(reservationId, parsedKwh, notes)
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isConfirmed) {
            onCancelClicked?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Nullify view binding to prevent view-hierarchy memory leaks (Rule 3)
        _binding = null
    }

    companion object {
        const val TAG = "TransferFinalizeDialog"
        private const val ARG_RESERVATION_ID = "arg_reservation_id"

        const val MIN_KWH = 0.01
        const val MAX_KWH = 999.99

        /**
         * Constructs an initialized TransferFinalizeDialog targeting a verified reservation.
         *
         * @param reservationId Verified reservation ID from QR server handshake.
         * @return Configured dialog instance.
         */
        fun newInstance(reservationId: String): TransferFinalizeDialog {
            return TransferFinalizeDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_RESERVATION_ID, reservationId)
                }
            }
        }
    }
}
