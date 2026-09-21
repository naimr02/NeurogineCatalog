package com.neurogine.catalog.ui

import com.neurogine.catalog.data.model.Product
import com.neurogine.catalog.data.model.ProductResponse
import com.neurogine.catalog.data.repository.ProductRepository
import com.neurogine.catalog.ui.screens.detail.ProductDetailUiState
import com.neurogine.catalog.ui.screens.detail.ProductDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val sampleProduct = Product(
        id = 10,
        title = "Test Phone",
        description = "A great smartphone",
        price = 699.99,
        rating = 4.8,
        thumbnail = "https://example.com/thumb.png",
        images = listOf("https://example.com/1.png", "https://example.com/2.png")
    )

    @Test
    fun loadProduct_success_emitsSuccessState() = runTest(testDispatcher) {
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
            override suspend fun getProductById(id: Int): Result<Product> = Result.success(sampleProduct)
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductDetailViewModel(productId = 10, repository = fakeRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Success)
        assertEquals(sampleProduct, (state as ProductDetailUiState.Success).product)
    }

    @Test
    fun loadProduct_failure_emitsErrorState() = runTest(testDispatcher) {
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(IOException("Server error"))
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductDetailViewModel(productId = 10, repository = fakeRepo)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Error)
        assertEquals("Server error", (state as ProductDetailUiState.Error).message)
    }

    @Test
    fun retry_reloadsProduct() = runTest(testDispatcher) {
        var shouldFail = true
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
            override suspend fun getProductById(id: Int): Result<Product> {
                return if (shouldFail) {
                    Result.failure(IOException("Timeout"))
                } else {
                    Result.success(sampleProduct)
                }
            }
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductDetailViewModel(productId = 10, repository = fakeRepo)

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ProductDetailUiState.Error)

        shouldFail = false
        viewModel.retry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductDetailUiState.Success)
        assertEquals(sampleProduct, (state as ProductDetailUiState.Success).product)
    }

    @Test
    fun provideFactory_createsViewModelInstance() {
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
            override suspend fun getProductById(id: Int): Result<Product> = Result.success(sampleProduct)
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val factory = ProductDetailViewModel.provideFactory(productId = 10, repository = fakeRepo)
        val createdVm = factory.create(ProductDetailViewModel::class.java)

        assertNotNull(createdVm)
    }
}
