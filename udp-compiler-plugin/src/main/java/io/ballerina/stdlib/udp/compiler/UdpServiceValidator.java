/*
 * Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.udp.compiler;

import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.ParameterNode;
import io.ballerina.compiler.syntax.tree.RequiredParameterNode;
import io.ballerina.compiler.syntax.tree.ReturnTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.UnionTypeDescriptorNode;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.stdlib.udp.Constants;

import java.util.Optional;

/**
 * Class to Validate UDP services.
 */
public class UdpServiceValidator {
    private FunctionDefinitionNode onDatagramFunctionNode;
    private FunctionDefinitionNode onBytesFunctionNode;
    private FunctionDefinitionNode onErrorFunctionNode;
    private final String modulePrefix;
    private SyntaxNodeAnalysisContext ctx;

    // Message formats for reporting error diagnostics
    private static final String CODE = "UDP_101";
    public static final String SERVICE_CANNOT_CONTAIN_BOTH_ON_DATAGRAM_0_AND_ON_BYTES_1_FUNCTIONS
            = "Service cannot contain both `onDatagram` {0} and `onBytes` {1} functions.";
    public static final String SERVICE_DOES_NOT_CONTAIN_ON_DATAGRAM_OR_ON_BYTES_FUNCTION
            = "Service does not contain `onDatagram` or `onBytes` function.";
    public static final String NO_PARAMETER_PROVIDED_FOR_0_FUNCTION_EXPECTS_1_AS_A_PARAMETER
            = "No parameter provided for `{0}`, function expects `{1}` as a parameter.";
    public static final String REMOTE_KEYWORD_EXPECTED_IN_0_FUNCTION_SIGNATURE
            = "`remote` keyword expected in `{0}` function signature.";
    public static final String INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2
            = "Invalid parameter `{0}` provided for `{1}`, function expects `{2}`.";
    public static final String INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION
            = "Invalid parameter `{0}` provided for `{1}` function.";
    public static final String INVALID_RETURN_TYPE_0_FUNCTION_1_RETURN_TYPE_SHOULD_BE_A_SUBTYPE_OF_2
            = "Invalid return type `{0}` provided for function `{1}`, return type should be a subtype of `{2}`";
    public static final String FUNCTION_0_NOT_ACCEPTED_BY_THE_SERVICE = "Function `{0}` not accepted by the service";

    // expected parameters and return types
    public static final String READONLY_INTERSECTION = "readonly & ";
    public static final String DATAGRAM = "Datagram";
    public static final String BYTE_ARRAY = "byte[]";
    public static final String ERROR = "Error";
    public static final String OPTIONAL = "?";

    public UdpServiceValidator(SyntaxNodeAnalysisContext syntaxNodeAnalysisContext, String modulePrefixOrModuleName) {
        ctx = syntaxNodeAnalysisContext;
        modulePrefix = modulePrefixOrModuleName;
    }

