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

import io.ballerina.compiler.syntax.tree.ExplicitNewExpressionNode;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.SeparatedNodeList;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.compiler.syntax.tree.TypeDescriptorNode;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import org.ballerinalang.stdlib.udp.Constants;

/**
 * Class to filter UDP services.
 */
public class UdpServiceValidatorTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext ctx) {
        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) ctx.node();
        SeparatedNodeList<ExpressionNode> expressions = serviceDeclarationNode.expressions();

        String modulePrefix = Constants.UDP;
        ModulePartNode modulePartNode = ctx.syntaxTree().rootNode();
        for (ImportDeclarationNode importDeclaration : modulePartNode.imports()) {
            if (importDeclaration.moduleName().get(0).toString().split(" ")[0].compareTo(Constants.UDP) == 0) {
                if (importDeclaration.prefix().isPresent()) {
                    modulePrefix = importDeclaration.prefix().get().children().get(1).toString();
                }
                break;
            }
        }

        UdpServiceValidator udpServiceValidator = null;
        for (ExpressionNode expressionNode : expressions) {
            if (expressionNode.kind() == SyntaxKind.EXPLICIT_NEW_EXPRESSION) {

                TypeDescriptorNode typeDescriptorNode = ((ExplicitNewExpressionNode) expressionNode).typeDescriptor();
                Node moduleIdentifierTokenOfListener = typeDescriptorNode.children().get(0);
                if (moduleIdentifierTokenOfListener.toString().compareTo(modulePrefix) == 0) {
                    udpServiceValidator = new UdpServiceValidator(ctx, modulePrefix
                            + SyntaxKind.COLON_TOKEN.stringValue());
                }
            }
        }

        if (udpServiceValidator != null) {
            udpServiceValidator.validate();
        }
    }
}
