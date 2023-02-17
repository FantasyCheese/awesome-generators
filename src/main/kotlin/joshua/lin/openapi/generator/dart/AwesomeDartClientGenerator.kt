package joshua.lin.openapi.generator.dart

import io.swagger.v3.oas.models.OpenAPI
import joshua.lin.openapi.generator.SUCCESS_RESPONSE_MODEL
import joshua.lin.openapi.generator.extractInlineEnum
import joshua.lin.openapi.generator.handleDescriptionByAllOf
import joshua.lin.openapi.generator.removeOperationTags
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.SupportingFile
import org.openapitools.codegen.languages.AbstractDartCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationsMap

class AwesomeDartClientGenerator : AbstractDartCodegen() {
    override fun getName() = "awesome-dart-client"

    init {
        modelDocTemplateFiles.clear()
        apiDocTemplateFiles.clear()
        modelTestTemplateFiles.clear()
        apiTestTemplateFiles.clear()

        additionalProperties[CodegenConstants.TEMPLATE_DIR] = name

        sourceFolder = ""

        typeMapping["file"] = "File"
        typeMapping["binary"] = "File"
        imports["File"] = "dart:io"
    }

    override fun processOpts() {
        super.processOpts()
        supportingFiles.addAll(
            listOf(
                SupportingFile("gitignore", ".gitignore"),
                SupportingFile("pubspec.mustache", "pubspec.yaml"),
                SupportingFile("init.sh", "init.sh"),
                SupportingFile("_index.mustache", "$libPath/$modelPackage/_index.dart"),
                SupportingFile("date_time.dart", "$libPath/$apiPackage/date_time.dart"),
            )
        )
    }

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        handleDescriptionByAllOf(openAPI)
        extractInlineEnum(openAPI)
        removeOperationTags(openAPI)
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): Map<String, ModelsMap> {
        var models = super.postProcessAllModels(objs)

        val discriminatorModels = models
            .map { it.value.models.first().model }
            .filter { it.hasDiscriminatorWithNonEmptyMapping }
            .flatMap { it.interfaceModels }.map { it.name }

        models = models.filterNot {
            val name = it.value.models.first().model.name
            name in discriminatorModels || name.endsWith("allOf")
        }

        models.map { it.value.models.first().model }.forEach { model ->
            model.vendorExtensions["CODE"] = model.code
        }

        return models
    }

    override fun postProcessOperationsWithModels(
        objs: OperationsMap?,
        allModels: MutableList<ModelMap>?
    ): OperationsMap {
        val operationsMap = super.postProcessOperationsWithModels(objs, allModels)
        val operations = operationsMap.operations.operation

        // handle null return type
        operations.filter { it.returnType == null }.forEach { it.returnType = "void" }

        operations.forEach { op ->
            val resp = op.responses?.firstOrNull { it.is2xx } ?: return@forEach
            val model = allModels?.map { it.model }?.firstOrNull { it.classname == resp.dataType } ?: return@forEach
            op.vendorExtensions[SUCCESS_RESPONSE_MODEL] = model
        }

        // TODO: support File return type
        operations.removeIf { it.returnTypeWithGeneric.matches(Regex("File")) }

        // TODO: support Map return type
        operations.removeIf { it.returnTypeWithGeneric.matches(Regex(".*Map<.+>.*")) }

        // set generated code to vendor extension
        operations.forEach {
            it.vendorExtensions["CODE"] = it.code
        }

        vendorExtensions["CLIENT_FACTORY"] = generateClientFactoryCode(operations)

        return operationsMap
    }

    override fun toApiFilename(name: String?) = "rest_client"

    override fun toEnumVarName(value: String, datatype: String?): String? = when {
        value.isEmpty() -> "empty"

        // Only rename numeric values when the datatype is numeric
        // AND the name is not changed by enum extensions (matches a numeric value).
        (isNumber(datatype) && value.matches(Regex("^-?\\d.*"))) ->
            toVarName("number${if (value.startsWith("-")) "_negative" else ""}${value}")

        else -> toVarName(value)
    }

    override fun toEnumValue(value: String?, datatype: String?): String? = when {
        isNumber(datatype) || "boolean".equals(datatype, ignoreCase = true) -> value
        else -> "\"${escapeText(value)}\""
    }

    private fun isNumber(type: String?): Boolean =
        listOf("num", "double", "int").any { it.equals(type, ignoreCase = true) }

    private fun generateClientFactoryCode(operations: List<CodegenOperation>): String {
        val globalParameters = mutableListOf<GlobalParameter>()
        operations.forEach { op ->
            op.allParams.filter { it.isGlobal }.forEach {
                val type = when {
                    it.isHeaderParam -> "headers"
                    it.isQueryParam -> "queryParameters"
                    else -> return@forEach
                }
                val name = it.paramName

                if (globalParameters.none { it.paramType == type && it.paramName == name }) {
                    globalParameters.add(GlobalParameter(type, name, mutableListOf()))
                }

                globalParameters.first { it.paramType == type && it.paramName == name }.operations.add(op)
            }
        }

        return if (globalParameters.isEmpty()) "" else """
            RestClient restClientWithGlobalParameter(
              Dio dio, {
              String? baseUrl,
              ${globalParameters.joinToString("\n") { "required Getter ${it.paramName}Getter," }}
            }) {
              dio.interceptors.add(GlobalParameterInterceptor(
                ${globalParameters.joinToString("\n") { "${it.paramName}Getter: ${it.paramName}Getter," }}
              ));
              return RestClient(dio, baseUrl: baseUrl);
            }

            class GlobalParameterInterceptor extends Interceptor {
              GlobalParameterInterceptor({
                ${globalParameters.joinToString("\n") { "required this.${it.paramName}Getter," }}
              });

              ${globalParameters.joinToString("\n") { "final Getter ${it.paramName}Getter;" }}
              
              @override
              void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
                ${globalParameters.joinToString("\n") { it.interceptorRequestCode }}
                handler.next(options);
              }
            }
            
            typedef Getter = dynamic Function();
        """.trimIndent()
    }
}

class GlobalParameter(val paramType: String, val paramName: String, val operations: MutableList<CodegenOperation>) {
    val interceptorRequestCode
        get() = """
        if ([
          ${operations.joinToString("\n") { "\"${it.httpMethod}:${it.path}\"," }}
        ].contains('${"$"}{options.method}:${"$"}{options.path}')) {
          options.${paramType}['${paramName}'] = ${paramName}Getter();
        }
    """.trimIndent()
}