    public void validate() {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        serviceDeclarationNode.members().stream()
                .filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION).forEach(node -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            String functionName = functionDefinitionNode.functionName().toString();
            if (functionName.compareTo(Constants.ON_DATAGRAM) != 0 && functionName.compareTo(Constants.ON_BYTES) != 0
                    && functionName.compareTo(Constants.ON_ERROR) != 0) {
                reportInvalidFunction(functionDefinitionNode);
            } else {
                onDatagramFunctionNode = functionName.compareTo(Constants.ON_DATAGRAM) == 0 ? functionDefinitionNode
                        : onDatagramFunctionNode;
                onBytesFunctionNode = functionName.compareTo(Constants.ON_BYTES) == 0 ? functionDefinitionNode
                        : onBytesFunctionNode;
                onErrorFunctionNode = functionName.compareTo(Constants.ON_ERROR) == 0 ? functionDefinitionNode
                        : onErrorFunctionNode;
            }
        });
        checkOnBytesAndOnDatagramFunctionExistence();
        validateFunctionSignature(onDatagramFunctionNode, Constants.ON_DATAGRAM);
        validateFunctionSignature(onBytesFunctionNode, Constants.ON_BYTES);
        validateFunctionSignature(onErrorFunctionNode, Constants.ON_ERROR);
    }

    private void reportInvalidFunction(FunctionDefinitionNode functionDefinitionNode) {
        DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE, FUNCTION_0_NOT_ACCEPTED_BY_THE_SERVICE,
                DiagnosticSeverity.ERROR);
        ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                functionDefinitionNode.location(), functionDefinitionNode.functionName().toString()));
    }

    private void validateFunctionSignature(FunctionDefinitionNode functionDefinitionNode, String functionName) {
        if (functionDefinitionNode != null) {
            hasRemoteKeyword(functionDefinitionNode, functionName);
            SeparatedNodeList<ParameterNode> parameterNodes = functionDefinitionNode.functionSignature().parameters();
            if (hasNoParameters(parameterNodes, functionDefinitionNode, functionName)) {
                return;
            }
            validateParameter(parameterNodes, functionName);
            validateFunctionReturnTypeDesc(functionDefinitionNode, functionName);
        }
    }

    private void checkOnBytesAndOnDatagramFunctionExistence() {
        if (onBytesFunctionNode != null && onDatagramFunctionNode != null) {
            // Service shouldn't contain both onDatagram, onBytes method
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE,
                    SERVICE_CANNOT_CONTAIN_BOTH_ON_DATAGRAM_0_AND_ON_BYTES_1_FUNCTIONS,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, ctx.node().location(),
                    onDatagramFunctionNode.location().lineRange(), onBytesFunctionNode.location().lineRange()));
        } else if (onBytesFunctionNode == null && onDatagramFunctionNode == null) {
            // At-least service should contain onDatagram method or onBytes method
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE,
                    SERVICE_DOES_NOT_CONTAIN_ON_DATAGRAM_OR_ON_BYTES_FUNCTION,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    ctx.node().location()));
        }

    }

    private void hasRemoteKeyword(FunctionDefinitionNode functionDefinitionNode, String functionName) {
        boolean hasRemoteKeyword = functionDefinitionNode.qualifierList().stream()
                .filter(q -> q.kind() == SyntaxKind.REMOTE_KEYWORD).toArray().length == 1;
        if (!hasRemoteKeyword) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE,
                    REMOTE_KEYWORD_EXPECTED_IN_0_FUNCTION_SIGNATURE,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.functionKeyword().location(), functionName));
        }
    }

    private boolean hasNoParameters(SeparatedNodeList<ParameterNode> parameterNodes,
                                    FunctionDefinitionNode functionDefinitionNode,
                                    String functionName) {
        if (parameterNodes.isEmpty()) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE,
                    NO_PARAMETER_PROVIDED_FOR_0_FUNCTION_EXPECTS_1_AS_A_PARAMETER,
                    DiagnosticSeverity.ERROR);
            String expectedParameter = functionName.equals(Constants.ON_BYTES) ?
                    READONLY_INTERSECTION + BYTE_ARRAY : functionName.equals(Constants.ON_ERROR) ?
                    modulePrefix + ERROR : READONLY_INTERSECTION + modulePrefix + DATAGRAM;
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.functionSignature().location(), functionName, expectedParameter));
            return true;
        }
        return false;
    }

    private void validateParameter(SeparatedNodeList<ParameterNode> parameterNodes, String functionName) {
        for (ParameterNode parameterNode : parameterNodes) {
            RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
            Node parameterTypeName = requiredParameterNode.typeName();
            DiagnosticInfo diagnosticInfo;
            if (functionName.equals(Constants.ON_DATAGRAM)
                    && ((parameterTypeName.kind() == SyntaxKind.INTERSECTION_TYPE_DESC
                    && !parameterTypeName.toString().contains(Constants.DATAGRAM_RECORD))
                    || (parameterTypeName.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE
                    && !parameterTypeName.toString().contains(Constants.CALLER)))) {
                if (parameterTypeName.toString().contains(Constants.DATAGRAM_RECORD)) {
                    diagnosticInfo = new DiagnosticInfo(CODE,
                            INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                            requiredParameterNode.location(), requiredParameterNode,
                            functionName, READONLY_INTERSECTION + modulePrefix + DATAGRAM));
                } else {
                    diagnosticInfo = new DiagnosticInfo(CODE,
                            INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                            requiredParameterNode.location(), requiredParameterNode, functionName));
                }
            } else if (functionName.equals(Constants.ON_BYTES)
                    && ((parameterTypeName.kind() == SyntaxKind.INTERSECTION_TYPE_DESC
                    && !parameterTypeName.toString().contains(Constants.BYTE_ARRAY))
                    || (parameterTypeName.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE
                    && !parameterTypeName.toString().contains(Constants.CALLER)))) {
                diagnosticInfo = new DiagnosticInfo(CODE,
                        INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION,
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode, functionName));
            } else if (functionName.equals(Constants.ON_ERROR)
                    && !parameterTypeName.toString().contains(Constants.ERROR)) {
                diagnosticInfo = new DiagnosticInfo(CODE,
                        INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2,
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode, functionName,
                        modulePrefix + ERROR));
            } else if (parameterTypeName.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE
                    && parameterTypeName.kind() != SyntaxKind.INTERSECTION_TYPE_DESC) {
                if (functionName.equals(Constants.ON_BYTES)
                        && parameterTypeName.toString().contains(Constants.BYTE_ARRAY)) {
                    diagnosticInfo = new DiagnosticInfo(CODE,
                            INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                            requiredParameterNode.location(), requiredParameterNode, functionName,
                            READONLY_INTERSECTION + modulePrefix + BYTE_ARRAY));
                } else if (functionName.equals(Constants.ON_DATAGRAM)
                        && parameterTypeName.toString().contains(Constants.DATAGRAM_RECORD)) {
                    diagnosticInfo = new DiagnosticInfo(CODE,
                            INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                            requiredParameterNode.location(), requiredParameterNode, functionName,
                            READONLY_INTERSECTION + modulePrefix + DATAGRAM));
                } else {
                    diagnosticInfo = new DiagnosticInfo(CODE,
                            INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION,
                            DiagnosticSeverity.ERROR);
                    ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                            requiredParameterNode.location(), requiredParameterNode, functionName));
                }
            }
        }
    }

    private void validateFunctionReturnTypeDesc(FunctionDefinitionNode functionDefinitionNode, String functionName) {
        Optional<ReturnTypeDescriptorNode> returnTypeDescriptorNode = functionDefinitionNode
                .functionSignature().returnTypeDesc();
        if (returnTypeDescriptorNode.isEmpty()) {
            return;
        }

        Node returnTypeDescriptor = returnTypeDescriptorNode.get().type();
        String returnTypeDescWithoutTrailingSpace = returnTypeDescriptor.toString().split(" ")[0];
        boolean isOnBytesOrOnDatagram = functionName.equals(Constants.ON_DATAGRAM)
                || functionName.equals(Constants.ON_BYTES);

        if (isOnBytesOrOnDatagram && returnTypeDescriptor.kind() == SyntaxKind.ARRAY_TYPE_DESC
                && returnTypeDescWithoutTrailingSpace.compareTo(BYTE_ARRAY) == 0) {
            return;
        }

        if (isOnBytesOrOnDatagram && returnTypeDescriptor.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE
                && returnTypeDescWithoutTrailingSpace.compareTo(modulePrefix + DATAGRAM) == 0) {
            return;
        }

        if (isOnBytesOrOnDatagram && returnTypeDescriptor.kind() == SyntaxKind.OPTIONAL_TYPE_DESC
                && (returnTypeDescWithoutTrailingSpace.compareTo(modulePrefix + ERROR + OPTIONAL) == 0
                || returnTypeDescWithoutTrailingSpace.compareTo(modulePrefix + DATAGRAM + OPTIONAL) == 0
                || returnTypeDescWithoutTrailingSpace.compareTo(BYTE_ARRAY + OPTIONAL) == 0)) {
            return;
        }

        if (functionName.equals(Constants.ON_ERROR) && returnTypeDescriptor.kind() == SyntaxKind.OPTIONAL_TYPE_DESC
                && returnTypeDescWithoutTrailingSpace.compareTo(modulePrefix + ERROR + OPTIONAL) == 0) {
            return;
        }

        if (returnTypeDescriptor.kind() == SyntaxKind.NIL_TYPE_DESC) {
            return;
        }

        boolean hasInvalidUnionTypeDesc = false;
        boolean isUnionTypeDesc = false;
        if (isOnBytesOrOnDatagram && returnTypeDescriptor.kind() == SyntaxKind.UNION_TYPE_DESC) {
            isUnionTypeDesc = true;
            UnionTypeDescriptorNode unionTypeDescriptorNode = (UnionTypeDescriptorNode) returnTypeDescriptor;
            for (Node descriptor : unionTypeDescriptorNode.children()) {
                String descriptorWithoutTrailingSpace = descriptor.toString().split(" ")[0];
                if (descriptor.kind() == SyntaxKind.PIPE_TOKEN) {
                    continue;
                } else if (descriptor.kind() == SyntaxKind.ARRAY_TYPE_DESC
                        && descriptorWithoutTrailingSpace.compareTo(BYTE_ARRAY) == 0) {
                    continue;
                } else if (descriptor.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE
                        && descriptorWithoutTrailingSpace.compareTo(modulePrefix + DATAGRAM) == 0) {
                    continue;
                } else if (descriptor.kind() == SyntaxKind.OPTIONAL_TYPE_DESC
                        && descriptorWithoutTrailingSpace.compareTo(modulePrefix + ERROR + OPTIONAL) == 0) {
                } else if (descriptor.kind() == SyntaxKind.OPTIONAL_TYPE_DESC
                        && (descriptorWithoutTrailingSpace.compareTo(modulePrefix + ERROR + OPTIONAL) == 0
                        || descriptorWithoutTrailingSpace.compareTo(modulePrefix + DATAGRAM + OPTIONAL) == 0
                        || descriptorWithoutTrailingSpace.compareTo(BYTE_ARRAY + OPTIONAL) == 0)) {
                    continue;
                } else {
                    hasInvalidUnionTypeDesc = true;
                }
            }
        }

        if (hasInvalidUnionTypeDesc || !isUnionTypeDesc) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo(CODE,
                    INVALID_RETURN_TYPE_0_FUNCTION_1_RETURN_TYPE_SHOULD_BE_A_SUBTYPE_OF_2,
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    returnTypeDescriptor.location(), returnTypeDescriptor.toString(), functionName,
                    BYTE_ARRAY + " | " + modulePrefix + DATAGRAM + " | " + modulePrefix + ERROR + OPTIONAL));
        }
    }
}
