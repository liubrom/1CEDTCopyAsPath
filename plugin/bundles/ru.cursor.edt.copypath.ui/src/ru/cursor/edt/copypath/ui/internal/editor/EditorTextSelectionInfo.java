package ru.cursor.edt.copypath.ui.internal.editor;

public final class EditorTextSelectionInfo
{
    private final int startLine;
    private final int endLine;
    private final int selectedLineCount;
    private final String fullLinesText;

    public EditorTextSelectionInfo(int startLine, int endLine, int selectedLineCount, String fullLinesText)
    {
        this.startLine = startLine;
        this.endLine = endLine;
        this.selectedLineCount = selectedLineCount;
        this.fullLinesText = fullLinesText;
    }

    public int getStartLine()
    {
        return startLine;
    }

    public int getEndLine()
    {
        return endLine;
    }

    public int getSelectedLineCount()
    {
        return selectedLineCount;
    }

    public String getFullLinesText()
    {
        return fullLinesText;
    }
}
