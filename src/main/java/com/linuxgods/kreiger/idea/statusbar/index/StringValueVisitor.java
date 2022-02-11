package com.linuxgods.kreiger.idea.statusbar.index;

import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtil;
import com.siyeh.ig.psiutils.ExpressionUtils;
import com.siyeh.ig.psiutils.MethodCallUtils;
import com.siyeh.ig.psiutils.TypeUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.intellij.notification.NotificationType.INFORMATION;
import static com.intellij.psi.JavaTokenType.NULL_KEYWORD;
import static com.intellij.psi.PsiType.*;

public class StringValueVisitor extends JavaElementVisitor {
    private StringBuilder string;

    static Optional<String> valueToString(@Nullable PsiElement element) {
        StringValueVisitor visitor = new StringValueVisitor();
        visitor.visitElement(element);
        return Optional.ofNullable(visitor.string).map(StringBuilder::toString);
    }

    @Override public void visitElement(@Nullable PsiElement element) {
        if (null != element) {
            element.accept(this);
        }
    }

    @Override
    public void visitExpression(PsiExpression expression) {
        /*
        var notification = new Notification("Editor notifications",
                "Status bar index", "Unsupported expression " + expression, INFORMATION);
        Notifications.Bus.notify(notification, expression.getProject());
        */
    }

    @Override
    public void visitMethodCallExpression(PsiMethodCallExpression methodCallExpression) {
        PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();
        String methodName = methodExpression.getReferenceName();
        if (methodName == null) return;
        PsiExpression qualifierExpression = PsiUtil.skipParenthesizedExprDown(methodExpression.getQualifierExpression());
        PsiExpression[] arguments = methodCallExpression.getArgumentList().getExpressions();
        if (isToStringMethodCall(methodName, arguments)) {
            visitElement(qualifierExpression);
            return;
        }
        boolean stringValueOf = MethodCallUtils.isCallToStaticMethod(methodCallExpression, CommonClassNames.JAVA_LANG_STRING, "valueOf", 1);
        if (stringValueOf) {
            visitElement(arguments[0]);
            return;
        }
        if (qualifierExpression != null) {
            PsiType type = qualifierExpression.getType();
            if (TypeUtils.isJavaLangString(type)) {
                visitStringMethodCall( methodCallExpression);
                return;
            }
        }
        visitExpression(methodCallExpression);
    }

    private void visitStringMethodCall(PsiMethodCallExpression stringMethodCall) {
        PsiExpression[] arguments = stringMethodCall.getArgumentList().getExpressions();
        PsiReferenceExpression methodExpression = stringMethodCall.getMethodExpression();
        PsiExpression qualifier = methodExpression.getQualifierExpression();
        String methodName = methodExpression.getReferenceName();
        if ("concat".equals(methodName) && arguments.length == 1) {
            visitElement(qualifier);
            visitElement(arguments[0]);
            return;
        }
        if ("intern".equals(methodName)) {
            visitElement(qualifier);
            return;
        }
        if ("repeat".equals(methodName) && arguments.length == 1) {
            getLiteralInteger(arguments[0]).ifPresent(value -> {
                for (int i = 0; i < (int) value; i++) {
                    visitElement(qualifier);
                }
            });
        };
        if ("replace".equals(methodName) && arguments.length == 2
                && CHAR.equals(arguments[0].getType()) && CHAR.equals(arguments[1].getType())) {
            visitElement(qualifier);
            return;
        }
        // TODO
        if ("substring".equals(methodName)) {
            valueToString(qualifier)
                    .ifPresent(string -> {
                        getSubstringEndIndex(arguments, string.length())
                                .ifPresent(endIndex ->
                                        getLiteralInteger(arguments[0])
                                                .ifPresent(beginIndex ->
                                                        append(string.substring(beginIndex, endIndex))));
                    });
        }
    }

    private Optional<Integer> getSubstringEndIndex(PsiExpression[] arguments, int length) {
        return arguments.length == 2 ? getLiteralInteger(arguments[1]) : Optional.of(length);
    }

    private Optional<Object> getArgumentLiteralValue(PsiExpression argument) {
        if (argument instanceof PsiLiteralExpression) {
            return Optional.ofNullable(((PsiLiteralExpression) argument).getValue());
        }
        return Optional.empty();
    }
    private Optional<Integer> getLiteralInteger(PsiExpression expression) {
        return getArgumentLiteralValue(expression)
                .filter(value -> value instanceof Integer)
                .map(value -> (int)value);
    }


    private boolean isToStringMethodCall(@NonNls String methodName, PsiExpression[] arguments) {
        if ("toString".equals(methodName)) {
            return arguments.length == 0;
        }
        return false;
    }

    @Override public void visitReferenceExpression(PsiReferenceExpression expression) {
        visitElement(expression.resolve());
    }

    @Override public void visitLiteralExpression(PsiLiteralExpression literalExpression) {
        Object value = literalExpression.getValue();
        if (value != null || NULL_KEYWORD == getTokenType(literalExpression)) {
            append(value);
        }
    }

    private void append(Object value) {
        if (value == null) return;
        if (this.string == null) this.string = new StringBuilder();
        this.string.append(value);
    }

    @Override public void visitVariable(PsiVariable variable) {
        Object value = variable.computeConstantValue();
        if (value != null) {
            append(value);
            return;
        }
        visitElement(variable.getInitializer());
    }

    @Override public void visitParenthesizedExpression(PsiParenthesizedExpression parenthesizedExpression) {
        visitElement(PsiUtil.skipParenthesizedExprDown(parenthesizedExpression));
    }

    @Override public void visitPolyadicExpression(PsiPolyadicExpression polyadicExpression) {
        if (!ExpressionUtils.isStringConcatenation(polyadicExpression)) {
            Object value = JavaPsiFacade.getInstance(polyadicExpression.getProject()).getConstantEvaluationHelper().computeConstantExpression(polyadicExpression);
            append(value);
            return;
        }
        if (!JavaTokenType.PLUS.equals(polyadicExpression.getOperationTokenType())) {
            return;
        }
        for (PsiExpression operand : polyadicExpression.getOperands()) {
            visitElement(operand);
        }
    }

    private IElementType getTokenType(PsiLiteralExpression literalExpression) {
        PsiElement child = literalExpression.getFirstChild();
        if (child instanceof PsiJavaToken) {
            return ((PsiJavaToken) child).getTokenType();
        }
        return null;
    }

}
