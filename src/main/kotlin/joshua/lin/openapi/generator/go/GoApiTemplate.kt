package joshua.lin.openapi.generator.go

import com.google.common.base.CaseFormat.*
import joshua.lin.openapi.generator.insert
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenParameter

val CodegenOperation.code
    get() = """
        // ${operationId}Ctrl - $summary ${isDeprecated insert { "\n// Deprecated" }}
        func ${operationId}Ctrl($operationId $operationId) func(w http.ResponseWriter, r *http.Request) {
            return func(w http.ResponseWriter, r *http.Request) {
                if !validateRequest(w, r) { return }
            
                ${hasBodyParam insert { decodeBody }}
                
                ${hasParams insert { paramsStructSetup }}
                
                $performOperation
                
                w.WriteHeader(http.StatusOK)
            }
        }
        type $operationId func(${hasParams insert { "params $paramsStruct" }}) ${returnType ?: ""}
        ${hasParams insert { paramsStructDefinition }}
    """.trimIndent()

val CodegenOperation.paramsStruct get() = "${operationId}Params"

val CodegenParameter.paramNameTitleCase: String get() = LOWER_CAMEL.to(UPPER_CAMEL, paramName)

val CodegenOperation.decodeBody
    get() = """
        // Decode request body into struct
		var ${bodyParam.paramName} ${bodyParam.dataType}
        _ = json.NewDecoder(r.Body).Decode(&${bodyParam.paramName})
    """.trimIndent()

val CodegenOperation.paramsStructSetup
    get() = """
        // Setup operation parameters
        params := ${paramsStruct}{
            ${allParams.joinToString("\n") { "${it.paramNameTitleCase}: ${it.fromRequest}," }}
        }
    """.trimIndent()

val CodegenOperation.performOperation
    get() = if (returnType == null) """
        // Perform operation
        ${operationId}(${hasParams insert { "params" }})
    """.trimIndent() else """
        // Perform operation
        result := ${operationId}(${hasParams insert { "params" }})
        _ = json.NewEncoder(w).Encode(result)
    """.trimIndent()

val CodegenOperation.paramsStructDefinition
    get() = """
        type $paramsStruct struct {
            ${allParams.joinToString("\n") { "${it.paramNameTitleCase} ${it.dataType}" }}
        }
    """.trimIndent()

val CodegenParameter.fromRequest
    get() = when {
        isHeaderParam -> "r.Header[\"${paramName}\"]"
        isQueryParam -> "r.URL.Query()[\"${paramName}\"]${!isArray insert { "[0]" }}"
        isBodyParam -> paramName ?: ""
        else -> ""
    }
