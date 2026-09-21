package com.neurogine.catalog.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurogine.catalog.data.model.Product
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

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isPaginating = MutableStateFlow(false)
    val isPaginating: StateFlow<Boolean> = _isPaginating.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadInitialProducts()
    }

    fun loadInitialProducts() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _endReached.value = false

            val result = repository.getProducts(limit = PAGE_SIZE, skip = 0)
            result.onSuccess { response ->
                _products.value = response.products
                _endReached.value = response.products.size < PAGE_SIZE || response.products.size >= response.total
            }.onFailure { throwable ->
                _errorMessage.value = throwable.localizedMessage ?: "Failed to load products"
            }

            _isLoading.value = false
        }
    }

    fun loadNextPage() {
        if (_isLoading.value || _isPaginating.value || _endReached.value) {
            return
        }

        viewModelScope.launch {
            _isPaginating.value = true
            val currentList = _products.value
            val result = repository.getProducts(limit = PAGE_SIZE, skip = currentList.size)

            result.onSuccess { response ->
                val updatedList = currentList + response.products
                _products.value = updatedList
                _endReached.value = response.products.size < PAGE_SIZE || updatedList.size >= response.total
            }.onFailure { throwable ->
                _errorMessage.value = throwable.localizedMessage ?: "Failed to load more products"
            }

            _isPaginating.value = false
        }
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
