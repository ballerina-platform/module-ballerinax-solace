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
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.syntax.tree.ExpressionNode;
import io.ballerina.compiler.syntax.tree.FunctionArgumentNode;
import io.ballerina.compiler.syntax.tree.MappingConstructorExpressionNode;
import io.ballerina.compiler.syntax.tree.MappingFieldNode;
import io.ballerina.compiler.syntax.tree.NamedArgumentNode;
import io.ballerina.compiler.syntax.tree.PositionalArgumentNode;
import io.ballerina.compiler.syntax.tree.RemoteMethodCallActionNode;
import io.ballerina.compiler.syntax.tree.SpecificFieldNode;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;

import java.util.Optional;

/**
 * Validates that the messages sent with {@code MessageProducer->sendAwaitAcknowledgement} use the PERSISTENT delivery
 * mode, as the broker does not acknowledge DIRECT messages. Only message values given as mapping constructors can be
 * validated at compile time, hence the remaining ones are validated at runtime.
 */
final class SendAwaitAcknowledgementAnalysisTask implements AnalysisTask<SyntaxNodeAnalysisContext> {

    private static final String SEND_AWAIT_ACKNOWLEDGEMENT = "sendAwaitAcknowledgement";
    private static final String MESSAGE_PARAMETER = "message";
    private static final String DELIVERY_MODE_FIELD = "deliveryMode";
    private static final String DIRECT = "DIRECT";

    @Override
    public void perform(SyntaxNodeAnalysisContext context) {
        if (!PluginUtils.isSolaceImported(context)) {
            return;
        }
        RemoteMethodCallActionNode remoteCall = (RemoteMethodCallActionNode) context.node();
        if (!SEND_AWAIT_ACKNOWLEDGEMENT.equals(remoteCall.methodName().name().text()) ||
                !isSolaceProducerMethod(context, remoteCall)) {
            return;
        }
        Optional<ExpressionNode> message = messageArgument(remoteCall);
        if (message.isEmpty() || message.get().kind() != SyntaxKind.MAPPING_CONSTRUCTOR) {
            return;
        }
        validateDeliveryMode(context, (MappingConstructorExpressionNode) message.get());
    }

    private boolean isSolaceProducerMethod(SyntaxNodeAnalysisContext context, RemoteMethodCallActionNode remoteCall) {
        Optional<Symbol> symbol = context.semanticModel().symbol(remoteCall.methodName());
        if (symbol.isPresent() && symbol.get() instanceof MethodSymbol method &&
                PluginUtils.isSolaceModule(method.getModule())) {
            return true;
        }
        return PluginUtils.typeOf(context, remoteCall.expression())
                .map(type -> PluginUtils.isSolaceType(type, "MessageProducer")).orElse(false);
    }

    private Optional<ExpressionNode> messageArgument(RemoteMethodCallActionNode remoteCall) {
        for (FunctionArgumentNode argument : remoteCall.arguments()) {
            if (argument instanceof NamedArgumentNode namedArgument &&
                    MESSAGE_PARAMETER.equals(namedArgument.argumentName().name().text())) {
                return Optional.of(namedArgument.expression());
            }
        }
        if (!remoteCall.arguments().isEmpty() &&
                remoteCall.arguments().get(0) instanceof PositionalArgumentNode positionalArgument) {
            return Optional.of(positionalArgument.expression());
        }
        return Optional.empty();
    }

    private void validateDeliveryMode(SyntaxNodeAnalysisContext context, MappingConstructorExpressionNode message) {
        SpecificFieldNode deliveryModeField = null;
        for (MappingFieldNode field : message.fields()) {
            if (field.kind() == SyntaxKind.SPREAD_FIELD) {
                return;
            }
            if (field instanceof SpecificFieldNode specificField &&
                    DELIVERY_MODE_FIELD.equals(stripQuotes(specificField.fieldName().toSourceCode().trim()))) {
                deliveryModeField = specificField;
            }
        }
        // The delivery mode defaults to DIRECT when it is not specified
        if (deliveryModeField == null) {
            context.reportDiagnostic(PluginUtils.diagnostic(DiagnosticCode.NON_PERSISTENT_AWAITED_MESSAGE,
                    message.location()));
            return;
        }
        Optional<ExpressionNode> deliveryMode = deliveryModeField.valueExpr();
        if (deliveryMode.isPresent() && isDirect(context, deliveryMode.get())) {
            context.reportDiagnostic(PluginUtils.diagnostic(DiagnosticCode.NON_PERSISTENT_AWAITED_MESSAGE,
                    deliveryMode.get().location()));
        }
    }

    private boolean isDirect(SyntaxNodeAnalysisContext context, ExpressionNode deliveryMode) {
        if (deliveryMode.kind() == SyntaxKind.STRING_LITERAL) {
            return DIRECT.equals(stripQuotes(deliveryMode.toSourceCode().trim()));
        }
        Optional<Symbol> symbol = context.semanticModel().symbol(deliveryMode);
        if (symbol.isPresent() && symbol.get() instanceof ConstantSymbol constant) {
            return constant.resolvedValue().map(this::stripQuotes).map(DIRECT::equals).orElse(false);
        }
        return false;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
