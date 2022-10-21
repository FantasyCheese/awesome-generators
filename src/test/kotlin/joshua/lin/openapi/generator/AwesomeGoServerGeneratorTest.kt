package joshua.lin.openapi.generator

import org.junit.Test
import org.openapitools.codegen.DefaultGenerator
import org.openapitools.codegen.config.CodegenConfigurator

class AwesomeGoServerGeneratorTest {
    @Test
    fun front() {
        generate("front-stage")
    }

    @Test
    fun back() {
        generate("back-stage")
    }

    private fun generate(stage: String) {
        val moduleName = "model-tv-$stage-go-server"
        DefaultGenerator().opts(
            CodegenConfigurator()
                .setGeneratorName("awesome-go-server")
                .setGlobalProperties(mapOf("skipFormModel" to "false"))
                .setAdditionalProperties(mapOf(
                    "moduleName" to moduleName,
                    "serverPort" to "58271"
                ))
                .setEnablePostProcessFile(true)
                .setInputSpec("../model-tv-api-spec/public/$stage/spec-bundle.yaml")
                .setOutputDir("generated/$moduleName")
                .toClientOptInput()
        ).generate()
    }
}
