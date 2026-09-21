package com.neurogine.catalog.ui

import com.neurogine.catalog.data.model.Product
import com.neurogine.catalog.data.model.ProductResponse
import com.neurogine.catalog.data.repository.ProductRepository
import com.neurogine.catalog.ui.screens.list.ProductListUiState
import com.neurogine.catalog.ui.screens.list.ProductListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createProduct(id: Int) = Product(
        id = id,
        title = "Product $id",
        description = "Description $id",
        price = 10.0 * id,
        rating = 4.5,
        thumbnail = "https://example.com/image$id.png"
    )

    @Test
    fun loadInitialProducts_success_emitsSuccessState() = runTest(testDispatcher) {
        val page1Products = (1..20).map { createProduct(it) }
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = page1Products, total = 50, skip = 0, limit = 20))

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Success)
        val successState = state as ProductListUiState.Success
        assertEquals(20, successState.products.size)
        assertFalse(successState.isPaginating)
        assertFalse(successState.endReached)
    }

    @Test
    fun loadInitialProducts_empty_emitsEmptyState() = runTest(testDispatcher) {
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = emptyList(), total = 0, skip = 0, limit = 20))

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ProductListUiState.Empty)
    }

    @Test
    fun loadInitialProducts_failure_emitsErrorState() = runTest(testDispatcher) {
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> =
                Result.failure(IOException("Network disconnected"))

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Error)
        assertEquals("Network disconnected", (state as ProductListUiState.Error).message)
    }

    @Test
    fun retry_reloadsInitialProducts() = runTest(testDispatcher) {
        var shouldFail = true
        val products = listOf(createProduct(1))
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> {
                return if (shouldFail) {
                    Result.failure(IOException("Server error"))
                } else {
                    Result.success(ProductResponse(products = products, total = 1, skip = 0, limit = 20))
                }
            }

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ProductListUiState.Error)

        shouldFail = false
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Success)
        assertEquals(1, (state as ProductListUiState.Success).products.size)
    }

    @Test
    fun loadNextPage_appendsProductsAndSetsEndReached() = runTest(testDispatcher) {
        val page1Products = (1..20).map { createProduct(it) }
        val page2Products = (21..35).map { createProduct(it) }

        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> {
                return if (skip == 0) {
                    Result.success(ProductResponse(products = page1Products, total = 35, skip = 0, limit = 20))
                } else {
                    Result.success(ProductResponse(products = page2Products, total = 35, skip = 20, limit = 20))
                }
            }

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        val state1 = viewModel.uiState.value as ProductListUiState.Success
        assertEquals(20, state1.products.size)
        assertFalse(state1.endReached)

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state2 = viewModel.uiState.value as ProductListUiState.Success
        assertEquals(35, state2.products.size)
        assertTrue(state2.endReached)
        assertFalse(state2.isPaginating)
    }

    @Test
    fun loadNextPage_failure_preservesExistingProducts() = runTest(testDispatcher) {
        val page1Products = (1..20).map { createProduct(it) }

        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> {
                return if (skip == 0) {
                    Result.success(ProductResponse(products = page1Products, total = 50, skip = 0, limit = 20))
                } else {
                    Result.failure(IOException("Pagination failed"))
                }
            }

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProductListUiState.Success
        assertEquals(20, state.products.size)
        assertFalse(state.isPaginating)
    }

    @Test
    fun onSearchQueryChanged_debouncedSearch_triggersSearchOnQuery() = runTest(testDispatcher) {
        val searchResults = listOf(createProduct(42))
        var searchedQuery: String? = null

        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = emptyList(), total = 0, skip = 0, limit = 20))

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())

            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> {
                searchedQuery = query
                return Result.success(ProductResponse(products = searchResults, total = 1, skip = 0, limit = 20))
            }
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("phone")
        advanceTimeBy(300L)
        // Not triggered yet at 300ms (debounce is 400ms)
        assertEquals(null, searchedQuery)

        advanceTimeBy(150L) // Now at 450ms
        advanceUntilIdle()

        assertEquals("phone", searchedQuery)
        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Success)
        val successState = state as ProductListUiState.Success
        assertEquals(1, successState.products.size)
        assertEquals(42, successState.products[0].id)
        assertTrue(successState.endReached)
    }

    @Test
    fun onSearchQueryChanged_emptyQuery_loadsInitialProducts() = runTest(testDispatcher) {
        var catalogLoaded = false
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> {
                catalogLoaded = true
                return Result.success(ProductResponse(products = listOf(createProduct(1)), total = 1, skip = 0, limit = 20))
            }

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())

            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = listOf(createProduct(99)), total = 1, skip = 0, limit = 20))
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("laptop")
        advanceTimeBy(500L)
        advanceUntilIdle()

        catalogLoaded = false
        viewModel.onSearchQueryChanged("")
        advanceTimeBy(500L)
        advanceUntilIdle()

        assertTrue(catalogLoaded)
    }

    @Test
    fun search_emptyResults_emitsEmptyState() = runTest(testDispatcher) {
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = emptyList(), total = 0, skip = 0, limit = 20))

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())

            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = emptyList(), total = 0, skip = 0, limit = 20))
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        viewModel.search("nonexistent_item")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ProductListUiState.Empty)
    }
}
