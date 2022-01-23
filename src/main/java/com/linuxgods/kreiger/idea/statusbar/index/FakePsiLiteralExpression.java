package com.linuxgods.kreiger.idea.statusbar.index;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

class FakePsiLiteralExpression extends FakePsiElement implements PsiLiteralExpression {
    private final PsiLiteralExpression literalExpression;
    private final String text;

    FakePsiLiteralExpression(PsiLiteralExpression literalExpression, String text) {
        this.literalExpression = literalExpression;
        this.text = text;
    }

    @Override public @Nullable @NonNls String getText() {
        return text;
    }

    @Override public @Nullable PsiType getType() {
        return literalExpression.getType();
    }

    @Override public @Nullable Object getValue() {
        return literalExpression.getValue();
    }

    @Override public PsiElement getParent() {
        return literalExpression.getParent();
    }

    @Override public boolean isTextBlock() {
        return literalExpression.isTextBlock();
    }
}
