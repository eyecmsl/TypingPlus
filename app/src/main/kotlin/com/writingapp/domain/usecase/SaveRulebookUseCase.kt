package com.writingapp.domain.usecase

import com.writingapp.domain.model.Rulebook
import com.writingapp.domain.repository.RulebookRepository

class SaveRulebookUseCase(private val repository: RulebookRepository) {
    suspend operator fun invoke(rulebook: Rulebook): Long = repository.saveRule(rulebook)
}
