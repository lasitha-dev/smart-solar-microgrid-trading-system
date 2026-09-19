/**
 * Description: BottomSheet dialog modal rendering the finalized energy transfer completion receipt,
 * metered kWh metrics, operator signature, and local cache synchronization confirmation (FR-M4-07.4).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.DialogTransferReceiptBinding
import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.ReservationStatus

/**
 * Presentation modal displaying animated transaction receipt upon successful energy transfer finalization.
 */
class TransferReceiptModal : BottomSheetDialogFragment() {

    private var _binding: DialogTransferReceiptBinding? = null
    val binding get() = _binding!!

    var onDoneClicked: (() -> Unit)? = null

    private var isActionHandled = false

    /**
     * Inflates the transfer receipt modal layout ViewBinding hierarchy.
     *
     * @param inflater The LayoutInflater object to inflate views.
     * @param container Optional parent container view.
     * @param savedInstanceState Previous saved state bundle if restoring.
     * @return The root View of the inflated layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransferReceiptBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Populates transfer completion receipt metrics, triggers entrance micro-animations, and sets click listeners.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState Previous saved state bundle if restoring.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reservationId = arguments?.getString(ARG_RESERVATION_ID) ?: ""
        val meteredKwh = arguments?.getDouble(ARG_METERED_KWH) ?: 0.0
        val operatorId = arguments?.getString(ARG_OPERATOR_ID) ?: ""
        val finalizedAt = arguments?.getString(ARG_FINALIZED_AT) ?: ""

        binding.tvReceiptDeliveredKwh.text =
            getString(R.string.receipt_metered_kwh_format, meteredKwh)
        binding.tvReceiptReservationId.text =
            getString(R.string.receipt_ref_format, reservationId)
        binding.tvReceiptOperator.text =
            getString(R.string.receipt_operator_format, operatorId)
        binding.tvReceiptTimestamp.text =
            getString(R.string.receipt_timestamp_format, finalizedAt)

        binding.badgeReceiptStatus.setStatus(ReservationStatus.COMPLETED)

        // Micro-animation: Pop-in scale and fade effect on the success checkmark
        animateSuccessIcon()

        binding.btnDoneReceipt.setOnClickListener {
            isActionHandled = true
            onDoneClicked?.invoke()
            dismiss()
        }
    }

    /**
     * Executes a subtle scale and fade entrance micro-animation on the success checkmark container.
     */
    private fun animateSuccessIcon() {
        binding.layoutReceiptIconContainer.alpha = 0f
        binding.layoutReceiptIconContainer.scaleX = 0.5f
        binding.layoutReceiptIconContainer.scaleY = 0.5f
        binding.layoutReceiptIconContainer.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .start()
    }

    /**
     * Handles modal dismissal and invokes completion callback if unhandled.
     *
     * @param dialog The dismissed dialog interface.
     */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isActionHandled) {
            onDoneClicked?.invoke()
        }
    }

    /**
     * Cleans up ViewBinding reference on view destruction to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Nullify view binding to prevent view-hierarchy memory leaks (Rule 3)
        _binding = null
    }

    companion object {
        const val TAG = "TransferReceiptModal"

        private const val ARG_RESERVATION_ID = "arg_reservation_id"
        private const val ARG_METERED_KWH = "arg_metered_kwh"
        private const val ARG_OPERATOR_ID = "arg_operator_id"
        private const val ARG_FINALIZED_AT = "arg_finalized_at"

        /**
         * Constructs a new TransferReceiptModal pre-populated with finalized transfer metrics.
         *
         * @param result Finalized energy transfer outcome domain entity.
         * @return Configured modal instance.
         */
        fun newInstance(result: FinalizeTransferResult): TransferReceiptModal {
            return TransferReceiptModal().apply {
                arguments = Bundle().apply {
                    putString(ARG_RESERVATION_ID, result.reservationId)
                    putDouble(ARG_METERED_KWH, result.meteredEnergyKwh)
                    putString(ARG_OPERATOR_ID, result.finalizedByOperator)
                    putString(ARG_FINALIZED_AT, result.finalizedAtIso)
                }
            }
        }
    }
}
