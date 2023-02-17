package joshua.lin.openapi.generator.typescript

import com.google.common.base.CaseFormat
import joshua.lin.openapi.generator.genericSymbols
import joshua.lin.openapi.generator.genericType
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenProperty

val CodegenModel.code
    get() = when {
        genericSymbols.isNotEmpty() -> """
            $importStatements
            
            export interface $classname$genericDeclarationCode {
                ${allVars.joinToString("\n") { it.fieldCode }}
            }
        """

//        hasDiscriminatorWithNonEmptyMapping -> """
//                import 'package:flutter/foundation.dart';
//                import 'package:freezed_annotation/freezed_annotation.dart';
//                $importStatements
//
//                part '${classFilename}.freezed.dart';
//                ${if (allVars.any { it.isBinary }) "" else "part '${classFilename}.g.dart';"}
//
//                @Freezed(fromJson: true)
//                class $classname with _${'$'}${classname} {
//                  ${(anyOf + oneOf).joinToString("\n") { modelName -> """
//                    const factory ${classname}.${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, modelName)}({
//                       ${interfaceModels.first { it.name == modelName }.allVars.joinToString("\n") { it.constructorParameter }}
//                    }) = $modelName;
//                  """}}
//
//                  factory ${classname}.fromJson(Map<String, dynamic> json) {
//                    switch (json['${discriminatorName}']) {
//                      ${discriminator.mapping.entries.joinToString("\n") {
//            "case '${it.key}': return ${it.value.split("/").last()}.fromJson(json);"
//        }}
//
//                      default:
//                        throw CheckedFromJsonException(json, '$discriminatorName', '$classname',
//                         'Invalid union type "${"$"}{json['type']}"!',
//                        );
//                    }
//                  }
//                }
//        """

        isEnum -> """
            export enum $classname {
                $enumValues
            }
        """

        else -> """
            $importStatements
            
            export interface $classname${if (isArray) " extends Array<$arrayModelType>" else ""} {
                ${vars.joinToString("\n") { it.code }}
                $additionalPropertiesCode
            }
            """
    }

val CodegenProperty.code
    get() = "$baseName${if (required) "" else "?"}: $dataType${if (isNullable) " | null" else ""}"

val CodegenModel.importStatements
    get() = imports.joinToString("\n") {
        """import {$it} from "./${CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, it)}";"""
    }

val CodegenModel.additionalPropertiesCode
    get() = if (additionalPropertiesType == null) "" else "[key: string]: ${additionalPropertiesType}${if (!hasVars) "" else " | any"};"

val CodegenModel.enumValues
    get() = allowableValues["enumVars"].let { it as List<Map<String, Any>> }
        .joinToString(",\n") { "${it["name"]} = ${it["value"]}" }

private val CodegenModel.genericDeclarationCode
    get() = "<${genericSymbols.joinToString(",")}>"

private val CodegenProperty.fieldCode
    get() = "${name}${if (required) "" else "?"}: ${genericType ?: dataType}"