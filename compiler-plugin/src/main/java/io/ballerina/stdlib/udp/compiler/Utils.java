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

import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.text.LineRange;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocumentChange;
import io.ballerina.tools.text.TextEdit;
import io.ballerina.tools.text.TextRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compiler-plugin utility class.
 */
public final class Utils {

    private Utils() {}

    public static final String NODE_LOCATION = "node.location";
    public static final String LS = System.lineSeparator();

    public static boolean equals(String actual, String expected) {
        return actual.compareTo(expected) == 0;
    }

    public static NonTerminalNode findNode(SyntaxTree syntaxTree, LineRange lineRange) {
        if (lineRange == null) {
            return null;
        }

        TextDocument textDocument = syntaxTree.textDocument();
        int start = textDocument.textPositionFrom(lineRange.startLine());
        int end = textDocument.textPositionFrom(lineRange.endLine());
        return ((ModulePartNode) syntaxTree.rootNode()).findNode(TextRange.from(start, end - start), true);
    }

    public static List<DocumentEdit> getDocumentEdits(CodeActionExecutionContext codeActionExecutionContext,
                                                       String serviceText) {
        LineRange lineRange = null;
        for (CodeActionArgument argument : codeActionExecutionContext.arguments()) {
            if (NODE_LOCATION.equals(argument.key())) {
                lineRange = argument.valueAs(LineRange.class);
            }
        }

        if (lineRange == null) {
            return Collections.emptyList();
        }

        SyntaxTree syntaxTree = codeActionExecutionContext.currentDocument().syntaxTree();
        NonTerminalNode node = Utils.findNode(syntaxTree, lineRange);
        if (!(node instanceof ServiceDeclarationNode)) {
            return Collections.emptyList();
        }

        ServiceDeclarationNode serviceDeclarationNode = (ServiceDeclarationNode) node;

        List<TextEdit> textEdits = new ArrayList<>();
        TextRange resourceTextRange;
        if (serviceDeclarationNode.members().isEmpty()) {
            resourceTextRange = TextRange.from(serviceDeclarationNode.openBraceToken().textRange().endOffset(),
                    serviceDeclarationNode.closeBraceToken().textRange().startOffset() -
                            serviceDeclarationNode.openBraceToken().textRange().endOffset());
        } else {
            Node lastMember = serviceDeclarationNode.members().get(serviceDeclarationNode.members().size() - 1);
            resourceTextRange = TextRange.from(lastMember.textRange().endOffset(),
                    serviceDeclarationNode.closeBraceToken().textRange().startOffset() -
                            lastMember.textRange().endOffset());
        }
        textEdits.add(TextEdit.from(resourceTextRange,
                serviceDeclarationNode.members().size() > 0 ? serviceText + LS : serviceText));
        TextDocumentChange change = TextDocumentChange.from(textEdits.toArray(new TextEdit[0]));
        return Collections.singletonList(new DocumentEdit(codeActionExecutionContext.fileUri(),
                SyntaxTree.from(syntaxTree, change)));
    }
}
