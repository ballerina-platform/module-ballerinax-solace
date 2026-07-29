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

import io.ballerina.compiler.api.symbols.ConstantSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Validates statically known queue and topic configurations.
 */
final class ConfigurationAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        MappingConstructorExpressionNode mapping = (MappingConstructorExpressionNode) context.node();
        Optional<TypeSymbol> expectedType = expectedType(context, mapping);
        Optional<TypeSymbol> actualType = context.semanticModel().typeOf(mapping);
        TypeSymbol configurationType = selectConfigurationType(expectedType, actualType).orElse(null);
        if (configurationType == null) {
            return;
        }

        Map<String, ExpressionNode> fields = new HashMap<>();
        for (MappingFieldNode field : mapping.fields()) {
            if (field.kind() == SyntaxKind.SPREAD_FIELD) {
                return;
            }
            if (field instanceof SpecificFieldNode specificField) {
                fields.put(fieldName(specificField), specificField.valueExpr().orElse(null));
            }
        }

        boolean topic = PluginUtils.isSolaceType(configurationType, "TopicConfiguration") ||
                PluginUtils.isSolaceType(configurationType, "TopicServiceConfiguration") ||
                fields.containsKey("topicName");
        if (topic) {
            validateTopic(context, mapping, fields);
        } else {
            validateQueue(context, mapping, fields);
        }
    }

    private void validateQueue(SyntaxNodeAnalysisContext context, MappingConstructorExpressionNode mapping,
                               Map<String, ExpressionNode> fields) {
        Optional<String> durability = value(context, fields.get("durability"));
        if (fields.containsKey("durability") && durability.isEmpty()) {
            return;
        }
        if (durability.orElse("DURABLE").equals("TEMPORARY")) {
            return;
        }
        if (isMissingOrEmpty(context, fields, "queueName")) {
            context.reportDiagnostic(PluginUtils.diagnostic(DiagnosticCode.MISSING_QUEUE_NAME, mapping.location()));
        }
    }

    private void validateTopic(SyntaxNodeAnalysisContext context, MappingConstructorExpressionNode mapping,
                               Map<String, ExpressionNode> fields) {
        Optional<String> durability = value(context, fields.get("durability"));
        if (!fields.containsKey("durability") || durability.isEmpty() || !durability.get().equals("DURABLE")) {
            return;
        }
        if (isMissingOrEmpty(context, fields, "endpointName")) {
            context.reportDiagnostic(PluginUtils.diagnostic(DiagnosticCode.MISSING_ENDPOINT_NAME, mapping.location()));
        }
    }

    private boolean isMissingOrEmpty(SyntaxNodeAnalysisContext context, Map<String, ExpressionNode> fields,
                                     String fieldName) {
        if (!fields.containsKey(fieldName)) {
            return true;
        }
        Optional<String> value = value(context, fields.get(fieldName));
        return value.isPresent() && value.get().isEmpty();
    }

    private Optional<TypeSymbol> selectConfigurationType(Optional<TypeSymbol> expected, Optional<TypeSymbol> actual) {
        return expected.filter(this::isConfigurationType).or(() -> actual.filter(this::isConfigurationType));
    }

    private boolean isConfigurationType(TypeSymbol type) {
        return PluginUtils.isSolaceType(type, "QueueConfiguration") ||
                PluginUtils.isSolaceType(type, "TopicConfiguration") ||
                PluginUtils.isSolaceType(type, "SubscriptionConfiguration") ||
                PluginUtils.isSolaceType(type, "QueueServiceConfiguration") ||
                PluginUtils.isSolaceType(type, "TopicServiceConfiguration") ||
                PluginUtils.isSolaceType(type, "ServiceConfiguration");
    }

    private Optional<TypeSymbol> expectedType(SyntaxNodeAnalysisContext context,
                                              MappingConstructorExpressionNode mapping) {
        Document document = context.currentPackage().module(context.moduleId()).document(context.documentId());
        return context.semanticModel().expectedType(document, mapping.location().lineRange().startLine());
    }

    private Optional<String> value(SyntaxNodeAnalysisContext context, ExpressionNode expression) {
        if (expression == null) {
            return Optional.empty();
        }
        String source = expression.toSourceCode().trim();
        if (expression.kind() == SyntaxKind.STRING_LITERAL) {
            return Optional.of(source.substring(1, source.length() - 1));
        }
        Optional<Symbol> symbol = context.semanticModel().symbol(expression);
        if (symbol.isPresent() && symbol.get() instanceof ConstantSymbol constant) {
            return constant.resolvedValue().map(this::stripQuotes);
        }
        if (source.equals("DURABLE") || source.endsWith(":DURABLE")) {
            return Optional.of("DURABLE");
        }
        if (source.equals("TEMPORARY") || source.endsWith(":TEMPORARY")) {
            return Optional.of("TEMPORARY");
        }
        return Optional.empty();
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private String fieldName(SpecificFieldNode field) {
        return stripQuotes(field.fieldName().toSourceCode().trim());
    }
}
