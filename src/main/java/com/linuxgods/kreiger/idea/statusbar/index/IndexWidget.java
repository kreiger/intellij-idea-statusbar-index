package com.linuxgods.kreiger.idea.statusbar.index;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedWidget;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiLiteralUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.Consumer;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

import static com.intellij.codeInsight.CodeInsightUtilCore.parseStringCharacters;
import static com.linuxgods.kreiger.idea.statusbar.index.StringValueVisitor.valueToString;

public class IndexWidget extends EditorBasedWidget implements CaretListener, StatusBarWidget.TextPresentation, @NotNull DocumentListener {
    private String text = "";

    protected IndexWidget(@NotNull Project project) {
        super(project);
    }

    @Override public @NonNls @NotNull String ID() {
        return "Index";
    }

    @Override public void install(@NotNull StatusBar statusBar) {
        super.install(statusBar);
        EditorEventMulticaster multicaster = EditorFactory.getInstance().getEventMulticaster();
        multicaster.addCaretListener(this, this);
        multicaster.addDocumentListener(this, this);

    }

    @Override public @Nullable WidgetPresentation getPresentation() {
        return this;
    }

    @Override public void caretPositionChanged(@NotNull CaretEvent event) {
        CaretListener.super.caretPositionChanged(event);

        update(getText(event.getEditor()));
    }

    private String getText(Editor editor) {
        DumbService dumbService = DumbService.getInstance(getProject());
        if (dumbService.isDumb()) return "";
        CaretModel caretModel = editor.getCaretModel();
        int caretCount = caretModel.getCaretCount();
        if (caretCount > 1) return "";
        Caret caret = caretModel.getCurrentCaret();
        PsiFile psiFile = PsiDocumentManager.getInstance(getProject()).getPsiFile(editor.getDocument());
        if (psiFile == null) return "";
        int offset = caret.getOffset();
        PsiElement element = psiFile.findElementAt(offset);
        if (!(element instanceof PsiJavaToken)) return "";
        PsiJavaToken javaToken = (PsiJavaToken) element;
        IElementType tokenType = javaToken.getTokenType();
        PsiElement parent = javaToken.getParent();
        if (!(parent instanceof PsiLiteralExpression)) return "";
        PsiLiteralExpression literalExpression = (PsiLiteralExpression) parent;
        TextRange range = literalExpression.getTextRange();
        if (!range.containsOffset(offset)) return "";
        if (tokenType == JavaTokenType.STRING_LITERAL) {
            String stringBeforeCaret = getStringLiteralContentBeforeCaret(offset, literalExpression, range);
            if (stringBeforeCaret == null) return "";
            StringPosition stringPosition = getStringPosition(stringBeforeCaret);
            StringPosition prefixLength = getPrefixLength(literalExpression);
            return prefixLength.plus(stringPosition).toString();
        } else if (tokenType == JavaTokenType.TEXT_BLOCK_LITERAL) {
            String stringBeforeCaret = getTextBlockContentBeforeCaret(caret, offset, literalExpression, range);
            if (stringBeforeCaret == null) return "";
            StringPosition stringPosition = getStringPosition(stringBeforeCaret);
            StringPosition prefixLength = getPrefixLength(literalExpression);
            return prefixLength.plus(stringPosition).toString();
        }
        return "";
    }

    @Nullable
    private String getTextBlockContentBeforeCaret(Caret caret, int offset, PsiLiteralExpression literalExpression, TextRange range) {
        TextRange literalContentRange = new TextRange(range.getStartOffset()+3, range.getEndOffset()-3);
        if (!literalContentRange.containsOffset(offset)) return null;
        LogicalPosition logicalPosition = caret.getLogicalPosition();
        int indent = PsiLiteralUtil.getTextBlockIndent(literalExpression);
        if (logicalPosition.column < indent) return null;
        int offsetInTextBlock = offset - range.getStartOffset();
        String fakeTail = "\\\n";
        String fakeIndent = StringUtils.repeat(" ", indent);
        String fakeText = literalExpression.getText().substring(0, offsetInTextBlock) + fakeTail + fakeIndent + "\"\"\"";
        FakePsiLiteralExpression fakeLiteralExpression = new FakePsiLiteralExpression(literalExpression, fakeText);
        String stringBeforeCaret = PsiLiteralUtil.getTextBlockText(fakeLiteralExpression);
        if (stringBeforeCaret == null) return "";
        int length = stringBeforeCaret.length();
        int fakeTailLength = fakeTail.length();
        if (length < fakeTailLength) return null;
        return stringBeforeCaret.substring(0, length - fakeTailLength);
    }

    @Nullable
    private String getStringLiteralContentBeforeCaret(int offset, PsiLiteralExpression literalExpression, TextRange range) {
        TextRange literalContentRange = new TextRange(range.getStartOffset()+1, range.getEndOffset()-1);
        if (!literalContentRange.containsOffset(offset)) return null;
        String literalContent = PsiLiteralUtil.getStringLiteralContent(literalExpression);
        if (null == literalContent) return null;
        int index = offset - literalContentRange.getStartOffset();
        //if (index < 0) return null;
        //if (index > literalContent.length()) return null;
        return literalContent.substring(0, index);
    }

    @NotNull private @NonNls StringPosition getStringPosition(String stringBeforeCaret) {
        StringBuilder outChars = new StringBuilder();
        parseStringCharacters(stringBeforeCaret, outChars, null, true, false);
        return new StringPosition(outChars.toString());
    }

    private StringPosition getPrefixLength(PsiLiteralExpression literalExpression) {
        StringPosition offset = StringPosition.ZERO;
        PsiElement literalExpressionParent = PsiUtil.skipParenthesizedExprUp(literalExpression.getParent());
        if (!(literalExpressionParent instanceof PsiPolyadicExpression)) {
            return offset;
        }
        PsiPolyadicExpression polyadicExpression = (PsiPolyadicExpression) literalExpressionParent;
        if (!JavaTokenType.PLUS.equals(polyadicExpression.getOperationTokenType())) {
            return offset;
        }
        PsiExpression[] operands = polyadicExpression.getOperands();
        for (PsiExpression operand : operands) {
            operand = PsiUtil.skipParenthesizedExprDown(operand);
            if (operand == literalExpression) {
                return offset;
            }
            String stringValue = valueToString(operand).orElse("");
            offset = offset.plus(stringValue);
        }
        return offset;
    }

    private void update(String text) {
        this.text = text;
        myStatusBar.updateWidget(ID());
    }

    @Override public @NotNull @NlsContexts.Label String getText() {
        return text;
    }

    @Override public float getAlignment() {
        return 0;
    }

    @Override public @Nullable @NlsContexts.Tooltip String getTooltipText() {
        return null;
    }

    @Override public @Nullable Consumer<MouseEvent> getClickConsumer() {
        return null;
    }

    @Override public void documentChanged(@NotNull DocumentEvent event) {
        EditorFactory.getInstance().editors(event.getDocument())
                .findFirst()
                .ifPresent(editor -> update(getText(editor)));
    }

}
