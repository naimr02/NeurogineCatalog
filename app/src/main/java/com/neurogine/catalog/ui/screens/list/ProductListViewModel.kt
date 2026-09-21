package com.neurogine.catalog.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurogine.catalog.data.remote.ApiClient
import com.neurogine.catalog.data.repository.ProductRepository
import com.neurogine.catalog.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val repository: ProductRepository = ProductRepositoryImpl(ApiClient.api)
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    init {
        loadInitialProducts()
    }

    fun loadInitialProducts() {
        viewModelScope.launch {
            _uiState.value = ProductListUiState.Loading

            val result = repository.getProducts(limit = PAGE_SIZE, skip = 0)
            result.onSuccess { response ->
                if (response.products.isEmpty()) {
                    _uiState.value = ProductListUiState.Empty
                } else {
                    val endReached = response.products.size < PAGE_SIZE || response.products.size >= response.total
                    _uiState.value = ProductListUiState.Success(
                        products = response.products,
                        isPaginating = false,
                        endReached = endReached
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = ProductListUiState.Error(
                    message = throwable.localizedMessage ?: "Unknown error occurred"
                )
            }
        }
    }

    fun retry() {
        loadInitialProducts()
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState !is ProductListUiState.Success || currentState.isPaginating || currentState.endReached) {
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isPaginating = true)

            val result = repository.getProducts(limit = PAGE_SIZE, skip = currentState.products.size)
            result.onSuccess { response ->
                val updatedProducts = currentState.products + response.products
                val endReached = response.products.size < PAGE_SIZE || updatedProducts.size >= response.total
                _uiState.value = currentState.copy(
                    products = updatedProducts,
                    isPaginating = false,
                    endReached = endReached
                )
            }.onFailure {
                _uiState.value = currentState.copy(isPaginating = false)
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
