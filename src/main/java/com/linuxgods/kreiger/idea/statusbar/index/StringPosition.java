package com.linuxgods.kreiger.idea.statusbar.index;

import com.intellij.openapi.editor.LogicalPosition;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NonNls;

import java.util.Objects;

class StringPosition extends LogicalPosition {
    private final int offset;

    final static StringPosition ZERO = new StringPosition(0, 0, 0);

    private StringPosition(int line, int column, int offset) throws IllegalArgumentException {
        super(line, column);
        this.offset = offset;
    }

    public StringPosition(String string) throws IllegalArgumentException {
        this(StringUtils.countMatches(string, "\n"), string.length() - StringUtils.lastIndexOf(string, '\n') - 1, string.length());
    }

    public StringPosition plus(StringPosition other) {
        return new StringPosition(line + other.line, other.line == 0 ? column + other.column : other.column, offset + other.offset);
    }

    public StringPosition plus(String string) {
        if (string.isEmpty()) return this;
        return plus(new StringPosition(string));
    }

    @Override public @NonNls String toString() {
        //if (line == 0) return "String index " + offset;
        return "String " + (line + 1) + ":" + (column + 1) + " (index " + offset + ")";
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StringPosition that = (StringPosition) o;
        return offset == that.offset;
    }

    @Override public int hashCode() {
        return Objects.hash(super.hashCode(), offset);
    }
}
