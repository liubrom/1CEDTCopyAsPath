package ru.cursor.edt.copypath.ui.internal.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import ru.cursor.edt.copypath.ui.internal.debug.CopyPathDebugLog;

public final class EditorTextSelectionReader
{
    private EditorTextSelectionReader()
    {
    }

    public static EditorTextSelectionInfo read(ITextEditor editor)
    {
        ITextSelection selection = getTextSelection(editor);
        if (selection == null)
        {
            return null;
        }
        IDocument document = getDocument(editor);
        if (document == null)
        {
            return null;
        }
        int startLineZeroBased = selection.getStartLine();
        int endLineZeroBased = selection.getEndLine();
        int startLine = startLineZeroBased + 1;
        int endLine = endLineZeroBased + 1;
        int selectedLineCount = endLineZeroBased - startLineZeroBased + 1;
        String fullLinesText = readFullLines(document, startLineZeroBased, endLineZeroBased);
        return new EditorTextSelectionInfo(startLine, endLine, selectedLineCount, fullLinesText);
    }

    private static String readFullLines(IDocument document, int startLineZeroBased, int endLineZeroBased)
    {
        StringBuilder builder = new StringBuilder();
        for (int line = startLineZeroBased; line <= endLineZeroBased; line++)
        {
            if (line > startLineZeroBased)
            {
                builder.append(System.lineSeparator());
            }
            String lineText = getLineText(document, line);
            if (lineText != null)
            {
                builder.append(lineText);
            }
        }
        return builder.toString();
    }

    private static ITextSelection getTextSelection(ITextEditor editor)
    {
        ISelectionProvider provider = editor.getSelectionProvider();
        if (provider == null)
        {
            return null;
        }
        ISelection selection = provider.getSelection();
        if (selection instanceof ITextSelection textSelection)
        {
            return textSelection;
        }
        return null;
    }

    private static IDocument getDocument(ITextEditor editor)
    {
        IDocumentProvider provider = editor.getDocumentProvider();
        if (provider == null)
        {
            return null;
        }
        return provider.getDocument(editor.getEditorInput());
    }

    private static String getLineText(IDocument document, int zeroBasedLine)
    {
        try
        {
            int offset = document.getLineOffset(zeroBasedLine);
            int length = document.getLineLength(zeroBasedLine);
            String line = document.get(offset, length);
            if (line.endsWith("\r\n")) //$NON-NLS-1$
            {
                return line.substring(0, line.length() - 2);
            }
            if (line.endsWith("\n") || line.endsWith("\r")) //$NON-NLS-1$ //$NON-NLS-2$
            {
                return line.substring(0, line.length() - 1);
            }
            return line;
        }
        catch (BadLocationException ex)
        {
            CopyPathDebugLog.log("editor-selection", "line read failed: " + ex.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
    }
}
