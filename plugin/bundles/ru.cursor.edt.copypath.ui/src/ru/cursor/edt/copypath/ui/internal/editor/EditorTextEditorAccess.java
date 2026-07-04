package ru.cursor.edt.copypath.ui.internal.editor;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

public final class EditorTextEditorAccess
{
    private EditorTextEditorAccess()
    {
    }

    public static ITextEditor asTextEditor(IWorkbenchPart part)
    {
        if (part == null)
        {
            return null;
        }
        ITextEditor adapted = part.getAdapter(ITextEditor.class);
        if (adapted != null)
        {
            return adapted;
        }
        if (part instanceof ITextEditor textEditor)
        {
            return textEditor;
        }
        if (part instanceof IEditorPart editorPart)
        {
            adapted = editorPart.getAdapter(ITextEditor.class);
            if (adapted != null)
            {
                return adapted;
            }
        }
        return null;
    }
}
