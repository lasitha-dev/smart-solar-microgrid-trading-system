/**
 * Description: Modular UI coordinator orchestrating the presentation, callback dispatching,
 * and lifecycle detachment of operator verification modals and simulation dialogs (Rule 3 SRP delegate).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import androidx.fragment.app.FragmentManager
import com.sliit.ssmts.operator_dashboard.domain.model.FinalizeTransferResult
import com.sliit.ssmts.operator_dashboard.domain.model.QrVerificationResult

/**
 * Encapsulates modal dialogue instantiation and fragment manager presentation logic for the operator scanner screen.
 *
 * @property context Host Android context for Alert Dialog builders.
 * @property fragmentManager FragmentManager for presenting Bottom Sheet and DialogFragment components.
 */
class ScannerModalCoordinator(
    private val context: Context,
    private val fragmentManager: FragmentManager
) {

    /**
     * Displays the Viva simulation Fast Test QR scenario selection dialog (Rule 6.3).
     *
     * @param onPayloadSelected Callback triggered when a test scenario or custom QR string is selected.
     */
    fun showFastTestDialog(onPayloadSelected: (String) -> Unit) {
        FastTestQrDialog.show(context, onPayloadSelected)
    }

    /**
     * Instantiates and presents the QR handshake modal showing verified prosumer and bay information.
     *
     * @param reservation Cryptographically verified reservation details.
     * @param onCancel Callback triggered when the modal is dismissed or cancelled without proceeding.
     * @param onProceed Callback triggered when the operator elects to advance to energy finalization.
     */
    fun showHandshakeModal(
        reservation: QrVerificationResult,
        onCancel: () -> Unit,
        onProceed: (String) -> Unit
    ) {
        val modal = TransferHandshakeModal.newInstance(reservation).apply {
            onCancelClicked = onCancel
            onProceedClicked = {
                onProceed(reservation.reservationId ?: "")
            }
        }
        modal.show(fragmentManager, TransferHandshakeModal.TAG)
    }

    /**
     * Instantiates and presents the energy metering dialog for numeric kWh input and operational notes.
     *
     * @param reservationId Target reservation identifier to finalize.
     * @param onCancel Callback triggered if the operator dismisses or cancels the dialog.
     * @param onFinalizeConfirmed Callback triggered with validated metered kWh and optional notes.
     */
    fun showFinalizeDialog(
        reservationId: String,
        onCancel: () -> Unit,
        onFinalizeConfirmed: (reservationId: String, meteredKwh: Double, notes: String?) -> Unit
    ) {
        val dialog = TransferFinalizeDialog.newInstance(reservationId).apply {
            this.onCancelClicked = onCancel
            this.onFinalizeConfirmed = onFinalizeConfirmed
        }
        dialog.show(fragmentManager, TransferFinalizeDialog.TAG)
    }

    /**
     * Instantiates and presents the transfer completion receipt modal with finalized transfer metrics.
     *
     * @param receipt Finalized energy transfer receipt returned by the central Web API.
     * @param onDone Callback triggered when the operator acknowledges the receipt and finishes the session.
     */
    fun showReceiptModal(
        receipt: FinalizeTransferResult,
        onDone: () -> Unit
    ) {
        val modal = TransferReceiptModal.newInstance(receipt).apply {
            this.onDoneClicked = onDone
        }
        modal.show(fragmentManager, TransferReceiptModal.TAG)
    }

    /**
     * Displays a themed rejection dialog explaining the cryptographic or business conflict that halted verification.
     *
     * @param errorCode Machine-readable error code (e.g., ERR_QR_COMPLETED, ERR_QR_SIGNATURE_MISMATCH).
     * @param message Human-readable descriptive explanation from central API or client parser.
     * @param onDismiss Callback invoked when the operator acknowledges and dismisses the rejection.
     */
    fun showRejectionDialog(
        errorCode: String?,
        message: String,
        onDismiss: () -> Unit
    ) {
        ScannerRejectionDialog.show(context, errorCode, message, onDismiss)
    }

    /**
     * Dispatches the ScannerUiState to the appropriate modal, dialog, or rejection presentation.
     *
     * @param state Active scanner state emitted by ViewModel.
     * @param onReset Callback to reset scanner state upon dismissal or completion.
     * @param onFinalize Callback to finalize energy transfer with validated kWh and notes.
     */
    fun dispatchState(
        state: ScannerUiState,
        onReset: () -> Unit,
        onFinalize: (reservationId: String, meteredKwh: Double, notes: String?) -> Unit
    ) {
        when (state) {
            is ScannerUiState.Handshake -> {
                showHandshakeModal(
                    reservation = state.reservation,
                    onCancel = onReset,
                    onProceed = { resId ->
                        showFinalizeDialog(
                            reservationId = resId,
                            onCancel = onReset,
                            onFinalizeConfirmed = onFinalize
                        )
                    }
                )
            }
            is ScannerUiState.Rejection -> {
                showRejectionDialog(state.errorCode, state.message, onReset)
            }
            is ScannerUiState.Finalized -> {
                showReceiptModal(state.receipt, onReset)
            }
            is ScannerUiState.Idle,
            is ScannerUiState.Verifying,
            is ScannerUiState.Finalizing -> {
                // Handled by loading overlays in host activity
            }
        }
    }
}
