package com.neurogine.catalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neurogine.catalog.ui.screens.list.ProductListScreen
import com.neurogine.catalog.ui.screens.list.ProductListViewModel
import com.neurogine.catalog.ui.theme.NeurogineCatalogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeurogineCatalogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: ProductListViewModel = viewModel()
                    ProductListScreen(
                        viewModel = viewModel,
                        onProductClick = { /* Handled in future navigation step */ }
                    )
                }
            }
        }
    }
}