package joshua.lin.openapi.generator

import com.google.common.base.CaseFormat
import io.swagger.v3.oas.models.OpenAPI
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.CodegenModel
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

        typeMapping["file"] = "File";
        typeMapping["binary"] = "File";
        imports["File"] = "dart:io"
    }

    override fun processOpts() {
        super.processOpts()
        supportingFiles.apply {
            add(SupportingFile("pubspec.mustache", "pubspec.yaml"))
        }
    }

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        handleDescriptionByAllOf(openAPI)
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>?) =
        super.postProcessAllModels(objs).mapValues {
            it.value.models[0].model.apply {
                vendorExtensions["code"] = code
            }
            return@mapValues it.value
        }.toMutableMap()

    override fun postProcessOperationsWithModels(
        objs: OperationsMap?, allModels: MutableList<ModelMap>?
    ): OperationsMap {
        return super.postProcessOperationsWithModels(objs, allModels)
    }

    private val CodegenModel.code
        get() = if (isEnum) """
            import 'package:freezed_annotation/freezed_annotation.dart';
            enum $classname { 
              ${enumValuesCode(allowableValues)}
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
    
              ${
                  if (allVars.any { it.isBinary }) "" 
                  else "factory ${classname}.fromJson(final Map<String, Object?> json) => _\$${classname}FromJson(json);"
              }
            }
        """

    private val CodegenProperty.code
        get() =
            if (required) "required $dataType ${name},"
            else "${dataType}? ${name},"

    private fun importStatement(classname: String) =
        imports[classname]?.let { "import '$it';" } ?: classname
            .let { CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it) }
            .let { "import 'package:${pubName}/${modelPackage}/${it}.dart';" }

    private fun enumValuesCode(allowableValues: Map<String, Any>) =
        allowableValues["enumVars"].let { it as List<Map<String, Any>> }.joinToString("\n") {
            "@JsonValue(${it["value"]}) ${it["name"]},"
        }
}
