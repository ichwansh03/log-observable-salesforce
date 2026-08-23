package com.observability.sfdc.controller

import com.observability.sfdc.domain.User
import com.observability.sfdc.dto.SalesforceUserDto
import com.observability.sfdc.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sfdc/users")
@Tag(name = "Salesforce Users", description = "Endpoints for retrieving Salesforce user information")
class SalesforceUserController(
    private val userService: UserService
) {

    @GetMapping
    @Operation(summary = "Get Users from Salesforce", description = "Retrieves active users directly from Salesforce.")
    fun getUsers(
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<SalesforceUserDto> {
        val offset = page * size
        return userService.getAllUsers(limit = size, offset = offset)
    }

    @GetMapping("/db")
    @Operation(summary = "Search Users in Database", description = "Searches for users stored in the local database.")
    fun searchUsers(
        @RequestParam(required = false) name: String?,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(defaultValue = "0") page: Int
    ): List<User> {
        val offset = page * size
        return userService.searchUsers(name, size, offset)
    }
}
