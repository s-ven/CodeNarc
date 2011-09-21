/*
 * Copyright 2011 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.rule.unnecessary

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codenarc.rule.AbstractAstVisitor
import org.codenarc.rule.AbstractAstVisitorRule
import org.codenarc.util.AstUtil

/**
 * If a parent class defines a method that follows the Java getter notation, and a subclass overrides that method to
 * return a constant, then it is cleaner to provide a Groovy property for the value rather than a Groovy method.
 *
 * @author Hamlet D'Arcy
 */
class UnnecessaryOverridingGetterRule extends AbstractAstVisitorRule {
    String name = 'UnnecessaryOverridingGetter'
    int priority = 2
    Class astVisitorClass = UnnecessaryOverridingGetterAstVisitor
}

class UnnecessaryOverridingGetterAstVisitor extends AbstractAstVisitor {

    private staticFieldNames = []

    @Override
    protected void visitClassEx(ClassNode node) {
        staticFieldNames.addAll(node.fields.findAll { it.isStatic() }*.name)
        super.visitClassEx(node)
    }

    @Override
    protected void visitClassComplete(ClassNode node) {
        staticFieldNames.clear()
        super.visitClassComplete(node)
    }

    @Override
    protected void visitMethodEx(MethodNode node) {
        if (AstUtil.isMethodNode(node, 'get[A-Z].*', 0) && !node.isStatic() && AstUtil.isOneLiner(node.code)) {

            def statement = node.code.statements[0]
            if (statement instanceof ExpressionStatement) {
                if (statement.expression instanceof ConstantExpression) {
                    addViolation(node, createMessage(node))
                } else if (statement.expression instanceof VariableExpression && statement.expression.variable ==~ /[A-Z].*/) {
                    addViolation(node, createMessage(node))
                } else if (statement.expression instanceof VariableExpression && staticFieldNames.contains(statement.expression.name)) {
                    addViolation(node, createMessage(node))
                }
            } else if (statement instanceof ReturnStatement) {
                if (statement.expression instanceof ConstantExpression) {
                    addViolation(node, createMessage(node))
                } else if (statement.expression instanceof ClassExpression) {
                    addViolation(node, createMessage(node))
                } else if (statement.expression instanceof VariableExpression && staticFieldNames.contains(statement.expression.name)) {
                    addViolation(node, createMessage(node))
                } 
            } 
        }
        super.visitMethodEx(node)
    }

    private String createMessage(MethodNode node) {
        def constant = node.code.statements[0].expression
        def methodName = node.name
        def methodType = node.returnType.nameWithoutPackage
        def propertyName = node.name[3].toLowerCase() + (node.name.length() == 4 ? '' : node.name[4..-1])

        def constantValue = null
        if (constant instanceof ConstantExpression) {
            constantValue = constant.value instanceof String ? "'" + constant.value + "'" : constant.value
        } else if (constant instanceof ClassExpression) {
            constantValue = constant.text
        } else if (constant instanceof VariableExpression) {
            constantValue = constant.name
        } else {
            constantValue = '<UNKNOWN>'
        }

        "The method '$methodName ' in class $currentClassName can be expressed more simply as the field declaration\n" +
            "final $methodType $propertyName = $constantValue"
    }
}
