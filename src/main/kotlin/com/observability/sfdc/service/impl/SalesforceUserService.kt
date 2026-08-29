package com.observability.sfdc.service.impl

import com.observability.sfdc.domain.User
import com.observability.sfdc.dto.SalesforceQueryResult
import com.observability.sfdc.dto.SalesforceUserDto
import com.observability.sfdc.repository.UserRepository
import com.observability.sfdc.service.SalesforceBaseService
import com.observability.sfdc.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SalesforceUserService(
    authService: SalesforceAuthService,
    private val userRepository: UserRepository,
    @Value($$"${salesforce.api-version}") apiVersion: String
) : SalesforceBaseService(authService, apiVersion), UserService {

    @Cacheable(value = ["sf_users"], key = "'all_users_' + (#name ?: 'all') + '_' + #limit + '_' + #offset", unless = "#result == null")
    @Transactional
    override fun getAllUsers(name: String?, limit: Int, offset: Int): List<SalesforceUserDto> {
        var query = "SELECT Id, Name, Username, Email, Profile.Name, IsActive, Entity__c FROM User WHERE IsActive = TRUE "
        if (!name.isNullOrBlank()) {
            val escapedName = name.replace("'", "\\'")
            query += "AND (Name LIKE '%$escapedName%' OR Name = 'Automated Process') "
        }
        query += "ORDER BY Name ASC LIMIT $limit OFFSET $offset"

        val records = querySalesforce("querying Salesforce Users", query, object : ParameterizedTypeReference<SalesforceQueryResult<SalesforceUserDto>>() {}, useTooling = false)
        if (records.isNotEmpty()) syncUsersToDatabase(records)
        return records
    }

    override fun searchUsers(name: String?, limit: Int, offset: Int): List<User> {
        val pageable = PageRequest.of(offset / limit, limit, Sort.by("name").ascending())
        if (!name.isNullOrBlank()) getAllUsers(name = name, limit = 200)
        else if (userRepository.count() == 0L) getAllUsers(limit = 200)
        
        return if (name.isNullOrBlank()) userRepository.findAllProjectedBy(pageable)
               else userRepository.findByNameContainingIgnoreCase(name, pageable)
    }

    @Transactional
    private fun syncUsersToDatabase(dtos: List<SalesforceUserDto>) = dtos.forEach { dto ->
        val entity = userRepository.findBySfdcId(dto.id).orElse(User(sfdcId = dto.id, name = dto.name, username = dto.username, email = dto.email, profileName = dto.profile?.name, isActive = dto.isActive, entity = dto.entity))
        userRepository.save(entity.copy(name = dto.name, username = dto.username, email = dto.email, profileName = dto.profile?.name, isActive = dto.isActive, entity = dto.entity))
    }
}
