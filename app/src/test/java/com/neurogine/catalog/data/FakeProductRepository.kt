package com.neurogine.catalog.data

import com.neurogine.catalog.data.model.Product
import com.neurogine.catalog.data.model.ProductResponse
import com.neurogine.catalog.data.repository.ProductRepository
import java.io.IOException

class FakeProductRepository(
    initialProducts: List<Product> = emptyList()
) : ProductRepository {

    val products = initialProducts.toMutableList()
    var shouldReturnError: Boolean = false
    var errorMessage: String = "Test error"

    override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> {
        if (shouldReturnError) {
            return Result.failure(IOException(errorMessage))
        }
        val paged = products.drop(skip).take(limit)
        return Result.success(
            ProductResponse(
                products = paged,
                total = products.size,
                skip = skip,
                limit = limit
            )
        )
    }

    override suspend fun getProductById(id: Int): Result<Product> {
        if (shouldReturnError) {
            return Result.failure(IOException(errorMessage))
        }
        val product = products.find { it.id == id }
        return if (product != null) {
            Result.success(product)
        } else {
            Result.failure(NoSuchElementException("Product with id $id not found"))
        }
    }

    override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> {
        if (shouldReturnError) {
            return Result.failure(IOException(errorMessage))
        }
        val filtered = products.filter { it.title.contains(query, ignoreCase = true) }
        val paged = filtered.drop(skip).take(limit)
        return Result.success(
            ProductResponse(
                products = paged,
                total = filtered.size,
                skip = skip,
                limit = limit
            )
        )
    }
}
