package com.surajkumar.holiday.domain.model

import java.time.LocalDate

data class Holiday (
    val id: Long = 0,
    val name: String,
    val date: LocalDate
)
