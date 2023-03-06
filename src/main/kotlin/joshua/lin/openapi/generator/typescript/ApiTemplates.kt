package joshua.lin.openapi.generator.typescript

import joshua.lin.openapi.generator.*
import org.openapitools.codegen.CodegenOperation
import org.openapitools.codegen.CodegenParameter

val CodegenOperation.code
    get() = """
        export async function ${operationId}(${if (hasParams) "params: ${operationIdCamelCase}Params" else ""}): Promise<${returnType()}> {
            const response = await defaultAxios.request({
                url: `${pathCode}`,
                method: '${httpMethod}',
                $queryParamsCode
                $headerParamsCode
                $bodyParamCode
            });
            return response.data;
        }
        $paramsInterfaceCode
    """

val CodegenOperation.queryParamsCode
    get() = if (!hasQueryParams) "" else """
        params: {
            ${queryParams.joinToString(",\n") { "${it.paramName}: params.${it.paramName}" }}
        },
    """

val CodegenOperation.headerParamsCode
    get() = if (!hasHeaderParams) "" else """
        headers: {
            ${headerParams.joinToString(",\n") { "${it.paramName}: params.${it.paramName}" }}
        },
    """

val CodegenOperation.bodyParamCode
    get() = if (!hasBodyParam) "" else "data: params.${bodyParam.paramName}"

val CodegenOperation.paramsInterfaceCode
    get() = if (!hasParams) "" else """
        export interface ${operationIdCamelCase}Params {
            ${allParams.joinToString("\n") { it.code }}        
        }
    """

val CodegenOperation.pathCode
    get() = path.replace(Regex("\\{(.*)}")) { "\${params.${it.groupValues[1]}}" }

val CodegenParameter.code
    get() = "$paramName${if (required) "?" else ""}: $dataType${if (isNullable) " | null" else ""}"
