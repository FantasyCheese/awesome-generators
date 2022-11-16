package joshua.lin.openapi.generator

import com.google.common.base.CaseFormat
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenProperty

val CodegenModel.code
    get() = when {
        isEnum -> """
                class $classname {
                  const $classname._(this.value);
                  final $dataType value;
                  factory $classname.fromJson($dataType value) => $classname._(value);
                  $dataType toJson() => value;

                  $enumValues
                }
            """

        genericSymbols.isNotEmpty() -> """
                import 'package:json_annotation/json_annotation.dart';
                $importStatements

                part '${classFilename}.g.dart';

                @JsonSerializable(genericArgumentFactories: true)
                class ${classname}${genericDeclarationCode} {
                  ${allVars.joinToString("\n") { it.fieldCode }}

                  const ${classname}(${allVars.joinToString(",") { "this.${it.name}" }});

                  factory ${classname}.fromJson(
                    Map<String, dynamic> json,
                    ${genericSymbols.joinToString("\n") { "$it Function(Object? json) fromJson$it," }}
                  ) {
                    return _$${classname}FromJson${genericDeclarationCode}(json, ${genericSymbols.joinToString(",") { "fromJson$it" }});
                  }

                  Map<String, dynamic> toJson(
                    ${genericSymbols.joinToString("\n") { "Object Function($it value) toJson$it," }}
                  ) {
                    return _$${classname}ToJson${genericDeclarationCode}(this, ${genericSymbols.joinToString(",") { "toJson$it" }});
                  }
                }
            """

        else -> """
                import 'package:flutter/foundation.dart';
                import 'package:freezed_annotation/freezed_annotation.dart';
                $importStatements

                part '${classFilename}.freezed.dart';
                ${if (allVars.any { it.isBinary }) "" else "part '${classFilename}.g.dart';"}

                @freezed
                class $classname with _$${classname} {
                  const factory ${classname}({
                     ${allVars.joinToString("\n") { it.constructorParameter }}
                  }) = _${classname};

                  ${if (allVars.any { it.isBinary }) "" else "factory ${classname}.fromJson(final Map<String, Object?> json) => _\$${classname}FromJson(json);"}
                }
            """
    }

private val CodegenModel.importStatements
    get() = imports.joinToString("\n") {
        val snakeCase = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, it)
        if (snakeCase == "file") "import 'dart:io';"
        else "import '${snakeCase}.dart';"
    }

private val CodegenModel.enumValues
    get() = allowableValues["enumVars"].let { it as List<Map<String, Any>> }.joinToString("\n") {
        "static const ${it["name"]} = ${classname}._(${it["value"]});"
    }

private val CodegenProperty.constructorParameter
    get() =
        if (required) "$typeDefault required ${genericType ?: dataType} ${name},"
        else "$typeDefault ${genericType ?: dataType}? ${name},"

private val CodegenModel.genericDeclarationCode
    get() = "<${genericSymbols.joinToString(",")}>"

private val CodegenProperty.fieldCode
    get() = "final ${genericType ?: dataType}${if (required) "" else "?"} ${name};"

private val CodegenProperty.typeDefault
    get() = when {
        dataType.startsWith("List") -> "@Default([])"
//        dataType.startsWith("String") -> "@Default('')"
//        dataType.startsWith("int") -> "@Default(0)"
//        dataType.startsWith("double") -> "@Default(0)"
        else -> ""
    }
