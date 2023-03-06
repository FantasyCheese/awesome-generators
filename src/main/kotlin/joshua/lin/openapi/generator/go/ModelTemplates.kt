package joshua.lin.openapi.generator.go

import com.google.common.base.CaseFormat.*
import joshua.lin.openapi.generator.*
import org.openapitools.codegen.CodegenModel
import org.openapitools.codegen.CodegenProperty

val CodegenModel.code
    get() = when {
        isEnum -> """
            type $classname struct{ _value $dataType }
            
            func (it $classname) Value() $dataType {
                switch it._value {
                case ${enumValues.joinToString(",") { it["value"] as String }}:
                    return it._value
                default:
                    panic(fmt.Errorf("illegal enum value: %v", it._value))
                }
                return it._value
            }
            func (it $classname) MarshalJSON() ([]byte, error) {
                return []byte(fmt.Sprintf("%v", it.Value())), nil
            }
            func (it *$classname) UnmarshalJSON(data []byte) error {
                var v $dataType
                if err := json.Unmarshal(data, &v); err != nil {
                    return err
                }
                switch v {
                case ${enumValues.joinToString(",") { it["value"] as String }}:
                    it._value = v
                    return nil
                default:
                    return fmt.Errorf("illegal enum value: %v", v)
                }
            }
            
            type $enumValuesName struct{}
            
            $enumConstructors
            
            var ${classname}Values $enumValuesName
        """.trimIndent()

        else -> """
            ${(!description.isNullOrBlank()) insert { "// $classname : $description" }}
            type $classname${genericDeclaration} struct {
                ${allVars.joinToString("\n") { it.code }}
            }
        """.trimIndent()
    }

val CodegenProperty.code
    get() = """
        ${deprecated insert { "// Deprecated" }}
        ${!description.isNullOrBlank() insert { "// $description" }}
        $name ${isNullable insert { "*" }}${type} `json:"$baseName${!required insert { ",omitempty" }}" form:"$baseName${!required insert { ",omitempty" }}"`
    """.trimIndent()

val CodegenModel.genericDeclaration
    get() = genericSymbols.isNotEmpty() insert { "[${genericSymbols.joinToString(",") { "$it any" }}]" }

val CodegenModel.enumValuesName get() = "${UPPER_CAMEL.to(LOWER_CAMEL, classname)}Values"

val CodegenModel.enumConstructors: String
    get() = enumValues.joinToString("\n") {
        "func (s $enumValuesName) ${
            UPPER_UNDERSCORE.to(UPPER_CAMEL, it["name"] as String)
        }() $classname  { return $classname{_value: ${it["value"]}} }"
    }
