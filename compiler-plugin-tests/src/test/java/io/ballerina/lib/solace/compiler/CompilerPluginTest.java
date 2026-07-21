/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ballerina.lib.solace.compiler;

import io.ballerina.projects.DiagnosticResult;
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
import java.util.List;

/**
 * Tests the Solace compiler plugin.
 */
public class CompilerPluginTest {

    private static final Path RESOURCE_DIRECTORY =
            Paths.get("src", "test", "resources", "ballerina_sources").toAbsolutePath();
    private static final Path DISTRIBUTION_PATH = Paths.get("..", "target", "ballerina-runtime").toAbsolutePath();

    @Test
    public void testMissingServiceConfigAnnotation() {
        DiagnosticResult result = compile("missing_service_config").diagnosticResult();
        Assert.assertEquals(result.errorCount(), 1);
        Diagnostic diagnostic = result.errors().iterator().next();
        Assert.assertEquals(diagnostic.diagnosticInfo().code(), "SOLACE_101");
        Assert.assertEquals(diagnostic.message(), "service must have the 'solace:ServiceConfig' annotation");
        Assert.assertEquals(diagnostic.location().lineRange().toString(), "(2:32,5:1)");
    }

    @Test
    public void testServiceValidationDiagnostics() {
        DiagnosticResult result = compile("service_validation").diagnosticResult();
        List<Diagnostic> errors = result.errors().stream().toList();
        Assert.assertEquals(errors.stream().map(diagnostic -> diagnostic.diagnosticInfo().code()).toList(),
                List.of("SOLACE_101", "SOLACE_102", "SOLACE_103", "SOLACE_104", "SOLACE_103", "SOLACE_105",
                        "SOLACE_106", "SOLACE_107", "SOLACE_108", "SOLACE_109"));
        Assert.assertEquals(errors.get(7).message(),
                "the second parameter (optional) must be of type 'solace:Caller'");
    }

    @Test
    public void testServiceDeclarationValidation() {
        DiagnosticResult result = compile("service_declaration").diagnosticResult();
        Assert.assertEquals(result.errorCount(), 1);
        Assert.assertEquals(result.errors().iterator().next().diagnosticInfo().code(), "SOLACE_101");
    }

    @Test
    public void testDirectServiceObjectValidation() {
        DiagnosticResult result = compile("direct_service_object").diagnosticResult();
        Assert.assertEquals(result.errorCount(), 1);
        Assert.assertEquals(result.errors().iterator().next().diagnosticInfo().code(), "SOLACE_101");
    }

    @Test
    public void testRestParameterValidation() {
        DiagnosticResult result = compile("rest_parameter_validation").diagnosticResult();
        List<String> codes = result.errors().stream().map(diagnostic -> diagnostic.diagnosticInfo().code()).toList();
        Assert.assertEquals(codes, List.of("SOLACE_105", "SOLACE_108"));
    }

    @Test
    public void testAliasedServiceValidation() {
        DiagnosticResult result = compile("aliased_service_validation").diagnosticResult();
        List<String> codes = result.errors().stream().map(diagnostic -> diagnostic.diagnosticInfo().code()).toList();
        Assert.assertEquals(codes, List.of("SOLACE_101", "SOLACE_101"));
    }

    @Test
    public void testConfigurationValidationDiagnostics() {
        DiagnosticResult result = compile("configuration_validation").diagnosticResult();
        List<String> codes = result.errors().stream().map(diagnostic -> diagnostic.diagnosticInfo().code()).toList();
        Assert.assertEquals(codes,
                List.of("SOLACE_201", "SOLACE_201", "SOLACE_201", "SOLACE_202", "SOLACE_202", "SOLACE_202"),
                codes.toString());
        List<String> messages = result.errors().stream().map(Diagnostic::message).toList();
        Assert.assertEquals(messages,
                List.of("queueName is required when the queue is DURABLE",
                        "queueName is required when the queue is DURABLE",
                        "queueName is required when the queue is DURABLE",
                        "endpointName is required when the topic is DURABLE",
                        "endpointName is required when the topic is DURABLE",
                        "endpointName is required when the topic is DURABLE"));
    }

    private PackageCompilation compile(String fixture) {
        Environment environment = EnvironmentBuilder.getBuilder().setBallerinaHome(DISTRIBUTION_PATH).build();
        ProjectEnvironmentBuilder environmentBuilder = ProjectEnvironmentBuilder.getBuilder(environment);
        BuildProject project = BuildProject.load(environmentBuilder, RESOURCE_DIRECTORY.resolve(fixture));
        return project.currentPackage().getCompilation();
    }
}
