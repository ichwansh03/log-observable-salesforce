package com.observability.sfdc.util

import com.observability.sfdc.dto.ReportDescribeDto
import com.observability.sfdc.dto.ReportFilterDto
import com.observability.sfdc.dto.ReportSoqlDto
import org.springframework.stereotype.Service
import kotlin.collections.get

@Service
class ReportToSoqlConverter {

    private val operatorMap = mapOf(
        "equals"      to "=",
        "notEqual"    to "!=",
        "lessThan"    to "<",
        "greaterThan" to ">",
        "lessOrEqual" to "<=",
        "greaterOrEqual" to ">=",
        "contains"    to "LIKE",
        "notContain"  to "NOT LIKE",
        "startsWith"  to "LIKE"
    )

    fun convert(reportId: String, describe: ReportDescribeDto): ReportSoqlDto {
        val metadata = describe.reportMetadata
        val reportTypeMetadata = describe.reportTypeMetadata
        val reportName = metadata?.name
        val detailColumns = metadata?.detailColumns ?: emptyList()
        val filters = metadata?.reportFilters ?: emptyList()
        val booleanFilter = metadata?.reportBooleanFilter

        // Extract objects from reportTypeMetadata categories
        val objects = reportTypeMetadata?.categories
            ?.mapNotNull { it.name }
            ?.distinct()
            ?: emptyList()

        // Step 1: Detect root object from detailColumns
        val rootObject = detectRootObject(detailColumns)

        // Step 2: Clean columns — strip root prefix, skip BucketField_*
        val columns = detailColumns
            .filter { !it.startsWith("BucketField_") }
            .map { col ->
                if (rootObject.isNotEmpty() && col.startsWith("$rootObject.")) {
                    col.removePrefix("$rootObject.")
                } else {
                    col
                }
            }

        // Step 3: Build WHERE clause from filters + boolean logic
        val whereClause = buildWhereClause(filters, rootObject, booleanFilter)

        // Step 4: Assemble SOQL
        val selectClause = "SELECT ${columns.joinToString(", ")}"
        val fromClause = " FROM $rootObject"
        val soql = selectClause + fromClause + whereClause

        return ReportSoqlDto(
            reportId = reportId,
            reportName = reportName,
            rootObject = rootObject,
            soql = soql,
            filters = filters,
            reportType = metadata?.reportType,
            reportFormat = metadata?.reportFormat,
            objects = objects,
            describeUrl = "/services/data/v61.0/analytics/reports/$reportId/describe",
            reportUrl = "/lightning/r/Report/$reportId/view"
        )
    }

    private fun detectRootObject(detailColumns: List<String>): String {
        val objectCounts = mutableMapOf<String, Int>()

        for (col in detailColumns) {
            if (col.contains(".") && !col.startsWith("BucketField_")) {
                val objectName = col.substringBefore(".")
                objectCounts[objectName] = (objectCounts[objectName] ?: 0) + 1
            }
        }

        return objectCounts.maxByOrNull { it.value }?.key ?: ""
    }

    private fun buildWhereClause(filters: List<ReportFilterDto>, rootObject: String, booleanFilter: String?): String {
        if (filters.isEmpty()) return ""

        // Convert each filter to its SOQL condition string
        val filterConditions = filters.map { filter ->
            val filterCol = if (rootObject.isNotEmpty() && filter.column?.startsWith("$rootObject.") == true) {
                filter.column.removePrefix("$rootObject.")
            } else {
                filter.column ?: ""
            }

            val rawValue = filter.value
            val isMultiValue = rawValue != null && rawValue.split(",").size > 1

            val soqlOperator = if (isMultiValue) "" else convertOperator(filter.operator)
            val formattedValue = formatFilterValue(rawValue)

            "$filterCol $soqlOperator $formattedValue"
        }

        // If booleanFilter exists, replace 1-based indices with actual conditions
        // e.g. "(1 OR 2) AND 3" → "([condition1] OR [condition2]) AND [condition3]"
        val whereLogic = if (!booleanFilter.isNullOrBlank()) {
            var logic = booleanFilter
            for (i in filterConditions.indices) {
                val placeholder = (i + 1).toString()
                logic = logic?.replace(placeholder, "(${filterConditions[i]})")
            }
            logic
        } else {
            // Default: AND all filters
            filterConditions.joinToString(" AND ")
        }

        return " WHERE $whereLogic"
    }

    private fun formatFilterValue(value: String?): String {
        if (value == null) return "''"

        val parts = value.split(",")
        if (parts.size == 1) {
            // Handle startsWith operator — add wildcard
            return "'${value.trim()}'"
        }

        // Multi-value → IN (...)
        val quoted = parts.map { "'${it.trim()}'" }
        return "IN (${quoted.joinToString(", ")})"
    }

    private fun convertOperator(reportOp: String?): String {
        return operatorMap[reportOp] ?: "="
    }
}