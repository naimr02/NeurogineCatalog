package com.neurogine.catalog.data

import com.neurogine.catalog.data.model.Product
import com.neurogine.catalog.data.model.ProductResponse
import com.neurogine.catalog.data.remote.DummyJsonApi
import com.neurogine.catalog.data.repository.ProductRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ProductRepositoryTest {

    private val sampleProduct = Product(
        id = 1,
        title = "Essence Mascara Lash Princess",
        description = "The Essence Mascara Lash Princess is a popular mascara.",
        price = 9.99,
        rating = 4.94,
        thumbnail = "https://cdn.dummyjson.com/products/images/beauty/Essence%20Mascara%20Lash%20Princess/thumbnail.png",
        images = listOf("https://cdn.dummyjson.com/products/images/beauty/Essence%20Mascara%20Lash%20Princess/1.png")
    )

    private val sampleResponse = ProductResponse(
        products = listOf(sampleProduct),
        total = 1,
        skip = 0,
        limit = 20
    )

    @Test
    fun getProducts_success_returnsResultSuccess() = runBlocking {
        val fakeApi = object : DummyJsonApi {
            override suspend fun getProducts(limit: Int, skip: Int): ProductResponse = sampleResponse
            override suspend fun getProductById(id: Int): Product = sampleProduct
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): ProductResponse = sampleResponse
        }

        val repository = ProductRepositoryImpl(api = fakeApi)
        val result = repository.getProducts(limit = 20, skip = 0)

        assertTrue(result.isSuccess)
        assertEquals(sampleResponse, result.getOrNull())
    }

    @Test
    fun getProducts_failure_returnsResultFailure() = runBlocking {
        val fakeApi = object : DummyJsonApi {
            override suspend fun getProducts(limit: Int, skip: Int): ProductResponse =
                throw IOException("Network error")
            override suspend fun getProductById(id: Int): Product =
                throw IOException("Network error")
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): ProductResponse =
                throw IOException("Network error")
        }

        val repository = ProductRepositoryImpl(api = fakeApi)
        val result = repository.getProducts(limit = 20, skip = 0)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }

    @Test
    fun getProductById_success_returnsProduct() = runBlocking {
        val fakeApi = object : DummyJsonApi {
            override suspend fun getProducts(limit: Int, skip: Int): ProductResponse = sampleResponse
            override suspend fun getProductById(id: Int): Product = sampleProduct
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): ProductResponse = sampleResponse
        }

        val repository = ProductRepositoryImpl(api = fakeApi)
        val result = repository.getProductById(1)

        assertTrue(result.isSuccess)
        assertEquals(sampleProduct, result.getOrNull())
    }

    @Test
    fun searchProducts_success_returnsMatchingProducts() = runBlocking {
        val fakeApi = object : DummyJsonApi {
            override suspend fun getProducts(limit: Int, skip: Int): ProductResponse = sampleResponse
            override suspend fun getProductById(id: Int): Product = sampleProduct
            override suspend fun searchProducts(query: String, limit: Int, skip: Int): ProductResponse = sampleResponse
        }

        val repository = ProductRepositoryImpl(api = fakeApi)
        val result = repository.searchProducts("mascara", 20, 0)

        assertTrue(result.isSuccess)
        assertEquals(sampleResponse, result.getOrNull())
    }
}
