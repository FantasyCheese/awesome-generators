package joshua.lin.openapi.generator

import com.google.common.base.CaseFormat
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.Schema
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenProperty
import org.openapitools.codegen.CodegenResponse
import org.openapitools.codegen.model.ModelMap

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

fun <T> Schema<T>.onlyDescription(): Boolean {
    return description != null && listOf(
        title, multipleOf, maximum, exclusiveMaximum, minimum, exclusiveMinimum, maxLength, minLength,
        pattern, maxItems, minItems, uniqueItems, maxProperties, minProperties, required, type, not,
        properties, additionalProperties, format, `$ref`, nullable, readOnly, writeOnly, example,
        externalDocs, deprecated, xml, extensions, enum, discriminator, default
    ).all { it == null }
}

fun setSuccessResponseModel(operations: List<CodegenOperation>, allModels: MutableList<ModelMap>?) {
    operations.forEach { op ->
        val resp = op.responses?.firstOrNull { it.is2xx } ?: return@forEach
        val model = allModels?.map { it.model }?.firstOrNull { it.classname == resp.dataType } ?: return@forEach
        op.vendorExtensions[SUCCESS_RESPONSE_MODEL] = model
    }
}

fun CodegenOperation.returnType(syntax: Pair<Char, Char> = '<' to '>', voidType: String = "void"): String {
    return vendorExtensions[SUCCESS_RESPONSE_MODEL]?.let { it as? CodegenModel }
        ?.takeIf { it.genericModel != null }
        ?.let {
            it.genericModel!!.classname + "${syntax.first}${it.genericTypes.joinToString(",")}${syntax.second}"
        } ?: returnType ?: voidType
}

infix fun Boolean.insert(string: () -> String) = if (this) string() else ""

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

val CodegenProperty.type: String
    get() = genericType ?: dataType

val CodegenModel.genericProperties
    get() = allVars.filter { it.genericType != null }

val CodegenModel.genericSymbols
    get() = genericProperties.map { it.genericType }

val CodegenModel.enumValues
    get() = allowableValues["enumVars"].let { it as List<Map<String, Any>> }

val CodegenOperation.response2XX get() = responses.firstOrNull { it.is2xx }

val CodegenOperation.response4XX get() = responses.firstOrNull { it.is4xx }

val CodegenResponse.isJson get() = content?.containsKey("application/json") == true

const val SUCCESS_RESPONSE_MODEL = "SUCCESS_RESPONSE_MODEL"
