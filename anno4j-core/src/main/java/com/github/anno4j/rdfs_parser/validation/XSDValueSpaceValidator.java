package com.github.anno4j.rdfs_parser.validation;

import com.github.anno4j.model.namespaces.XSD;
import com.github.anno4j.rdfs_parser.model.RDFSClazz;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;

import java.util.Arrays;

/**
 * A validator injecting code which checks whether a parameter or variable is
 * in the value space of its XML Schema Definition datatype.
 * (see <a href="https://www.w3.org/TR/xmlschema11-2/">https://www.w3.org/TR/xmlschema11-2/</a>).
 */
public class XSDValueSpaceValidator implements Validator {

    private static final ClassName ILLEGAL_ARG_EXCEPTION = ClassName.get(IllegalArgumentException.class);

    /**
     * The URIs of the constrained XSD datatypes.
     */
    private static final String[] CONSTRAINED_TYPES = {XSD.NORMALIZED_STRING, XSD.TOKEN, XSD.LANGUAGE, XSD.NON_POSITIVE_INTEGER,
            XSD.NEGATIVE_INTEGER, XSD.NON_NEGATIVE_INTEGER, XSD.UNSIGNED_LONG, XSD.UNSIGNED_INT, XSD.UNSIGNED_SHORT,
            XSD.UNSIGNED_BYTE, XSD.POSITIVE_INTEGER};

    static {
        // Sort the constrained datatypes for later binary searching:
        Arrays.sort(CONSTRAINED_TYPES);
    }

