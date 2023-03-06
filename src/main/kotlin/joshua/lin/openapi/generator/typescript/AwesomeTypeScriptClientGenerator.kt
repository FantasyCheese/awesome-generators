package joshua.lin.openapi.generator.typescript

import io.swagger.v3.oas.models.OpenAPI
import joshua.lin.openapi.generator.*
import org.openapitools.codegen.CodegenConfig
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.SupportingFile
import org.openapitools.codegen.config.GlobalSettings
import org.openapitools.codegen.languages.TypeScriptAxiosClientCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationsMap

class AwesomeTypeScriptClientGenerator : TypeScriptAxiosClientCodegen(), CodegenConfig {
    init {
        GlobalSettings.setProperty("skipFormModel", "false")
        additionalProperties[CodegenConstants.TEMPLATE_DIR] = "awesome-typescript-client"
        additionalProperties[CodegenConstants.API_PACKAGE] = "APIs"
        additionalProperties[CodegenConstants.MODEL_PACKAGE] = "models"
        additionalProperties[NPM_REPOSITORY] = "//gitlab.com/api/v4/projects/33730459/packages/npm/"
        additionalProperties[SEPARATE_MODELS_AND_API] = true
    }

    override fun getName() = "awesome-typescript-client"

    override fun processOpts() {
        super.processOpts()
        supportingFiles.removeIf {
            it.templateFile in listOf(
                    "baseApi.mustache",
                    "common.mustache",
                    "configuration.mustache",
                    "README.mustache",
                    "npmignore",
                    "git_push.sh.mustache"
            )
        }

        val tsApiPackage = additionalProperties["tsApiPackage"] as String
        with(supportingFiles) {
            add(SupportingFile("npmrc.mustache", ".npmrc"))
            add(SupportingFile("apiIndex.mustache", tsApiPackage, "index.ts"))
        }
    }

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        handleDescriptionByAllOf(openAPI)
        extractInlineEnum(openAPI)
        removeOperationTags(openAPI)
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): Map<String, ModelsMap> {
        val models = super.postProcessAllModels(objs)

        models.map { it.value.models.first().model }.forEach { model ->
            model.vendorExtensions["CODE"] = model.code
        }

        return models
    }

    override fun postProcessOperationsWithModels(objs: OperationsMap?, allModels: MutableList<ModelMap>?): OperationsMap {
        val operationsMap = super.postProcessOperationsWithModels(objs, allModels)
        val operations = operationsMap.operations.operation

        setSuccessResponseModel(operations, allModels)

        operations.forEach {
            it.vendorExtensions["CODE"] = it.code
        }

        vendorExtensions["IMPORTS"] = "import {${allModels?.joinToString(",\n") { it.model.classname }}} from '../models/index';"

        return operationsMap
    }
}
