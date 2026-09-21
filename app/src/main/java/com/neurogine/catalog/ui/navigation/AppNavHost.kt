package com.neurogine.catalog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neurogine.catalog.ui.screens.detail.ProductDetailScreen
import com.neurogine.catalog.ui.screens.detail.ProductDetailViewModel
import com.neurogine.catalog.ui.screens.list.ProductListScreen
import com.neurogine.catalog.ui.screens.list.ProductListViewModel

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = "list"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = "list") {
            val viewModel: ProductListViewModel = viewModel()
            ProductListScreen(
                viewModel = viewModel,
                onProductClick = { productId ->
                    navController.navigate("detail/$productId")
                }
            )
        }

        composable(
            route = "detail/{productId}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.IntType
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId") ?: return@composable
            val viewModel: ProductDetailViewModel = viewModel(
                factory = ProductDetailViewModel.provideFactory(productId)
            )
            ProductDetailScreen(
                viewModel = viewModel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
