package ru.cursor.edt.copypath.ui.internal.editor;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.ITextEditor;

import ru.cursor.edt.copypath.ui.internal.debug.CopyPathDebugLog;
import ru.cursor.edt.copypath.ui.internal.path.CopyPathClipboard;

public class EditorCopyPathHandler extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        ITextEditor textEditor = EditorTextEditorAccess.asTextEditor(editor);
        if (textEditor == null)
        {
            CopyPathDebugLog.log("editor-copy", "skip: no ITextEditor adapter for " //$NON-NLS-1$ //$NON-NLS-2$
                + (editor == null ? "null" : editor.getClass().getName())); //$NON-NLS-1$
            return null;
        }
        String reference = EditorPathReferenceFormatter.format(textEditor);
        if (reference == null || reference.isEmpty())
        {
            CopyPathDebugLog.log("editor-copy", "skip: empty reference"); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        CopyPathDebugLog.log("editor-copy", reference); //$NON-NLS-1$
        CopyPathClipboard.copyText(reference);
        return null;
    }
}
