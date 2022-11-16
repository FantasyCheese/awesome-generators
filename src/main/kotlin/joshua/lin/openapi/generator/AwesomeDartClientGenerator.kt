package joshua.lin.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import org.openapitools.codegen.CodegenConstants
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

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): Map<String, ModelsMap> {
        val allModels = super.postProcessAllModels(objs)
        allModels.map { it.value.models.first().model }.forEach { model ->
            model.vendorExtensions["CODE"] = model.code
        }
        return allModels
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

        return operationsMap
    }

    override fun toApiFilename(name: String?) = "rest_client"
}
