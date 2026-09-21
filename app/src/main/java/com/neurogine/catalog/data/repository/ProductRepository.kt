package com.neurogine.catalog.data.repository

import com.neurogine.catalog.data.model.Product
import com.neurogine.catalog.data.model.ProductResponse
import com.neurogine.catalog.data.remote.ApiClient
import com.neurogine.catalog.data.remote.DummyJsonApi

interface ProductRepository {
    suspend fun getProducts(limit: Int = 20, skip: Int = 0): Result<ProductResponse>
    suspend fun getProductById(id: Int): Result<Product>
    suspend fun searchProducts(query: String, limit: Int = 20, skip: Int = 0): Result<ProductResponse>
}

class ProductRepositoryImpl(
    private val api: DummyJsonApi = ApiClient.dummyJsonApi
) : ProductRepository {

    override suspend fun getProducts(limit: Int, skip: Int): Result<ProductResponse> = runCatching {
        api.getProducts(limit = limit, skip = skip)
    }

    override suspend fun getProductById(id: Int): Result<Product> = runCatching {
        api.getProductById(id = id)
    }

    override suspend fun searchProducts(query: String, limit: Int, skip: Int): Result<ProductResponse> = runCatching {
        api.searchProducts(query = query, limit = limit, skip = skip)
    }
}
