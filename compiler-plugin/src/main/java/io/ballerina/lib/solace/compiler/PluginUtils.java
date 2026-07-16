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

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.AnnotationSymbol;
import io.ballerina.compiler.api.symbols.FunctionSymbol;
import io.ballerina.compiler.api.symbols.IntersectionTypeSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.RecordTypeSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeReferenceTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.api.symbols.VariableSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.FunctionCallExpressionNode;
import io.ballerina.compiler.syntax.tree.MethodCallExpressionNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.tools.diagnostics.Diagnostic;
import io.ballerina.tools.diagnostics.DiagnosticFactory;
import io.ballerina.tools.diagnostics.DiagnosticInfo;
import io.ballerina.tools.diagnostics.DiagnosticSeverity;
import io.ballerina.tools.diagnostics.Location;

import java.util.List;
import java.util.Optional;

/**
 * Shared compiler plugin utilities.
 */
final class PluginUtils {

    static final String PACKAGE_ORG = "ballerinax";
    static final String PACKAGE_NAME = "solace";

    private PluginUtils() {
    }

    static Diagnostic diagnostic(DiagnosticCode code, Location location, Object... args) {
        DiagnosticInfo info = new DiagnosticInfo(code.code(), code.message(), DiagnosticSeverity.ERROR);
        return DiagnosticFactory.createDiagnostic(info, location, args);
    }

    static boolean isSolaceType(TypeSymbol type, String typeName) {
        if (type instanceof TypeReferenceTypeSymbol typeReference) {
            return typeName.equals(typeReference.getName().orElse(typeReference.name())) &&
                    isSolaceModule(typeReference.getModule());
        }
        if (type instanceof IntersectionTypeSymbol intersection) {
            return isSolaceType(intersection.effectiveTypeDescriptor(), typeName);
        }
        return false;
    }

    static boolean isMessageType(TypeSymbol type) {
        if (isSolaceType(type, "Message")) {
            return true;
        }
        TypeSymbol rawType = rawType(type);
        if (!(rawType instanceof RecordTypeSymbol recordType)) {
            return false;
        }
        return recordType.typeInclusions().stream().anyMatch(inclusion -> isMessageType(inclusion));
    }

    static TypeSymbol rawType(TypeSymbol type) {
        TypeSymbol current = type;
        if (current instanceof TypeReferenceTypeSymbol typeReference) {
            current = typeReference.typeDescriptor();
        }
        if (current instanceof IntersectionTypeSymbol intersection) {
            current = intersection.effectiveTypeDescriptor();
        }
        return current;
    }

    static Optional<AnnotationNode> findServiceConfig(SemanticModel semanticModel,
                                                       List<AnnotationNode> annotations) {
        return annotations.stream().filter(annotation -> semanticModel.symbol(annotation.annotReference())
                        .filter(symbol -> symbol instanceof AnnotationSymbol)
                        .map(symbol -> (AnnotationSymbol) symbol)
                        .filter(symbol -> "ServiceConfig".equals(symbol.getName().orElse("")))
                        .filter(symbol -> isSolaceModule(symbol.getModule()))
                        .isPresent())
                .findFirst();
    }

    static boolean isSolaceModule(Optional<ModuleSymbol> module) {
        return module.map(moduleSymbol -> PACKAGE_ORG.equals(moduleSymbol.id().orgName()) &&
                PACKAGE_NAME.equals(moduleSymbol.id().moduleName())).orElse(false);
    }

    static Optional<TypeSymbol> typeOfParentVariable(SyntaxNodeAnalysisContext context, Node node) {
        Node parent = node.parent();
        while (parent != null) {
            Optional<Symbol> symbol = context.semanticModel().symbol(parent);
            if (symbol.isPresent() && symbol.get() instanceof VariableSymbol variable) {
                return Optional.of(variable.typeDescriptor());
            }
            parent = parent.parent();
        }
        return Optional.empty();
    }

    static Optional<TypeSymbol> typeOf(SyntaxNodeAnalysisContext context, Node node) {
        return context.semanticModel().typeOf(node);
    }

    static Optional<TypeSymbol> typeOfFunctionArgument(SyntaxNodeAnalysisContext context, Node node) {
        if (!(node.parent() instanceof FunctionArgumentNode argument)) {
            return Optional.empty();
        }

        Node invocation = argument.parent();
        List<FunctionArgumentNode> arguments;
        Optional<Symbol> symbol;
        if (invocation instanceof FunctionCallExpressionNode functionCall) {
            arguments = functionCall.arguments().stream().toList();
            symbol = context.semanticModel().symbol(functionCall.functionName());
        } else if (invocation instanceof MethodCallExpressionNode methodCall) {
            arguments = methodCall.arguments().stream().toList();
            symbol = context.semanticModel().symbol(methodCall.methodName());
        } else {
            return Optional.empty();
        }

        if (symbol.isEmpty() || !(symbol.get() instanceof FunctionSymbol function)) {
            return Optional.empty();
        }
        List<ParameterSymbol> parameters = function.typeDescriptor().params().orElse(List.of());
        if (argument instanceof NamedArgumentNode namedArgument) {
            String argumentName = namedArgument.argumentName().name().text();
            return parameters.stream().filter(parameter -> argumentName.equals(parameter.getName().orElse("")))
                    .map(ParameterSymbol::typeDescriptor).findFirst();
        }

        int argumentIndex = arguments.indexOf(argument);
        if (argumentIndex >= 0 && argumentIndex < parameters.size()) {
            return Optional.of(parameters.get(argumentIndex).typeDescriptor());
        }
        return function.typeDescriptor().restParam().map(ParameterSymbol::typeDescriptor);
    }
}
