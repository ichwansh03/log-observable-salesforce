package com.observability.sfdc.service

import com.observability.sfdc.domain.User
import com.observability.sfdc.dto.SalesforceUserDto

interface UserService {
    fun getAllUsers(name: String? = null, limit: Int = 10, offset: Int = 0): List<SalesforceUserDto>
    fun searchUsers(name: String? = null, limit: Int = 10, offset: Int = 0): List<User>
}
