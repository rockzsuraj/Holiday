package com.surajkumar.holiday


import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun HolidayApp(holidayViewModel: HolidayViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            MainScreen(
                holidays = holidayViewModel.sortedHolidays,
                onHolidayClick = { id -> navController.navigate(Routes.detail(id)) }
            )
        }
        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument(Routes.HOLIDAY_ID) { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong(Routes.HOLIDAY_ID)
            DetailScreen(
                holiday = id?.let { holidayViewModel.getHoliday(it) },
                onBack = { navController.popBackStack() }
            )
        }
    }
}