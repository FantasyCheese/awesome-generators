package joshua.lin.openapi.generator.typescript

import com.google.common.base.CaseFormat
import joshua.lin.openapi.generator.*
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenProperty

val CodegenModel.code
    get() = when {
        isEnum -> """
            export enum $classname {
                $enumValuesDeclaration
            }
        """

        else -> """
            $importStatements
            
            export interface $classname${genericDeclaration}${isArray insert { " extends Array<$arrayModelType>" }} {
                ${allVars.joinToString("\n") { it.code }}
                $additionalPropertiesCode
            }
        """
    }

val CodegenProperty.code
    get() = "$baseName${if (required) "" else "?"}: ${type}${if (isNullable) " | null" else ""}"

val CodegenModel.importStatements
    get() = imports.joinToString("\n") {
        """import {$it} from "./${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, it)}";"""
    }

val CodegenModel.additionalPropertiesCode
    get() = if (additionalPropertiesType == null) "" else "[key: string]: ${additionalPropertiesType}${if (!hasVars) "" else " | any"};"

val CodegenModel.enumValuesDeclaration
    get() = enumValues.joinToString(",\n") { "${it["name"]} = ${it["value"]}" }

private val CodegenModel.genericDeclaration
    get() = genericSymbols.isNotEmpty() insert { "<${genericSymbols.joinToString(",")}>" }
