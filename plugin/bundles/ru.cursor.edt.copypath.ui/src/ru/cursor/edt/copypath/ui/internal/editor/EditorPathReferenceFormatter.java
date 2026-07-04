package ru.cursor.edt.copypath.ui.internal.editor;

import org.eclipse.ui.texteditor.ITextEditor;

import ru.cursor.edt.copypath.ui.internal.Messages;

public final class EditorPathReferenceFormatter
{
    private static final int MAX_CODE_LINES = 10;

    private EditorPathReferenceFormatter()
    {
    }

    public static String format(ITextEditor editor)
    {
        String quotedPath = EditorFilePathResolver.resolveQuotedPath(editor);
        if (quotedPath == null)
        {
            return null;
        }
        EditorTextSelectionInfo selection = EditorTextSelectionReader.read(editor);
        if (selection == null)
        {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append(quotedPath);
        result.append(", "); //$NON-NLS-1$
        result.append(formatLinePart(selection));
        String codePart = resolveCodePart(selection);
        if (codePart != null && !codePart.isEmpty())
        {
            if (selection.getSelectedLineCount() > 1)
            {
                result.append(',').append(System.lineSeparator());
                result.append(codePart);
            }
            else
            {
                result.append(", "); //$NON-NLS-1$
                result.append(codePart);
            }
        }
        return result.toString();
    }

    private static String formatLinePart(EditorTextSelectionInfo selection)
    {
        int startLine = selection.getStartLine();
        int endLine = selection.getEndLine();
        if (startLine == endLine)
        {
            return Messages.editorLineSingle(startLine);
        }
        return Messages.editorLinesRange(startLine, endLine);
    }

    private static String resolveCodePart(EditorTextSelectionInfo selection)
    {
        if (selection.getSelectedLineCount() > MAX_CODE_LINES)
        {
            return null;
        }
        String fullLinesText = selection.getFullLinesText();
        if (fullLinesText == null || fullLinesText.isEmpty())
        {
            return null;
        }
        return fullLinesText;
    }
}
