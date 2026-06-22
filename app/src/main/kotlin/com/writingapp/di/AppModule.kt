package com.writingapp.di

import com.writingapp.data.repository.AiRepositoryImpl
import com.writingapp.data.repository.DocumentRepositoryImpl
import com.writingapp.data.repository.RulebookRepositoryImpl
import com.writingapp.data.repository.WordlineRepositoryImpl
import com.writingapp.domain.repository.AiRepository
import com.writingapp.domain.repository.DocumentRepository
import com.writingapp.domain.repository.RulebookRepository
import com.writingapp.domain.repository.WordlineRepository
import com.writingapp.domain.usecase.*
import com.writingapp.ui.dashboard.DashboardViewModel
import com.writingapp.ui.editor.CopilotViewModel
import com.writingapp.ui.editor.EditorViewModel
import com.writingapp.ui.rulebook.RulebookViewModel
import com.writingapp.ui.search.SearchViewModel
import com.writingapp.ui.settings.SettingsViewModel
import com.writingapp.ui.wordline.WordlineViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val repositoryModule = module {
    single<DocumentRepository> { DocumentRepositoryImpl(get()) }
    single<RulebookRepository> { RulebookRepositoryImpl(get()) }
    single<WordlineRepository> { WordlineRepositoryImpl(get()) }
    single<AiRepository> { AiRepositoryImpl(get(), get(), get(), get()) }
}

val useCaseModule = module {
    factory { SaveDocumentUseCase(get()) }
    factory { DeleteDocumentUseCase(get()) }
    factory { SendAiMessageUseCase(get()) }
    factory { SaveRulebookUseCase(get()) }
    factory { SaveWordlineEventUseCase(get()) }
}

val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get(), get()) }
    viewModel { EditorViewModel(get(), get(), get()) }
    viewModel { RulebookViewModel(get(), get()) }
    viewModel { WordlineViewModel(get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { CopilotViewModel(get(), get()) }
}

val appModules = listOf(
    networkModule,
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
