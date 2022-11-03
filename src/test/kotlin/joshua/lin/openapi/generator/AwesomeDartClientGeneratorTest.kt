package joshua.lin.openapi.generator

import org.junit.Test
import org.openapitools.codegen.DefaultGenerator
import org.openapitools.codegen.config.CodegenConfigurator

class AwesomeDartClientGeneratorTest {
    @Test
    fun front() {
        generate("front-stage")
    }

    @Test
    fun back() {
        generate("back-stage")
    }

    private fun generate(stage: String) {
        val moduleName = "model-tv-$stage-dart-client"
        DefaultGenerator().opts(
            CodegenConfigurator()
                .setGeneratorName("awesome-dart-client")
                .setGlobalProperties(mapOf("skipFormModel" to "false"))
                .setAdditionalProperties(mapOf(
                    "pubName" to moduleName.replace("-", "_")
                ))
                .setInputSpec("../model-tv/model-tv-api-spec/public/$stage/spec-bundle.yaml")
                .setOutputDir("generated/$moduleName")
                .toClientOptInput()
        ).generate()
    }
}