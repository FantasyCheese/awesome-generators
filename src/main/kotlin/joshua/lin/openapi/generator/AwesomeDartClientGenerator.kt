package joshua.lin.openapi.generator

import com.google.common.base.CaseFormat
import io.swagger.v3.oas.models.OpenAPI
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenParameter
import org.openapitools.codegen.CodegenProperty
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
                SupportingFile("pubspec.mustache", "pubspec.yaml"),
                SupportingFile("_index.mustache", "$libPath/$modelPackage/_index.dart"),
                SupportingFile("date_time.dart", "$libPath/$apiPackage/date_time.dart"),
            )
        )
    }

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        handleDescriptionByAllOf(openAPI)
        removeOperationTags(openAPI)
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>) =
        super.postProcessAllModels(objs)
            .mapValues(::addCodeInVendorExtension)

    private fun addCodeInVendorExtension(modelsMapEntry: Map.Entry<String, ModelsMap>): ModelsMap {
        modelsMapEntry.value.models[0].model.apply { vendorExtensions["code"] = code }
        return modelsMapEntry.value
    }

    override fun postProcessOperationsWithModels(
        objs: OperationsMap?,
        allModels: MutableList<ModelMap>?
    ): OperationsMap {
        val operationsMap = super.postProcessOperationsWithModels(objs, allModels)
        val operations = operationsMap.operations.operation

        // TODO: support File return type
        operations.removeIf { it.returnType == "File" }

        // handle null return type
        operations.forEach {
            if (it.returnType == null) it.returnType = "void"
            it.vendorExtensions["code"] = it.code
        }

        return operationsMap
    }

    private val CodegenModel.code
        get() = if (isEnum) """
            class $classname {
              const $classname._(this.value);
              final $dataType value;
              factory $classname.fromJson($dataType value) => $classname._(value);
              $dataType toJson() => value;
            
              ${enumValuesCode(classname, allowableValues)}
            }
        """ else """
            import 'package:flutter/foundation.dart';
            import 'package:freezed_annotation/freezed_annotation.dart';
            ${imports.joinToString("\n", transform = ::importStatement)}
    
            part '${classFilename}.freezed.dart';
            ${if (allVars.any { it.isBinary }) "" else "part '${classFilename}.g.dart';"}
            
            @freezed
            class $classname with _$${classname} {
              const factory ${classname}({
                 ${allVars.joinToString("\n") { it.code }}
              }) = _${classname};
    
              ${if (allVars.any { it.isBinary }) "" else "factory ${classname}.fromJson(final Map<String, Object?> json) => _\$${classname}FromJson(json);"}
            }
        """

    private val CodegenProperty.code
        get() =
            if (required) "required $dataType ${name},"
            else "${dataType}? ${name},"

    private fun importStatement(classname: String) = imports[classname]?.let { "import '$it';" }
        ?: classname.let { "import '${CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it)}.dart';" }

    private fun enumValuesCode(classname: String, allowableValues: Map<String, Any>) =
        allowableValues["enumVars"].let { it as List<Map<String, Any>> }.joinToString("\n") {
            "static const ${it["name"]} = ${classname}._(${it["value"]});"
        }

    private val CodegenOperation.code
        get() = """
            @${httpMethod}("$path")
            Future<${returnType}> ${operationId}(
              ${allParams.joinToString("\n") { it.code }}
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
            isFormParam -> "name: \"${paramName}\""
            else -> "\"${paramName}\""
        }

    private val CodegenParameter.dataTypeWithFileFix
        get() = if (dataType == "File") "List<File>" else dataType
}
