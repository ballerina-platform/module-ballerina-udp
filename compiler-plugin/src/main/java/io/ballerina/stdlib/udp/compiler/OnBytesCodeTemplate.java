/*
 * Copyright (c) 2022 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.stdlib.udp.compiler;

import io.ballerina.projects.plugins.codeaction.CodeAction;
import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionExecutionContext;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.projects.plugins.codeaction.DocumentEdit;
import io.ballerina.tools.diagnostics.Diagnostic;

import java.util.List;
import java.util.Optional;

import static io.ballerina.stdlib.udp.compiler.UdpServiceValidator.CODE_107;
import static io.ballerina.stdlib.udp.compiler.Utils.LS;

/**
 * Code action to add onBytes code snippet.
 */
public class OnBytesCodeTemplate implements CodeAction {

    public static final String SERVICE_TEXT = LS +
            "\tremote function onBytes(readonly & byte[] data) " +
            "returns byte[]|udp:Error? {"  + LS + LS +
            "\t}" + LS;

    @Override
    public List<String> supportedDiagnosticCodes() {
        return List.of(CODE_107);
    }

    @Override
    public Optional<CodeActionInfo> codeActionInfo(CodeActionContext codeActionContext) {
        Diagnostic diagnostic = codeActionContext.diagnostic();
        if (diagnostic.location() == null) {
            return Optional.empty();
        }
        CodeActionArgument locationArg = CodeActionArgument.from(Utils.NODE_LOCATION,
                diagnostic.location().lineRange());
        return Optional.of(CodeActionInfo.from("Add onBytes remote function", List.of(locationArg)));
    }

    @Override
    public List<DocumentEdit> execute(CodeActionExecutionContext codeActionExecutionContext) {
        return Utils.getDocumentEdits(codeActionExecutionContext, SERVICE_TEXT);
    }

    @Override
    public String name() {
        return "ADD_ON_BYTES_CODE_SNIPPET";
    }
}
