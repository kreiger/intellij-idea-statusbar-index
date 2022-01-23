package com.linuxgods.kreiger.idea.statusbar.index;

import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtil;
import com.siyeh.ig.psiutils.ExpressionUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.intellij.psi.JavaTokenType.NULL_KEYWORD;

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
        visitExpression(variable.getInitializer());
    }

    @Override public void visitParenthesizedExpression(PsiParenthesizedExpression parenthesizedExpression) {
        visitExpression(PsiUtil.skipParenthesizedExprDown(parenthesizedExpression));
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
            visitExpression(operand);
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
