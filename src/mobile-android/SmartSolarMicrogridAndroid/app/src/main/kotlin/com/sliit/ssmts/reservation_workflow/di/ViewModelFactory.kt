/**
 * Student: Kumarasinghe S.S | IT22221414
 * Branch: feature/member-3-reservation-workflow
 * Component: Reservation Workflow (Member 3) - SE4040 EAD 2026
 * Description: Factory to construct ViewModels with the Repository dependency.
 */

package com.sliit.ssmts.reservation_workflow.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sliit.ssmts.reservation_workflow.domain.repository.IReservationRepository
import com.sliit.ssmts.reservation_workflow.ui.booking.ReservationViewModel
import com.sliit.ssmts.reservation_workflow.ui.manage.ManageReservationViewModel
import com.sliit.ssmts.reservation_workflow.ui.summary.BookingSummaryViewModel

class ViewModelFactory(
    private val repository: IReservationRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ReservationViewModel::class.java) -> {
                ReservationViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ManageReservationViewModel::class.java) -> {
                ManageReservationViewModel(repository) as T
            }
            modelClass.isAssignableFrom(BookingSummaryViewModel::class.java) -> {
                BookingSummaryViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
