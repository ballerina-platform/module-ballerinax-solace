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

import io.ballerina.compiler.api.symbols.ServiceDeclarationSymbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.AnnotationNode;
import io.ballerina.compiler.syntax.tree.MetadataNode;
import io.ballerina.compiler.syntax.tree.ObjectConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.ServiceDeclarationNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

import java.util.List;
import java.util.Optional;

/**
 * Identifies Solace services and validates their compile-time contract.
 */
final class SolaceServiceAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        if (context.node().kind() == SyntaxKind.OBJECT_CONSTRUCTOR) {
            validateObjectConstructor(context, (ObjectConstructorExpressionNode) context.node());
            return;
        }
        validateServiceDeclaration(context, (ServiceDeclarationNode) context.node());
    }

    private void validateObjectConstructor(SyntaxNodeAnalysisContext context, ObjectConstructorExpressionNode node) {
        Optional<TypeSymbol> targetType = PluginUtils.typeOf(context, node)
                .filter(type -> PluginUtils.isSolaceType(type, "Service"))
                .or(() -> PluginUtils.typeOfParentVariable(context, node)
                        .filter(type -> PluginUtils.isSolaceType(type, "Service")))
                .or(() -> PluginUtils.typeOfFunctionArgument(context, node)
                        .filter(type -> PluginUtils.isSolaceType(type, "Service")));
        if (targetType.isEmpty()) {
            return;
        }
        new ServiceValidator(context, node.members(), node.annotations().stream().toList(), node).validate();
    }

    private void validateServiceDeclaration(SyntaxNodeAnalysisContext context, ServiceDeclarationNode node) {
        boolean isSolaceService = context.semanticModel().symbol(node)
                .filter(symbol -> symbol instanceof ServiceDeclarationSymbol)
                .map(symbol -> (ServiceDeclarationSymbol) symbol)
                .map(service -> service.listenerTypes().stream()
                        .anyMatch(type -> PluginUtils.isSolaceType(type, "Listener")))
                .orElse(false);
        if (!isSolaceService) {
            return;
        }
        List<AnnotationNode> annotations = node.metadata().map(MetadataNode::annotations)
                .map(annotationNodes -> annotationNodes.stream().toList()).orElse(List.of());
        new ServiceValidator(context, node.members(), annotations, node).validate();
    }
}
