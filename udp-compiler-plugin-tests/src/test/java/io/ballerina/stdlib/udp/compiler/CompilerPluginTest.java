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

import io.ballerina.projects.DiagnosticResult;
import io.ballerina.projects.Package;
import io.ballerina.projects.PackageCompilation;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.directory.BuildProject;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import io.ballerina.tools.diagnostics.Diagnostic;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This class includes tests for Ballerina UDP compiler plugin.
 */
public class CompilerPluginTest {

    private static final Path RESOURCE_DIRECTORY = Paths.get("src", "test", "resources", "ballerina_sources")
            .toAbsolutePath();
    private static final Path DISTRIBUTION_PATH = Paths.get("build", "target", "ballerina-distribution")
            .toAbsolutePath();

    @Test
    public void testServiceWithOnDatagramAndOnBytes() {
        Package currentPackage = loadPackage("sample_package_1");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                UdpServiceValidator.SERVICE_CANNOT_CONTAIN_BOTH_ON_DATAGRAM_0_AND_ON_BYTES_1_FUNCTIONS);
    }

    @Test
    public void testServiceWithoutOnDatagramAndOnBytes() {
        Package currentPackage = loadPackage("sample_package_2");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                UdpServiceValidator.SERVICE_DOES_NOT_CONTAIN_ON_DATAGRAM_OR_ON_BYTES_FUNCTION);
    }

    @Test
    public void testRemoteFunctionsWithoutRemoteKeyword() {
        Package currentPackage = loadPackage("sample_package_3");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 3);
        for (Diagnostic diagnostic : diagnosticResult.diagnostics()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    UdpServiceValidator.REMOTE_KEYWORD_EXPECTED_IN_0_FUNCTION_SIGNATURE);
        }
    }

    @Test
    public void testRemoteFunctionsWithoutParameters() {
        Package currentPackage = loadPackage("sample_package_4");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 3);
        for (Diagnostic diagnostic : diagnosticResult.diagnostics()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    UdpServiceValidator.NO_PARAMETER_PROVIDED_FOR_0_FUNCTION_EXPECTS_1_AS_A_PARAMETER);
        }
    }

    @Test(description = "test onBytes and onDatagram function with invalid parameter type")
    public void testReadFunctionsWithInvalidParameters() {
        Package currentPackage = loadPackage("sample_package_5");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 4);
        for (Diagnostic diagnostic : diagnosticResult.diagnostics()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    UdpServiceValidator.INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION);
        }
    }

    @Test
    public void testOnErrorFunctionWithInvalidParameter() {
        Package currentPackage = loadPackage("sample_package_6");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                UdpServiceValidator.INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2);
    }

    @Test(description = "test onBytes and onDatagram function with byte[] and udp:Datagram parameters," +
            " without readonly intersection")
    public void testReadFunctionsWithoutReadonlyParameters() {
        Package currentPackage = loadPackage("sample_package_7");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 2);
        for (Diagnostic diagnostic : diagnosticResult.diagnostics()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    UdpServiceValidator.INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2);
        }
    }

    @Test
    public void testValidService() {
        Package currentPackage = loadPackage("sample_package_8");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 0);
    }

    @Test
    public void testWithAliasModuleNamePrefix() {
        Package currentPackage = loadPackage("sample_package_9");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 1);
        Diagnostic diagnostic = (Diagnostic) diagnosticResult.diagnostics().toArray()[0];
        Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                UdpServiceValidator.INVALID_PARAMETER_0_PROVIDED_FOR_1_FUNCTION_EXPECTS_2);
    }

    @Test
    public void testWithOtherModuleServices() {
        Package currentPackage = loadPackage("sample_package_10");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 0);
    }

    @Test
    public void testWithInvalidReturnType() {
        Package currentPackage = loadPackage("sample_package_11");
        PackageCompilation compilation = currentPackage.getCompilation();
        DiagnosticResult diagnosticResult = compilation.diagnosticResult();
        Assert.assertEquals(diagnosticResult.diagnostics().size(), 3);
        for (Diagnostic diagnostic : diagnosticResult.diagnostics()) {
            Assert.assertEquals(diagnostic.diagnosticInfo().messageFormat(),
                    UdpServiceValidator.INVALID_RETURN_TYPE_0_FUNCTION_1_RETURN_TYPE_SHOULD_BE_A_SUBTYPE_OF_2);
        }
    }

    private Package loadPackage(String path) {
        Path projectDirPath = RESOURCE_DIRECTORY.resolve(path);
        BuildProject project = BuildProject.load(getEnvironmentBuilder(), projectDirPath);
        return project.currentPackage();
    }

    private static ProjectEnvironmentBuilder getEnvironmentBuilder() {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        return ProjectEnvironmentBuilder.getBuilder(environment);
    }
}
