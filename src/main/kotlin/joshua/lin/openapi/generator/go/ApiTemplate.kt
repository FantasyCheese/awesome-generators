package joshua.lin.openapi.generator.go

import com.google.common.base.CaseFormat.*
import joshua.lin.openapi.generator.*
import org.openapitools.codegen.*

val CodegenOperation.code
    get() = """
        // ${operationId}Ctrl - $summary ${isDeprecated insert { "\n// Deprecated" }}
        func ${operationId}Ctrl(
            ${responses.joinToString("\n", transform = ::respondFunction)}
        ) func(http.ResponseWriter, *http.Request) {
            return func(writer http.ResponseWriter, request *http.Request) {
                $errToBytes5XX
                
                $validateRequest
            
                ${hasBodyParam insert { decodeBody }}
                
                ${hasParams insert { paramsStructSetup }}
                
                $performOperation
                
                $writeResponse
            }
        }
        ${hasParams insert { paramsStructDefinition }}
    """.trimIndent()

val CodegenOperation.paramsStruct get() = "${operationId}Params"

val CodegenParameter.paramNameTitleCase: String get() = LOWER_CAMEL.to(UPPER_CAMEL, paramName)

val CodegenOperation.validateRequest: String
    get() = """
        // Validate request
        input, err := validateRequest(request)
        if err != nil {
            $bytes4XX
            respond(writer, input, resp5XX, &response{
                Status: http.StatusBadRequest,
                Header: http.Header{"Content-Type": {"application/json"}},
                Body: bytes,
            })
            return
        }
    """.trimIndent()

val CodegenOperation.decodeBody
    get() = """
        // Decode request body into struct
		var ${bodyParam.paramName} ${bodyParam.dataType}
        _ = json.NewDecoder(request.Body).Decode(&${bodyParam.paramName})
    """.trimIndent()

val CodegenOperation.paramsStructSetup
    get() = """
        // Setup operation parameters
        params := ${paramsStruct}{
            ${allParams.joinToString("\n") { "${it.paramNameTitleCase}: ${it.retrieveFromRequest(path)}," }}
        }
    """.trimIndent()

val CodegenOperation.performOperation: String
    get() {
        val receiver = if (returnType == null) "err = " else "result, err := "
        return """    
            // Perform operation
            ${receiver}respond${responses.first { it.is2xx }.code}(request${hasParams insert { ", params" }})
            if err != nil {
                $bytes4XX
                respond(writer, input, resp5XX, &response{
                    Status: http.StatusInternalServerError,
                    Header: http.Header{"Content-Type": {"application/json"}},
                    Body:   bytes,
                })
                return
            }
        """.trimIndent()
    }

val CodegenOperation.writeResponse
    get() = """
    resp := &response{Status: http.StatusOK}
    
    ${(response2XX?.isJson ?: true) insert { jsonResponse }}
    
    respond(writer, input, resp5XX, resp)
""".trimIndent()

val jsonResponse = """
    // Write JSON response body and header
    bytes, _ := json.Marshal(result)
    respond(writer, input, resp5XX, &response{
        Status: http.StatusOK,
        Header: http.Header{"Content-Type": {"application/json"}},
        Body: bytes,
    })
""".trimIndent()

val CodegenOperation.goReturnType: String
    get() {
        val returnType = returnType('[' to ']', "")
        return if (returnType.isBlank()) "error" else "($returnType, error)"
    }

val CodegenOperation.paramsStructDefinition
    get() = """
        type $paramsStruct struct {
            ${allParams.joinToString("\n") { "${it.paramNameTitleCase} ${it.dataType}" }}
        }
    """.trimIndent()

fun CodegenParameter.retrieveFromRequest(path: String): String {
    val code = when {
        isPathParam -> {
            val matches = Regex("\\{(.*)}").find(path)
            println("${path}, $paramName: ${matches?.groupValues}")
            val index = matches?.groupValues?.indexOf(paramName)?:0
            "strings.Split(request.URL.Path, \"/\")[$index]"
        }
        isHeaderParam -> "request.Header[\"${paramName}\"]${!isArray insert { "[0]" }}"
        isQueryParam -> "request.URL.Query()[\"${paramName}\"]${!isArray insert { "[0]" }}"
        isBodyParam -> paramName ?: ""
        else -> ""
    }

    val conversionCode = when {
        isString || isBodyParam -> code
        isArray -> "stringArray2${baseType}Array($code)"
        else -> "string2${dataType}($code)"
    }

    return conversionCode
}

fun CodegenOperation.respondFunction(response: CodegenResponse): String {
    val funcName = "respond" + response.code
    val param = when {
        !response.is2xx -> "err error"
        hasParams -> "params $paramsStruct"
        else -> ""
    }
    val returnType = when {
        !response.is2xx -> response.dataType
        else -> goReturnType
    }

    return "$funcName func(request *http.Request, $param) $returnType,"
}

val CodegenOperation.bytes4XX
    get() = responses.firstOrNull { it.is4xx }
        ?.let { "bytes, _ := json.Marshal(respond${it.code}(request, err))" } ?: "bytes := []byte(err.Error())"

val CodegenOperation.bytes5XX
    get() = responses.firstOrNull { it.is5xx }
        ?.let { "bytes, _ := json.Marshal(respond${it.code}(request, err))" } ?: "bytes := []byte(err.Error())"

val CodegenOperation.errToBytes5XX
    get() = """
        // Setup 5XX error transformer
        resp5XX := func(err error) []byte {
            $bytes5XX
            return bytes
        }
    """.trimIndent()