    private static MethodSpec.Builder addNormalizedStringValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return builder.beginControlFlow("if($N.indexOf($S) != -1 || $N.indexOf($S) != -1 || $N.indexOf($S) != -1)",
                param, "\r", param, "\n", param, "\t")
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be a normalized string. Must not contain carriage return, line feed or tab.")
                .endControlFlow();
    }

    private static MethodSpec.Builder addTokenValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return
                // Check that value is normalized:
                addNormalizedStringValidation(builder, param)
                        // Check that value does not start or end with whitespace:
                        .beginControlFlow("if($N.startsWith($S) || $N.endsWith($S))", param, " ", param, " ")
                        .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                                "Value must be a XSD token. Must not start or end with whitespace.")
                        .endControlFlow()
                        // Check that the value does nor contain subsequences of two or more whitespaces:
                        .beginControlFlow("if($N.indexOf($S) != -1)", param, "  ")
                        .addStatement("throw new $T($S)",
                                ClassName.get(IllegalArgumentException.class),
                                "Value must be a XSD token. Must not contain subsequences of two or more whitespaces.")
                        .endControlFlow();
    }

    private static MethodSpec.Builder addLanguageValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return builder.beginControlFlow("if(!$N.matches($S))", param, "[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*")
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be a language identifier, as defined by BCP 47.")
                .endControlFlow();
    }

    private static MethodSpec.Builder addNonPositiveValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return builder.beginControlFlow("if($N > 0)", param)
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be non-positive.")
                .endControlFlow();
    }

    private static MethodSpec.Builder addNegativeValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return builder.beginControlFlow("if($N >= 0)", param)
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be negative.")
                .endControlFlow();
    }

    private static MethodSpec.Builder addNonNegativeValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return builder.beginControlFlow("if($N < 0)", param)
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be non-negative.")
                .endControlFlow();
    }

    private static MethodSpec.Builder addUnsignedValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return builder.beginControlFlow("if($N < 0)", param)
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be non-negative")
                .endControlFlow();
    }

    private static <T> MethodSpec.Builder addMaximumValidation(MethodSpec.Builder builder, ParameterSpec param, T max) {
        return addUnsignedValidation(builder, param)
                .beginControlFlow("if($N > " + max.toString() + ")", param)
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be less than " + max.toString())
                .endControlFlow();
    }

    private static MethodSpec.Builder addUnsignedIntegerValidation(MethodSpec.Builder builder, ParameterSpec param) {
        builder = addUnsignedValidation(builder, param);
        return addMaximumValidation(builder, param, 2147483647);
    }

    private static MethodSpec.Builder addUnsignedShortValidation(MethodSpec.Builder builder, ParameterSpec param) {
        builder = addUnsignedValidation(builder, param);
        return addMaximumValidation(builder, param, 65535);
    }

    private static MethodSpec.Builder addUnsignedByteValidation(MethodSpec.Builder builder, ParameterSpec param) {
        builder = addUnsignedValidation(builder, param);
        return addMaximumValidation(builder, param, 255);
    }

    private static MethodSpec.Builder addPositiveValidation(MethodSpec.Builder builder, ParameterSpec param) {
        return builder.beginControlFlow("if($N <= 0)", param)
                .addStatement("throw new $T($S)", ILLEGAL_ARG_EXCEPTION,
                        "Value must be positive")
                .endControlFlow();
    }

    @Override
    public void addValueSpaceCheck(MethodSpec.Builder methodBuilder, ParameterSpec symbol, RDFSClazz range) {
        if (range.getResourceAsString().startsWith(XSD.NS)) {
            // Add validation for the XSD datatypes:
            switch (range.getResourceAsString()) {
                case XSD.NORMALIZED_STRING:
                    addNormalizedStringValidation(methodBuilder, symbol);
                    break;
                case XSD.TOKEN:
                    addTokenValidation(methodBuilder, symbol);
                    break;
                case XSD.LANGUAGE:
                    addLanguageValidation(methodBuilder, symbol);
                    break;
                case XSD.NON_POSITIVE_INTEGER:
                    addNonPositiveValidation(methodBuilder, symbol);
                    break;
                case XSD.NEGATIVE_INTEGER:
                    addNegativeValidation(methodBuilder, symbol);
                    break;
                case XSD.NON_NEGATIVE_INTEGER:
                    addNonNegativeValidation(methodBuilder, symbol);
                    break;
                case XSD.UNSIGNED_LONG:
                    addUnsignedValidation(methodBuilder, symbol);
                    break;
                case XSD.UNSIGNED_INT:
                    addUnsignedIntegerValidation(methodBuilder, symbol);
                    break;
                case XSD.UNSIGNED_SHORT:
                    addUnsignedShortValidation(methodBuilder, symbol);
                    break;
                case XSD.UNSIGNED_BYTE:
                    addUnsignedByteValidation(methodBuilder, symbol);
                    break;
                case XSD.POSITIVE_INTEGER:
                    addPositiveValidation(methodBuilder, symbol);
                    break;
            }
        }
    }

    @Override
    public String getValueSpaceDefinition(RDFSClazz clazz) {
        switch (clazz.getResourceAsString()) {
            case XSD.NORMALIZED_STRING:
                return "The value space is the set of strings that do not contain the carriage " +
                        "return (#xD), line feed (#xA) nor tab (#x9) characters.";
            case XSD.TOKEN:
                return "The value space is the set of strings that do not contain the carriage return (#xD), " +
                        "line feed (#xA) nor tab (#x9) characters, that have no leading or trailing spaces (#x20) " +
                        "and that have no internal sequences of two or more spaces.";
            case XSD.LANGUAGE:
                return "The value space is the set of all strings that conform to the pattern" +
                        "[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*";
            case XSD.NON_POSITIVE_INTEGER:
                return "The value space is the infinite set {...,-2,-1,0}.";
            case XSD.NEGATIVE_INTEGER:
                return "The value space is the infinite set {...,-2,-1}.";
            case XSD.NON_NEGATIVE_INTEGER:
                return "The value space is the infinite set {0,1,2,...}.";
            case XSD.UNSIGNED_LONG:
                return "The value space is the infinite set {0,1,2,..., 18446744073709551615}.";
            case XSD.UNSIGNED_INT:
                return "The value space is the infinite set {0,1,2,..., 4294967295}.";
            case XSD.UNSIGNED_SHORT:
                return "The value space is the infinite set {0,1,2,..., 65535}.";
            case XSD.UNSIGNED_BYTE:
                return "The value space is the infinite set {0,1,2,..., 255}.";
            case XSD.POSITIVE_INTEGER:
                return "The value space is the infinite set {1,2,...}.";
            default:
                return null;
        }
    }

    @Override
    public boolean isValueSpaceConstrained(RDFSClazz clazz) {
        return Arrays.binarySearch(CONSTRAINED_TYPES, clazz.getResourceAsString()) >= 0;
    }
}
