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

import io.ballerina.compiler.api.symbols.FunctionTypeSymbol;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Qualifier;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.UnionTypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionDefinitionNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Validates the compile-time shape of a Solace service.
 */
final class ServiceValidator {

    private final SyntaxNodeAnalysisContext context;
    private final NodeList<Node> members;
    private final List<AnnotationNode> annotations;
    private final Node serviceNode;

    ServiceValidator(SyntaxNodeAnalysisContext context, NodeList<Node> members,
                     List<AnnotationNode> annotations, Node serviceNode) {
        this.context = context;
        this.members = members;
        this.annotations = annotations;
        this.serviceNode = serviceNode;
    }

    void validate() {
        if (PluginUtils.findServiceConfig(context.semanticModel(), annotations).isEmpty()) {
            report(DiagnosticCode.MISSING_SERVICE_CONFIG, serviceNode);
        }

        List<FunctionDefinitionNode> remoteMethods = new ArrayList<>();
        for (Node member : members) {
            if (member.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
                report(DiagnosticCode.RESOURCE_METHOD, member);
                continue;
            }
            if (member.kind() != SyntaxKind.OBJECT_METHOD_DEFINITION) {
                continue;
            }
            FunctionDefinitionNode function = (FunctionDefinitionNode) member;
            methodSymbol(function).filter(method -> method.qualifiers().contains(Qualifier.REMOTE))
                    .ifPresent(method -> remoteMethods.add(function));
        }

        List<FunctionDefinitionNode> onMessages = named(remoteMethods, "onMessage");
        List<FunctionDefinitionNode> onErrors = named(remoteMethods, "onError");
        remoteMethods.stream().filter(method -> !method.functionName().text().equals("onMessage") &&
                        !method.functionName().text().equals("onError"))
                .forEach(method -> report(DiagnosticCode.UNSUPPORTED_REMOTE_METHOD, method,
                        method.functionName().text()));

        if (onMessages.size() != 1 || onErrors.size() > 1 || remoteMethods.size() != onMessages.size() +
                onErrors.size()) {
            report(DiagnosticCode.INVALID_REMOTE_METHOD_SET, serviceNode);
        }
        if (onMessages.size() == 1) {
            validateOnMessage(onMessages.get(0));
        }
        if (onErrors.size() == 1) {
            validateOnError(onErrors.get(0));
        }
        remoteMethods.forEach(this::validateReturnType);
    }

    private void validateOnMessage(FunctionDefinitionNode function) {
        List<ParameterSymbol> parameters = parameters(function);
        if (hasRestParameter(function) || parameters.size() < 1 || parameters.size() > 2 || parameters.stream()
                .anyMatch(parameter -> parameter.paramKind() != ParameterKind.REQUIRED)) {
            report(DiagnosticCode.INVALID_ON_MESSAGE_PARAMETERS, function);
            return;
        }
        ParameterSymbol message = parameters.get(0);
        if (!PluginUtils.isMessageType(message.typeDescriptor())) {
            if (PluginUtils.isSolaceType(message.typeDescriptor(), "Caller")) {
                report(DiagnosticCode.INVALID_CALLER_PARAMETER, function);
            } else {
                report(DiagnosticCode.INVALID_MESSAGE_PARAMETER, function, message.getName().orElse("message"));
            }
        }
        if (parameters.size() == 2 && !PluginUtils.isSolaceType(parameters.get(1).typeDescriptor(), "Caller")) {
            report(DiagnosticCode.INVALID_CALLER_PARAMETER, function);
        }
    }

    private void validateOnError(FunctionDefinitionNode function) {
        List<ParameterSymbol> parameters = parameters(function);
        if (hasRestParameter(function) || parameters.size() != 1 ||
                parameters.get(0).paramKind() != ParameterKind.REQUIRED ||
                !PluginUtils.isSolaceType(parameters.get(0).typeDescriptor(), "Error")) {
            report(DiagnosticCode.INVALID_ON_ERROR_PARAMETER, function);
        }
    }

    private void validateReturnType(FunctionDefinitionNode function) {
        methodSymbol(function).map(MethodSymbol::typeDescriptor)
                .flatMap(FunctionTypeSymbol::returnTypeDescriptor)
                .filter(type -> !isErrorOrNil(type))
                .ifPresent(type -> report(DiagnosticCode.INVALID_RETURN_TYPE, function,
                        function.functionName().text()));
    }

    private boolean isErrorOrNil(TypeSymbol type) {
        TypeSymbol rawType = PluginUtils.rawType(type);
        if (rawType.typeKind() == TypeDescKind.NIL || rawType.typeKind() == TypeDescKind.ERROR ||
                PluginUtils.isSolaceType(type, "Error")) {
            return true;
        }
        return rawType instanceof UnionTypeSymbol union && union.memberTypeDescriptors().stream()
                .allMatch(this::isErrorOrNil);
    }

    private List<ParameterSymbol> parameters(FunctionDefinitionNode function) {
        return methodSymbol(function).map(MethodSymbol::typeDescriptor)
                .flatMap(FunctionTypeSymbol::params).orElse(List.of());
    }

    private boolean hasRestParameter(FunctionDefinitionNode function) {
        return methodSymbol(function).map(MethodSymbol::typeDescriptor)
                .flatMap(FunctionTypeSymbol::restParam).isPresent();
    }

    private Optional<MethodSymbol> methodSymbol(FunctionDefinitionNode function) {
        return context.semanticModel().symbol(function)
                .filter(symbol -> symbol instanceof MethodSymbol)
                .map(symbol -> (MethodSymbol) symbol);
    }

    private List<FunctionDefinitionNode> named(List<FunctionDefinitionNode> functions, String name) {
        return functions.stream().filter(function -> name.equals(function.functionName().text())).toList();
    }

    private void report(DiagnosticCode code, Node node, Object... args) {
        context.reportDiagnostic(PluginUtils.diagnostic(code, node.location(), args));
    }
}
