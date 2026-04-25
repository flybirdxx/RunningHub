package com.runninghub.app.di

import com.runninghub.app.ui.feature.creator.CreatorProfileViewModel
import com.runninghub.app.ui.feature.detail.AppDetailViewModel
import com.runninghub.app.ui.feature.discovery.DiscoveryViewModel
import com.runninghub.app.ui.feature.profile.ProfileViewModel
import com.runninghub.app.ui.feature.search.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { DiscoveryViewModel(get()) }
    viewModel { AppDetailViewModel(get(), get(), get()) }
    viewModel { ProfileViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { CreatorProfileViewModel(get(), get()) }
}
