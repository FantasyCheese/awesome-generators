package joshua.lin.openapi.generator

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

val CodegenModel.genericTypes: List<String>
    get() {
        val genericModel = interfaceModels?.firstOrNull { it.genericSymbols.isNotEmpty() } ?: return emptyList()
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
