/**
 * Description: Material dialog component allowing evaluators to select and inject predefined
 * or custom QR payload scenarios into the operator verification flow (Rule 6.3 & FR-M4-05).
 */
package com.sliit.ssmts.operator_dashboard.ui.operator

import android.content.Context
import android.widget.EditText
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sliit.ssmts.R
import com.sliit.ssmts.operator_dashboard.util.FastTestQrScenarios

/**
 * Controller for constructing and presenting the Fast Test QR simulation selection dialog.
 */
object FastTestQrDialog {

    /**
     * Displays the scenario selection dialog to the user.
     *
     * @param context Host Android context.
     * @param onPayloadSelected Callback triggered when a test payload is chosen for injection.
     */
    fun show(
        context: Context,
        onPayloadSelected: (String) -> Unit
    ) {
        val scenarios = FastTestQrScenarios.getPredefinedScenarios()
        val items = scenarios.map { it.title }.toMutableList().apply {
            add(context.getString(R.string.scanner_fast_test_scenario_custom))
        }.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.scanner_fast_test_dialog_title))
            .setItems(items) { dialog, which ->
                if (which < scenarios.size) {
                    val selectedScenario = scenarios[which]
                    onPayloadSelected(selectedScenario.payload)
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                    showCustomPayloadInput(context, onPayloadSelected)
                }
            }
            .setNegativeButton(context.getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Prompts the user to manually enter an arbitrary QR payload for edge-case simulation.
     *
     * @param context Host Android context.
     * @param onPayloadSelected Callback triggered when the custom string is confirmed.
     */
    private fun showCustomPayloadInput(
        context: Context,
        onPayloadSelected: (String) -> Unit
    ) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.scanner_fast_test_custom_hint)
            setSingleLine(false)
            maxLines = 4
        }

        val container = FrameLayout(context).apply {
            val padding = context.resources.getDimensionPixelSize(R.dimen.spacing_lg)
            setPadding(padding, padding, padding, padding)
            addView(input)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.scanner_fast_test_scenario_custom))
            .setView(container)
            .setPositiveButton(context.getString(R.string.btn_inject_test_qr)) { dialog, _ ->
                val enteredText = input.text.toString().trim()
                if (enteredText.isNotEmpty()) {
                    onPayloadSelected(enteredText)
                }
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
