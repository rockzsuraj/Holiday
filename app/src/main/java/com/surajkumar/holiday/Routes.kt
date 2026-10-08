package com.surajkumar.holiday

object Routes {
    const val HOME = "home"
    const val HOLIDAY_ID = "holidayId"
    const val DETAIL = "detail/{$HOLIDAY_ID}"

    fun detail(id: Long) = "detail/$id"
}