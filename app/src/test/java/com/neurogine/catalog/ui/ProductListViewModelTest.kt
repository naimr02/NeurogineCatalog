package com.neurogine.catalog.ui

import com.neurogine.catalog.data.model.Product
import com.neurogine.catalog.data.model.ProductResponse
import com.neurogine.catalog.data.repository.ProductRepository
import com.neurogine.catalog.ui.screens.list.ProductListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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

    private fun createProduct(id: Int) = Product(
        id = id,
        title = "Product $id",
        description = "Description $id",
        price = 10.0 * id,
        rating = 4.5,
        thumbnail = "https://example.com/image$id.png"
    )

    @Test
    fun loadInitialProducts_success_updatesProductsAndFlags() = runTest(testDispatcher) {
        val page1Products = (1..20).map { createProduct(it) }
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = page1Products, total = 50, skip = 0, limit = 20))

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        assertEquals(20, viewModel.products.value.size)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isPaginating.value)
        assertFalse(viewModel.endReached.value)
    }

    @Test
    fun loadInitialProducts_endReached_whenLessThanPageSize() = runTest(testDispatcher) {
        val products = (1..5).map { createProduct(it) }
        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> =
                Result.success(ProductResponse(products = products, total = 5, skip = 0, limit = 20))

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        assertEquals(5, viewModel.products.value.size)
        assertTrue(viewModel.endReached.value)
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

        assertEquals(20, viewModel.products.value.size)
        assertFalse(viewModel.endReached.value)

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(35, viewModel.products.value.size)
        assertTrue(viewModel.endReached.value)
        assertFalse(viewModel.isPaginating.value)
    }

    @Test
    fun loadNextPage_guarded_whenEndReached() = runTest(testDispatcher) {
        var callCount = 0
        val page1Products = (1..10).map { createProduct(it) }

        val fakeRepo = object : ProductRepository {
            override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> {
                callCount++
                return Result.success(ProductResponse(products = page1Products, total = 10, skip = 0, limit = 20))
            }

            override suspend fun getProductById(id: Int): Result<Product> = Result.failure(Exception())
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = Result.failure(Exception())
        }

        val viewModel = ProductListViewModel(repository = fakeRepo)
        advanceUntilIdle()

        assertEquals(1, callCount)
        assertTrue(viewModel.endReached.value)

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(1, callCount) // No second call
    }
}
