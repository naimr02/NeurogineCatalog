package com.neurogine.catalog.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neurogine.catalog.data.remote.ApiClient
import com.neurogine.catalog.data.repository.ProductRepository
import com.neurogine.catalog.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ProductListViewModel(
    private val repository: ProductRepository = ProductRepositoryImpl(ApiClient.api)
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        loadInitialProducts()
                    } else {
                        search(query.trim())
                    }
                }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
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

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = ProductListUiState.Loading

            val result = repository.searchProducts(query = query, limit = PAGE_SIZE, skip = 0)
            result.onSuccess { response ->
                if (response.products.isEmpty()) {
                    _uiState.value = ProductListUiState.Empty
                } else {
                    _uiState.value = ProductListUiState.Success(
                        products = response.products,
                        isPaginating = false,
                        endReached = true
                    )
                }
            }.onFailure { throwable ->
                _uiState.value = ProductListUiState.Error(
                    message = throwable.localizedMessage ?: "Failed to search products"
                )
            }
        }
    }

    fun retry() {
        val query = _searchQuery.value
        if (query.isBlank()) {
            loadInitialProducts()
        } else {
            search(query.trim())
        }
    }

    fun loadNextPage() {
        val currentState = _uiState.value
        if (currentState !is ProductListUiState.Success || currentState.isPaginating || currentState.endReached || _searchQuery.value.isNotBlank()) {
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
