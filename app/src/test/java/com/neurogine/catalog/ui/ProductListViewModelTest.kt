package com.neurogine.catalog.ui

import com.neurogine.catalog.data.FakeProductRepository
import com.neurogine.catalog.data.model.Product
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

    private fun createProduct(id: Int, title: String = "Product $id") = Product(
        id = id,
        title = title,
        description = "Description $id",
        price = 10.0 * id,
        rating = 4.5,
        thumbnail = "https://example.com/image$id.png"
    )

    @Test
    fun loadInitialProducts_emitsSuccess_whenRepositoryReturnsData() = runTest(testDispatcher) {
        val sampleProducts = (1..5).map { createProduct(it) }
        val repository = FakeProductRepository(initialProducts = sampleProducts)

        val viewModel = ProductListViewModel(repository = repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Success)
        val successState = state as ProductListUiState.Success
        assertEquals(5, successState.products.size)
        assertEquals(sampleProducts, successState.products)
        assertFalse(successState.isPaginating)
        assertTrue(successState.endReached)
    }

    @Test
    fun loadInitialProducts_emitsError_whenRepositoryThrowsException() = runTest(testDispatcher) {
        val repository = FakeProductRepository().apply {
            shouldReturnError = true
            errorMessage = "Network connection failed"
        }

        val viewModel = ProductListViewModel(repository = repository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Error)
        assertEquals("Network connection failed", (state as ProductListUiState.Error).message)
    }

    @Test
    fun search_emitsEmpty_whenRepositoryReturnsEmptyList() = runTest(testDispatcher) {
        val repository = FakeProductRepository(initialProducts = emptyList())

        val viewModel = ProductListViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.search("nonexistent")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Empty)
    }

    @Test
    fun loadNextPage_appendsProductsAndUpdatesState() = runTest(testDispatcher) {
        val sampleProducts = (1..25).map { createProduct(it) }
        val repository = FakeProductRepository(initialProducts = sampleProducts)

        val viewModel = ProductListViewModel(repository = repository)
        advanceUntilIdle()

        val state1 = viewModel.uiState.value as ProductListUiState.Success
        assertEquals(20, state1.products.size)
        assertFalse(state1.endReached)

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state2 = viewModel.uiState.value as ProductListUiState.Success
        assertEquals(25, state2.products.size)
        assertTrue(state2.endReached)
        assertFalse(state2.isPaginating)
    }

    @Test
    fun onSearchQueryChanged_debouncesAndSearches() = runTest(testDispatcher) {
        val sampleProducts = listOf(
            createProduct(1, "Apple iPhone"),
            createProduct(2, "Samsung Galaxy")
        )
        val repository = FakeProductRepository(initialProducts = sampleProducts)

        val viewModel = ProductListViewModel(repository = repository)
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("iPhone")
        advanceTimeBy(300L)
        // Before 400ms debounce completes, search hasn't triggered yet
        assertTrue(viewModel.uiState.value is ProductListUiState.Success)
        assertEquals(2, (viewModel.uiState.value as ProductListUiState.Success).products.size)

        advanceTimeBy(150L) // past 400ms debounce
        advanceUntilIdle()

        val searchState = viewModel.uiState.value
        assertTrue(searchState is ProductListUiState.Success)
        val filtered = (searchState as ProductListUiState.Success).products
        assertEquals(1, filtered.size)
        assertEquals("Apple iPhone", filtered[0].title)
    }

    @Test
    fun retry_reloadsDataAfterFailure() = runTest(testDispatcher) {
        val repository = FakeProductRepository(initialProducts = listOf(createProduct(1))).apply {
            shouldReturnError = true
        }

        val viewModel = ProductListViewModel(repository = repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ProductListUiState.Error)

        repository.shouldReturnError = false
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ProductListUiState.Success)
        assertEquals(1, (viewModel.uiState.value as ProductListUiState.Success).products.size)
    }
}
