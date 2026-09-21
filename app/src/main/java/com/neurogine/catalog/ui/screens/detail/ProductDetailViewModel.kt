package com.neurogine.catalog.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neurogine.catalog.data.model.Product
import com.neurogine.catalog.data.remote.ApiClient
import com.neurogine.catalog.data.repository.ProductRepository
import com.neurogine.catalog.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Success(val product: Product) : ProductDetailUiState
    data class Error(val message: String) : ProductDetailUiState
}

class ProductDetailViewModel(
    private val productId: Int,
    private val repository: ProductRepository = ProductRepositoryImpl(ApiClient.api)
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadProduct()
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.value = ProductDetailUiState.Loading
            val result = repository.getProductById(productId)
            result.onSuccess { product ->
                _uiState.value = ProductDetailUiState.Success(product)
            }.onFailure { throwable ->
                _uiState.value = ProductDetailUiState.Error(
                    throwable.localizedMessage ?: "Failed to load product details"
                )
            }
        }
    }

    fun retry() {
        loadProduct()
    }

    companion object {
        fun provideFactory(
            productId: Int,
            repository: ProductRepository = ProductRepositoryImpl(ApiClient.api)
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProductDetailViewModel(productId, repository) as T
            }
        }
    }
}
