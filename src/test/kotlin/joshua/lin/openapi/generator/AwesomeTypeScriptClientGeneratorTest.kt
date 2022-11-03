package joshua.lin.openapi.generator

import org.junit.Test
import org.openapitools.codegen.DefaultGenerator
import org.openapitools.codegen.config.CodegenConfigurator

class AwesomeTypeScriptClientGeneratorTest {
    @Test
    fun front() {
        generate("front-stage")
    }

    @Test
    fun back() {
        generate("back-stage")
    }

    private fun generate(stage: String) {
        val packageName = "model-tv-$stage-typescript-client"
        DefaultGenerator().opts(
            CodegenConfigurator()
                .setEnablePostProcessFile(true)
                .setGenerateAliasAsModel(true)
                .setGeneratorName("awesome-typescript-client")
                .setAdditionalProperties(mapOf("npmName" to packageName))
                .setInputSpec("../model-tv/model-tv-api-spec/public/$stage/spec-bundle.yaml")
                .setOutputDir("generated/$packageName")
                .toClientOptInput()
        ).generate()
    }
}
