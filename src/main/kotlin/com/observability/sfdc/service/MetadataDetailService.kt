package com.observability.sfdc.service

import com.observability.sfdc.dto.MetadataDetailDto

interface MetadataDetailService {
    fun getMetadataDetail(id: String, type: String): MetadataDetailDto?
}
