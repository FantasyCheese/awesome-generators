package joshua.lin.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import org.openapitools.codegen.CodegenConfig
import org.openapitools.codegen.CodegenConstants
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenOperation
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
        additionalProperties[RESPONSE_WRAPPER] = "ResponseWrapper.data"
        additionalProperties[LIST_RESPONSE_WRAPPER] = "ListResponseWrapper.data"
        additionalProperties[SEPARATE_MODELS_AND_API] = true
    }

    override fun getName() = "awesome-typescript-client"

    override fun processOpts() {
        super.processOpts()
        supportingFiles.removeIf {
            it.templateFile in listOf(
                    "api.mustache",
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
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): MutableMap<String, ModelsMap> {
        val models = super.postProcessAllModels(objs)
        handleResponseWrapperModel(models, additionalProperties[RESPONSE_WRAPPER] as String)
        handleResponseWrapperModel(models, additionalProperties[LIST_RESPONSE_WRAPPER] as String)
        return models
    }

    private fun handleResponseWrapperModel(models: MutableMap<String, ModelsMap>, wrapper: String) {
        val name = wrapper.split(".").first()
        val path = wrapper.split(".").last()
        @Suppress("UNCHECKED_CAST")
        val model = models[name] as MutableMap<String, Any>
        @Suppress("UNCHECKED_CAST")
        val codegenModel = (model["models"] as List<Map<String, Any>>).first()["model"] as CodegenModel
        codegenModel.vars.forEach {
            if (it.name == path) {
                it.dataType = "T"
                model["isGeneric"] = true
            }
        }
    }

    override fun postProcessOperationsWithModels(objs: OperationsMap?, allModels: MutableList<ModelMap>?): OperationsMap {
        val obj = super.postProcessOperationsWithModels(objs, allModels)

        @Suppress("UNCHECKED_CAST") val operations = obj["operations"] as? Map<String, Any>?
        @Suppress("UNCHECKED_CAST") val operation = operations?.get("operation") as? List<CodegenOperation>? ?: listOf()

        // For JS string interpolation "/{id}" -> "/${args.id}"
        operation.forEach {
            it.path = it.path.replace("\\{(.*)}".toRegex()) { "\${args.${it.groupValues[1]}}" }
        }

        handleResponseWrapper(operation, additionalProperties[RESPONSE_WRAPPER] as String)
        handleResponseWrapper(operation, additionalProperties[LIST_RESPONSE_WRAPPER] as String)

        return obj
    }

    private fun handleResponseWrapper(operations: List<CodegenOperation>, wrapper: String) {
        val name = wrapper.split(".").first()
        val path = wrapper.split(".").last()
        operations.forEach { op ->
            if (op.returnType?.startsWith(name) != true) return@forEach
            val resp = op.responses.firstOrNull { it.is2xx } ?: return@forEach
            val payloadType = resp.composedSchemas?.allOf
                ?.firstOrNull { name != it.dataType }?.vars
                ?.firstOrNull { it.name == path }?.dataType
                ?: "void"
            op.returnType = "$name<${payloadType}>"
        }
    }

    companion object {
        const val RESPONSE_WRAPPER = "responseWrapper"
        const val LIST_RESPONSE_WRAPPER = "listResponseWrapper"
    }
}
