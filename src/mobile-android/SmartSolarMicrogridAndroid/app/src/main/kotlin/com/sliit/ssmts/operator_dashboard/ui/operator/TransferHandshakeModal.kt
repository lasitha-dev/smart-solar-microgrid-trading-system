/**
 * Description: BottomSheet dialog modal displaying verified reservation metadata, prosumer NIC,
 * and bay assignment, prompting the operator to proceed to energy transfer (FR-M4-06.4).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.sliit.ssmts.operator_dashboard.R
import com.sliit.ssmts.operator_dashboard.databinding.DialogTransferHandshakeBinding
import com.sliit.ssmts.operator_dashboard.domain.model.QrVerificationResult

/**
 * Presentation modal displaying cryptographic verification handshake details to the Grid Operator.
 */
class TransferHandshakeModal : BottomSheetDialogFragment() {

    private var _binding: DialogTransferHandshakeBinding? = null
    val binding get() = _binding!!

    var onProceedClicked: (() -> Unit)? = null
    var onCancelClicked: (() -> Unit)? = null

    private var isActionHandled = false

    /**
     * Inflates the bottom sheet layout ViewBinding hierarchy.
     *
     * @param inflater The LayoutInflater used to inflate the modal.
     * @param container Optional parent container view.
     * @param savedInstanceState Saved bundle state if restoring.
     * @return The root View of the inflated layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTransferHandshakeBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Binds reservation arguments to modal UI controls and establishes button action handlers.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState Saved bundle state if restoring.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reservationId = arguments?.getString(ARG_RESERVATION_ID) ?: ""
        val prosumerNic = arguments?.getString(ARG_PROSUMER_NIC) ?: ""
        val stationName = arguments?.getString(ARG_STATION_NAME) ?: ""
        val allocatedBayId = arguments?.getString(ARG_ALLOCATED_BAY) ?: ""

        binding.tvHandshakeReservationId.text =
            getString(R.string.handshake_reservation_id, reservationId)
        binding.tvHandshakeProsumer.text =
            getString(R.string.handshake_prosumer, prosumerNic)
        binding.tvHandshakeStation.text =
            getString(R.string.handshake_station, stationName)
        binding.tvHandshakeBay.text =
            getString(R.string.handshake_bay, allocatedBayId)

        binding.btnProceedFinalize.setOnClickListener {
            isActionHandled = true
            onProceedClicked?.invoke()
            dismiss()
        }

        binding.btnCancelHandshake.setOnClickListener {
            isActionHandled = true
            onCancelClicked?.invoke()
            dismiss()
        }
    }

    /**
     * Handles modal dismissal and triggers cancel callback if unhandled.
     *
     * @param dialog The dismissed dialog interface.
     */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isActionHandled) {
            onCancelClicked?.invoke()
        }
    }

    /**
     * Nullifies ViewBinding reference to prevent view-hierarchy memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        // Nullify view binding to prevent view-hierarchy memory leaks (Rule 3)
        _binding = null
    }

    companion object {
        const val TAG = "TransferHandshakeModal"

        private const val ARG_RESERVATION_ID = "arg_reservation_id"
        private const val ARG_PROSUMER_NIC = "arg_prosumer_nic"
        private const val ARG_STATION_NAME = "arg_station_name"
        private const val ARG_ALLOCATED_BAY = "arg_allocated_bay"

        /**
         * Constructs a new TransferHandshakeModal configured with verified reservation details.
         *
         * @param result Verified reservation domain entity.
         * @return Initialized modal fragment instance.
         */
        fun newInstance(result: QrVerificationResult): TransferHandshakeModal {
            return TransferHandshakeModal().apply {
                arguments = Bundle().apply {
                    putString(ARG_RESERVATION_ID, result.reservationId ?: "")
                    putString(ARG_PROSUMER_NIC, result.prosumerNic ?: "")
                    putString(ARG_STATION_NAME, result.stationName ?: "")
                    putString(ARG_ALLOCATED_BAY, result.allocatedBayId ?: "")
                }
            }
        }
    }
}
