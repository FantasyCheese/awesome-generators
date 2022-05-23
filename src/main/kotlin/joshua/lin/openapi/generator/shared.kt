package joshua.lin.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema

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

fun <T> Schema<T>.onlyDescription() : Boolean {
    return description != null && listOf(
        title, multipleOf, maximum, exclusiveMaximum, minimum, exclusiveMinimum, maxLength, minLength,
        pattern, maxItems, minItems, uniqueItems, maxProperties, minProperties, required, type, not,
        properties, additionalProperties, format, `$ref`, nullable, readOnly, writeOnly, example,
        externalDocs, deprecated, xml, extensions, enum, discriminator, default
    ).all { it == null }
}