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
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import org.ballerinalang.stdlib.udp.Constants;

/**
 * Class to Validate UDP services.
 */
public class ServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private FunctionDefinitionNode onDatagramFunctionNode;
    private FunctionDefinitionNode onBytesFunctionNode;
    private FunctionDefinitionNode onErrorFunctionNode;

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        serviceDeclarationNode.members().stream()
                .filter(child -> child.kind() == SyntaxKind.OBJECT_METHOD_DEFINITION).forEach(node -> {
            FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
            onDatagramFunctionNode = functionDefinitionNode.functionName().toString()
                    .compareTo(Constants.ON_DATAGRAM) == 0 ? functionDefinitionNode : onDatagramFunctionNode;
            onBytesFunctionNode = functionDefinitionNode.functionName()
                    .toString().compareTo(Constants.ON_BYTES) == 0 ? functionDefinitionNode : onBytesFunctionNode;
            onErrorFunctionNode = functionDefinitionNode.functionName()
                    .toString().compareTo(Constants.ON_ERROR) == 0 ? functionDefinitionNode : onErrorFunctionNode;
        });
        checkOnBytesAndOnDatagramFunctionExistence(ctx);
        validateFunctionSignature(ctx, onDatagramFunctionNode, Constants.ON_DATAGRAM);
        validateFunctionSignature(ctx, onBytesFunctionNode, Constants.ON_BYTES);
        validateFunctionSignature(ctx, onErrorFunctionNode, Constants.ON_ERROR);

    }

    private void validateFunctionSignature(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode functionDefinitionNode,
                                           String functionName) {
        if (functionDefinitionNode != null) {
            hasRemoteKeyword(ctx, functionDefinitionNode, functionName);
            SeparatedNodeList<ParameterNode> parameterNodes = functionDefinitionNode.functionSignature().parameters();
            if (hasNoParameters(ctx, parameterNodes, functionDefinitionNode, functionName)) {
                return;
            }
            validateParameter(ctx, parameterNodes, functionName);
        }
    }

    private void checkOnBytesAndOnDatagramFunctionExistence(SyntaxNodeAnalysisContext ctx) {
        if (onBytesFunctionNode != null && onDatagramFunctionNode != null) {
            // Service shouldn't contain both onDatagram, onBytes method
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo("code",
                    "Service cannot contain both `onDatagram` {0} and `onBytes` {1} functions.",
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo, ctx.node().location(),
                    onDatagramFunctionNode.location().lineRange(), onBytesFunctionNode.location().lineRange()));
        } else if (onBytesFunctionNode == null && onDatagramFunctionNode == null) {
            // At-least service should contain onDatagram method or onBytes method
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo("code",
                    "Service does not contain `onDatagram` or `onBytes` function.",
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    ctx.node().location()));
        }

    }

    private void hasRemoteKeyword(SyntaxNodeAnalysisContext ctx, FunctionDefinitionNode functionDefinitionNode,
                                  String functionName) {
        boolean hasRemoteKeyword = functionDefinitionNode.qualifierList().stream()
                .filter(q -> q.kind() == SyntaxKind.REMOTE_KEYWORD).toArray().length == 1;
        if (!hasRemoteKeyword) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo("code",
                    "`remote` keyword expected in `{0}` function signature.",
                    DiagnosticSeverity.ERROR);
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.functionKeyword().location(), functionName));

        }
    }

    private boolean hasNoParameters(SyntaxNodeAnalysisContext ctx, SeparatedNodeList<ParameterNode> parameterNodes,
                                    FunctionDefinitionNode functionDefinitionNode,
                                    String functionName) {
        if (parameterNodes.isEmpty()) {
            DiagnosticInfo diagnosticInfo = new DiagnosticInfo("code",
                    "No parameter provided for `{0}`," +
                            " function expects `{1}` as a parameter.",
                    DiagnosticSeverity.ERROR);
            String expectedParameter = functionName.equals(Constants.ON_BYTES) ?
                    Constants.READ_ONLY_BYTE_ARRAY : functionName.equals(Constants.ON_ERROR) ?
                    "udp:" + Constants.ErrorType.Error.errorType() : Constants.READ_ONLY_DATAGRAM;
            ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                    functionDefinitionNode.functionSignature().location(), functionName, expectedParameter));
            return true;
        }
        return false;
    }

    private void validateParameter(SyntaxNodeAnalysisContext ctx, SeparatedNodeList<ParameterNode> parameterNodes,
                                   String functionName) {
        for (ParameterNode parameterNode : parameterNodes) {
            RequiredParameterNode requiredParameterNode = (RequiredParameterNode) parameterNode;
            Node typeName = requiredParameterNode.typeName();
            DiagnosticInfo diagnosticInfo;
            String code = "UDP_101";
            if (functionName.equals(Constants.ON_DATAGRAM)
                    && ((typeName.kind() == SyntaxKind.INTERSECTION_TYPE_DESC
                    && !typeName.toString().contains(Constants.DATAGRAM_RECORD))
                    || (typeName.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE
                    && !typeName.toString().contains(Constants.CALLER)))) {
                if (typeName.toString().contains(Constants.DATAGRAM_RECORD)) {
                    diagnosticInfo = new DiagnosticInfo(code,
                            "Invalid parameter `{0}` provided for `onDatagram`," +
                                    " function expects `readonly & udp:Datagram`.",
                            DiagnosticSeverity.ERROR);
                } else {
                    diagnosticInfo = new DiagnosticInfo(code,
                            "Invalid parameter `{0}` provided for `onDatagram` function.",
                            DiagnosticSeverity.ERROR);
                }
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode));
            } else if (functionName.equals(Constants.ON_BYTES)
                    && ((typeName.kind() == SyntaxKind.INTERSECTION_TYPE_DESC
                    && !typeName.toString().contains(Constants.BYTE_ARRAY))
                    || (typeName.kind() == SyntaxKind.QUALIFIED_NAME_REFERENCE
                    && !typeName.toString().contains(Constants.CALLER)))) {
                diagnosticInfo = new DiagnosticInfo(code,
                        "Invalid parameter `{0}` provided for `onBytes` function.",
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode));
            } else if (functionName.equals(Constants.ON_ERROR)
                    && !typeName.toString().contains(Constants.ErrorType.Error.errorType())) {
                diagnosticInfo = new DiagnosticInfo(code,
                        "Invalid parameter `{0}` provided for `onError`, function expects `udp:Error`.",
                        DiagnosticSeverity.ERROR);
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode));
            } else if (typeName.kind() != SyntaxKind.QUALIFIED_NAME_REFERENCE
                    && typeName.kind() != SyntaxKind.INTERSECTION_TYPE_DESC) {
                if (functionName.equals(Constants.ON_BYTES)
                        && typeName.toString().contains(Constants.BYTE_ARRAY)) {
                    diagnosticInfo = new DiagnosticInfo(code,
                            "Invalid parameter `{0}` provided for `{1}`," +
                                    " function expects `readonly & byte[]`.",
                            DiagnosticSeverity.ERROR);
                } else if (functionName.equals(Constants.ON_DATAGRAM)
                        && typeName.toString().contains(Constants.DATAGRAM_RECORD)) {
                    diagnosticInfo = new DiagnosticInfo(code,
                            "Invalid parameter `{0}` provided for `{1}`," +
                                    " function expects `readonly & udp:Datagram`.",
                            DiagnosticSeverity.ERROR);
                } else {
                    diagnosticInfo = new DiagnosticInfo(code,
                            "Invalid parameter `{0}` provided for `{1}` function.",
                            DiagnosticSeverity.ERROR);
                }
                ctx.reportDiagnostic(DiagnosticFactory.createDiagnostic(diagnosticInfo,
                        requiredParameterNode.location(), requiredParameterNode, functionName));
            }
        }
    }
}
