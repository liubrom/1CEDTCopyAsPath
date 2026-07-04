package ru.cursor.edt.copypath.ui.internal.editor;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.texteditor.ITextEditor;

public class EditorCopyPathSupportedTester extends PropertyTester
{
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
    {
        if (!"editorSupported".equals(property) || !(receiver instanceof ITextEditor textEditor)) //$NON-NLS-1$
        {
            return false;
        }
        return EditorFilePathResolver.resolveQuotedPath(textEditor) != null;
    }
}
