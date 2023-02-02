package joshua.lin.openapi.generator

import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenParameter

val CodegenOperation.code
    get() = """
            @${httpMethod}("$path") ${if (this.hasFormParams) "@MultiPart()" else ""}
            Future<${returnTypeWithGeneric}> ${operationId}(
              ${allParams.filterNot { it.isGlobal }.joinToString("\n") { it.code }}
            );
        """

private val CodegenParameter.code
    get() = "@${annotation}(${annotationParam}) ${dataTypeWithFileFix}${if (!required) "?" else ""} $paramName,"

private val CodegenParameter.annotation
    get() = when {
        isPathParam -> "Path"
        isQueryParam -> "Query"
        isHeaderParam -> "Header"
        isBodyParam -> "Body"
        isFormParam -> "Part"
        else -> throw IllegalArgumentException("Unsupported parameter: $this")
    }

private val CodegenParameter.annotationParam
    get() = when {
        isBodyParam -> ""
        isFormParam -> "name: \"${baseName}\""
        else -> "\"${baseName}\""
    }

private val CodegenParameter.dataTypeWithFileFix
    get() = if (dataType == "File") "List<MultipartFile>" else dataType

val CodegenOperation.returnTypeWithGeneric: String
    get() {
        val model = vendorExtensions[SUCCESS_RESPONSE_MODEL] as? CodegenModel ?: return returnType
        return if (model.genericTypes.isEmpty()) returnType else {
            model.dataType + "<${model.genericTypes.joinToString(",")}>"
        }
    }

val CodegenParameter.isGlobal
    get() = vendorExtensions.containsKey("x-global-parameter")

const val SUCCESS_RESPONSE_MODEL = "SUCCESS_RESPONSE_MODEL"
