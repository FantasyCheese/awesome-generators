package joshua.lin.openapi.generator.go

import io.swagger.v3.oas.models.OpenAPI
import joshua.lin.openapi.generator.extractInlineEnum
import joshua.lin.openapi.generator.handleDescriptionByAllOf
import joshua.lin.openapi.generator.removeOperationTags
import joshua.lin.openapi.generator.setSuccessResponseModel
import org.openapitools.codegen.CodegenConfig
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.SupportingFile
import org.openapitools.codegen.languages.AbstractGoCodegen
import org.openapitools.codegen.languages.GoServerCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationsMap
import org.slf4j.LoggerFactory

class AwesomeGoServerGenerator : GoServerCodegen(), CodegenConfig {
    override fun getName() = "awesome-go-server"

    init {
        additionalProperties[CodegenConstants.TEMPLATE_DIR] = "awesome-go-server"
        typeMapping.let {
            it["File"] = "*multipart.FileHeader"
            it["file"] = "*multipart.FileHeader"
            it["binary"] = "*multipart.FileHeader"
        }
    }

    override fun processOpts() {
        super.processOpts()
        apiTemplateFiles.remove("service.mustache")
        supportingFiles.removeIf {
            it.templateFile in listOf(
                "openapi.mustache",
                "Dockerfile.mustache",
                "README.mustache",
                "routers.mustache",
                "logger.mustache",
                "impl.mustache",
                "helpers.mustache",
                "api.mustache",
                "error.mustache",
                "main.mustache"
            )
        }
        supportingFiles.addAll(
            listOf(
                SupportingFile("gitignore", ".gitignore"),
                SupportingFile("go.mustache", "go.mod"),
                SupportingFile("spec.mustache", sourceFolder, "spec.json"),
                SupportingFile("validation.mustache", sourceFolder, "validation.go"),
                SupportingFile("conversion.mustache", sourceFolder, "conversion.go")
            )
        )
    }

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        handleDescriptionByAllOf(openAPI)
        extractInlineEnum(openAPI)
        removeOperationTags(openAPI)
    }

    override fun postProcessSupportingFileData(objs: Map<String?, Any?>?): Map<String?, Any?>? {
        generateJSONSpecFile(objs)
        return super.postProcessSupportingFileData(objs)
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): MutableMap<String, ModelsMap> {
        val models = super.postProcessAllModels(objs)

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

        setSuccessResponseModel(operations, allModels)

        operations.forEach {
            it.vendorExtensions["CODE"] = it.code
        }

        return operationsMap
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(AbstractGoCodegen::class.java)
    }
}
