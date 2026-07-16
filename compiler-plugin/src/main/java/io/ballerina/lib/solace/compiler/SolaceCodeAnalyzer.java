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

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.projects.plugins.CodeAnalysisContext;
import io.ballerina.projects.plugins.CodeAnalyzer;

import java.util.List;

/**
 * Registers compile-time validations for the Solace module.
 */
public class SolaceCodeAnalyzer extends CodeAnalyzer {

    @Override
    public void init(CodeAnalysisContext context) {
        context.addSyntaxNodeAnalysisTask(new SolaceServiceAnalysisTask(),
                List.of(SyntaxKind.SERVICE_DECLARATION, SyntaxKind.OBJECT_CONSTRUCTOR));
        context.addSyntaxNodeAnalysisTask(new ConfigurationAnalysisTask(), SyntaxKind.MAPPING_CONSTRUCTOR);
    }
}
