package com.neurogine.catalog.ui.screens.list

import com.neurogine.catalog.data.model.Product

sealed interface ProductListUiState {
    data object Loading : ProductListUiState

    data class Success(
        val products: List<Product>,
        val isPaginating: Boolean = false,
        val endReached: Boolean = false
    ) : ProductListUiState

    data object Empty : ProductListUiState

    data class Error(
        val message: String
    ) : ProductListUiState
}
