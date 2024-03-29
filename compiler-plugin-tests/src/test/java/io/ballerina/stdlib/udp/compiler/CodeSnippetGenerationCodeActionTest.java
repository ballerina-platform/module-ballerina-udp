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

import io.ballerina.projects.plugins.codeaction.CodeActionArgument;
import io.ballerina.projects.plugins.codeaction.CodeActionInfo;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static io.ballerina.stdlib.udp.compiler.Utils.NODE_LOCATION;

/**
 * A class for testing code actions.
 */
public class CodeSnippetGenerationCodeActionTest extends AbstractCodeActionTest {
    @Test(dataProvider = "testDataProvider")
    public void testCodeActions(String srcFile, int line, int offset, CodeActionInfo expected, String resultFile)
            throws IOException {
        Path filePath = RESOURCE_PATH.resolve("ballerina_sources")
                .resolve("sample_package_14")
                .resolve(srcFile);
        Path resultPath = RESOURCE_PATH.resolve("codeaction")
                .resolve(getConfigDir())
                .resolve(resultFile);

        performTest(filePath, LinePosition.from(line, offset), expected, resultPath);
    }

    @DataProvider
    private Object[][] testDataProvider() {
        return new Object[][]{
                {"service.bal", 2, 8, getOnDatagramCodeAction(), "onDatagram.bal"},
                {"service.bal", 2, 8, getOnBytesCodeAction(), "onBytes.bal"}
        };
    }

    private CodeActionInfo getOnDatagramCodeAction() {
        LineRange lineRange = LineRange.from("service.bal", LinePosition.from(2, 0),
                LinePosition.from(3, 1));
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, lineRange);
        CodeActionInfo codeAction = CodeActionInfo.from("Add onDatagram remote function", List.of(locationArg));
        codeAction.setProviderName("UDP_106/ballerina/udp/ADD_ON_DATAGRAM_CODE_SNIPPET");
        return codeAction;
    }

    private CodeActionInfo getOnBytesCodeAction() {
        LineRange lineRange = LineRange.from("service.bal", LinePosition.from(2, 0),
                LinePosition.from(3, 1));
        CodeActionArgument locationArg = CodeActionArgument.from(NODE_LOCATION, lineRange);
        CodeActionInfo codeAction = CodeActionInfo.from("Add onBytes remote function", List.of(locationArg));
        codeAction.setProviderName("UDP_107/ballerina/udp/ADD_ON_BYTES_CODE_SNIPPET");
        return codeAction;
    }

    protected String getConfigDir() {
        return "code_snippet_generation";
    }
}
