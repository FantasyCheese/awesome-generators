package joshua.lin.openapi.generator

import io.swagger.v3.oas.models.OpenAPI
import org.apache.commons.io.FilenameUtils
import org.openapitools.codegen.*
import org.openapitools.codegen.languages.AbstractGoCodegen
import org.openapitools.codegen.languages.GoGinServerCodegen
import org.openapitools.codegen.model.ModelMap
import org.openapitools.codegen.model.ModelsMap
import org.openapitools.codegen.model.OperationsMap
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class AwesomeGoServerGenerator : GoGinServerCodegen(), CodegenConfig {
    override fun getName() = "awesome-go-server"

    init {
        additionalProperties[RESPONSE_WRAPPER] = "ResponseWrapper.data"
        additionalProperties[LIST_RESPONSE_WRAPPER] = "ListResponseWrapper.data"
        additionalProperties[CodegenConstants.TEMPLATE_DIR] = "awesome-go-server"
        additionalProperties[CodegenConstants.PACKAGE_NAME] = "api"
        additionalProperties["apiPath"] = "api"
        typeMapping.let {
            it["File"] = "*multipart.FileHeader"
            it["file"] = "*multipart.FileHeader"
            it["binary"] = "*multipart.FileHeader"
        }
    }

    override fun processOpts() {
        super.processOpts()
        supportingFiles.apply {
            removeIf { it.templateFile in listOf(
                    "openapi.mustache",
                    "Dockerfile.mustache",
                    "README.mustache",
            ) }
            add(SupportingFile("go.mustache", "go.mod"))
            add(SupportingFile("gitignore", ".gitignore"))
            add(SupportingFile("utils.mustache", apiPath, "utils.go"))
            add(SupportingFile("spec.mustache", apiPath, "spec.json"))
        }
    }

    override fun preprocessOpenAPI(openAPI: OpenAPI) {
        super.preprocessOpenAPI(openAPI)
        handleDescriptionByAllOf(openAPI)
    }

    override fun postProcessAllModels(objs: MutableMap<String, ModelsMap>): MutableMap<String, ModelsMap> {
        val models = super.postProcessAllModels(objs)
        return models
    }

    private fun handleResponseWrapperModel(models: MutableMap<String, ModelsMap>, wrapper: String) {
        val name = wrapper.split(".").first()
        val path = wrapper.split(".").last()
        val model = models[name] ?: return
        @Suppress("UNCHECKED_CAST")
        val codegenModel = (model["models"] as List<Map<String, Any>>).first()["model"] as CodegenModel
        codegenModel.vars.forEach {
            if (it.baseName == path) {
                it.dataType = "T"
                model["isGeneric"] = true
            }
        }
    }

    override fun postProcessSupportingFileData(objs: Map<String?, Any?>?): Map<String?, Any?>? {
        generateJSONSpecFile(objs)
        return super.postProcessSupportingFileData(objs)
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

    override fun toApiFilename(name: String): String {
        var filename = "controller_${name.lowercase().replace("-", "_")}"
        if (isReservedFilename(filename)) {
            LOGGER.warn("$filename.go with suffix (reserved word) cannot be used as filename. Renamed to ${filename}_.go")
            filename += "_"
        }
        return filename
    }

    override fun postProcessParameter(parameter: CodegenParameter?) {
        super.postProcessParameter(parameter)
        if (parameter?.dataType == "IntBool") parameter.dataType = "bool"
    }

    override fun postProcessModelProperty(model: CodegenModel?, property: CodegenProperty?) {
        super.postProcessModelProperty(model, property)
        if (property?.dataType == "IntBool") property.dataType = "bool"
    }

    override fun postProcessFile(file: File?, fileType: String?) {
        super.postProcessFile(file, fileType)
        val goPostProcessFile = "/usr/local/go/bin/gofmt -w"
        if ("go" == FilenameUtils.getExtension(file.toString())) {
            val command = "$goPostProcessFile $file"
            try {
                val p = Runtime.getRuntime().exec(command)
                val exitValue = p.waitFor()
                if (exitValue != 0) {
                    LOGGER.error("Error running the command ({}). Exit code: {}", command, exitValue)
                } else {
                    LOGGER.info("Successfully executed: {}", command)
                }
            } catch (e: InterruptedException) {
                LOGGER.error("Error running the command ({}). Exception: {}", command, e.message)
                // Restore interrupted state
                Thread.currentThread().interrupt()
            } catch (e: IOException) {
                LOGGER.error("Error running the command ({}). Exception: {}", command, e.message)
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun handleResponseWrapper(operations: List<CodegenOperation>, wrapper: String) {
        val name = wrapper.split(".").first()
        val path = wrapper.split(".").last()
        operations.forEach { op ->
            if (op.returnType?.startsWith(name) != true) return@forEach
            val resp = op.responses.firstOrNull { it.is2xx } ?: return@forEach
            val payloadType = resp.composedSchemas?.allOf
                ?.firstOrNull { name != it.dataType }?.vars
                ?.firstOrNull { it.baseName == path }?.dataType
            op.returnType = payloadType?.let { "$name[${it}]" }
        }
    }

    companion object {
        const val RESPONSE_WRAPPER = "responseWrapper"
        const val LIST_RESPONSE_WRAPPER = "listResponseWrapper"
        private val LOGGER = LoggerFactory.getLogger(AbstractGoCodegen::class.java)
    }
}
