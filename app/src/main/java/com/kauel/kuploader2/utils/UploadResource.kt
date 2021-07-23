package com.kauel.kuploader2.utils

import com.kauel.kuploader2.api.ApiService
import com.kauel.kuploader2.api.responceApi.ResponseAPI
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * Get one inventory detail from Table find for inventoryId
 * @param inventoryID       idInventory to search into table InventoryDetail
 *
 * **/
//fun getInventoryDetail(listFile: ArrayList<File>, url: String, token: String): Flow<Resource<ResponseAPI>> {
//
//    return flow {
//
//        emit(Resource.Loading(null))
//
//        listFile.map {
//            when()
//        }
//        //Loop listFile
//        when (val inventoryDetailResponse = ApiService.uploadImage()) {
//            is Result.Success -> {
//
//                //Fetch Inventory detail data
//                val detailViewData = inventoryDetailMapper.executeMapping(inventoryDetailResponse.data)
//                //ID to request additional specifications
//                val networkID = detailViewData?.specificationsSectionViewData?.specificationCode.orEmpty()
//                coroutineScope {
//
//                    //Fetch data to complete inventory detail
//                    val deferredList = listOf(
//                        //fetch additional specifications section
//                        async { inventoryRemoteSource.getAdditionalSpecification(networkID) },
//                        //fetch products to complete performance section
//                        async { inventoryRemoteSource.getProductsInventory(inventoryID) },
//                        //fetch manage publishing section
//                        async { inventoryRemoteSource.getPublishing(inventoryID) }
//
//                    )
//                    val results = deferredList.awaitAll()
//
//                    when (val additionalSpecificationSection = results[0]) {
//                        is Result.Success -> {
//                            detailViewData?.additionalSpecificationSectionViewData =
//                                additionalSpecificationMapper.executeMapping(additionalSpecificationSection.data as AdditionalData)
//                        }
//                        is Result.Error -> {
//                            detailViewData?.additionalSpecificationSectionViewData = null
//                        }
//                    }
//                    when (val productSection = results[1]) {
//                        is Result.Success -> {
//                            val productList = productSection.data as List<ProductData>
//                            detailViewData?.performanceSectionViewData?.products = addStandardItem(productList.toMutableList())
//                        }
//                        is Result.Error -> {
//                        }
//                    }
//                    when (val publishingSection = results[2]) {
//                        is Result.Success -> {
//                            detailViewData?.managePublishingSectionViewData =
//                                managePublishingSectionDataMapper.executeMapping(publishingSection.data as List<PublishingDestinationData?>)
//
//                            detailViewData?.managePublishingSectionViewData?.saleStatus = detailViewData?.saleStatus
//                        }
//                        is Result.Error -> {
//                        }
//                    }
//                }
//                inventoryDetailDao.deleteAndCreate(detailViewData)
//            }
//
//            is Result.Error -> {
//                //emit(State.error<InventoryDetailViewData>(context?.getString(R.string.error_main_data).orEmpty()))
//            }
//
//        }
//
//        val source = inventoryDetailDao.getInventoryDetail(inventoryID).map {
//            if (it != null) {
//                State.success(it)
//            } else {
//                State.error<InventoryDetailViewData>(context?.getString(R.string.error_main_data).orEmpty())
//
//            }
//        }
//
//        emitAll(source)
//    }.flowOn(Dispatchers.IO)
//}