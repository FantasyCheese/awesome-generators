package joshua.lin.openapi.generator

import com.google.common.base.CaseFormat
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenProperty

fun handleDescriptionByAllOf(openAPI: OpenAPI) {
    openAPI.components.schemas.map { it.value.properties ?: mutableMapOf() }.forEach { props ->
        props.keys.forEach { key ->
            val prop = props[key]
            if (prop is ComposedSchema && prop.allOf?.size == 2 && prop.allOf.any { it.onlyDescription() }) {
                val idx = prop.allOf.indexOfFirst { !it.onlyDescription() }
                if (idx != -1) props[key] = prop.allOf[idx]
            }
        }
    }
}

fun extractInlineEnum(openAPI: OpenAPI) {
    val extractedSchemas = mutableMapOf<String, Schema<Any>>()
    openAPI.components.schemas.forEach { (schemaName, schema) ->
        schema.properties?.filterNot { it.value.enum.isNullOrEmpty() }?.forEach { (propName, prop) ->
            val enumName = schemaName + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, propName)
            schema.properties[propName] = Schema<Any>().apply { `$ref` = "#/components/schemas/$enumName" }
            extractedSchemas[enumName] = prop
        }
    }
    openAPI.components.schemas.putAll(extractedSchemas)
}

fun removeOperationTags(openAPI: OpenAPI) {
    openAPI.paths.values.flatMap { it.readOperations() }.forEach { it.tags?.clear() }
}

fun <T> Schema<T>.onlyDescription() : Boolean {
    return description != null && listOf(
        title, multipleOf, maximum, exclusiveMaximum, minimum, exclusiveMinimum, maxLength, minLength,
        pattern, maxItems, minItems, uniqueItems, maxProperties, minProperties, required, type, not,
        properties, additionalProperties, format, `$ref`, nullable, readOnly, writeOnly, example,
        externalDocs, deprecated, xml, extensions, enum, discriminator, default
    ).all { it == null }
}

val CodegenModel.genericModel: CodegenModel?
    get() = interfaceModels?.firstOrNull { it.genericSymbols.isNotEmpty() }

val CodegenModel.genericTypes: List<String>
    get() {
        val genericModel = genericModel ?: return emptyList()
        val properties = interfaceModels.filterNot { it == genericModel }.flatMap { it.allVars }
        return genericModel.genericProperties.map { genericProp ->
            try {
                properties.first { it.name == genericProp.name }.dataType
            } catch (e: Exception) {
                return listOf()
            }
        }
    }

val CodegenProperty.genericType
    get() = vendorExtensions?.get("x-generic-type") as? String
val CodegenModel.genericProperties
    get() = allVars.filter { it.genericType != null }

val CodegenModel.genericSymbols
    get() = genericProperties.map { it.genericType }

const val SUCCESS_RESPONSE_MODEL = "SUCCESS_RESPONSE_MODEL"
