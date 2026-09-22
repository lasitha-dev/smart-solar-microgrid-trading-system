/*
 * Student Name: SILVA M N U
 * Student ID: IT22169112
 * Module: SE4040 Enterprise Application Development (2026)
 * Component: Identity, Authentication & Account Lifecycle (Member 1)
 * Description: Factory class responsible for instantiating ViewModels with repository dependency injection.
 */

package com.sliit.ssmts.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sliit.ssmts.data.repository.AuthRepositoryImpl
import com.sliit.ssmts.domain.repository.IAuthRepository
import com.sliit.ssmts.ui.auth.AuthViewModel
import com.sliit.ssmts.ui.profile.ProfileViewModel

/**
 * ViewModelProvider.Factory implementation for creating ViewModels with repository dependency.
 */
class ViewModelFactory(
    private val repository: IAuthRepository
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(AuthRepositoryImpl(context.applicationContext))

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> {
                AuthViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
