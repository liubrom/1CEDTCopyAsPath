package ru.cursor.edt.copypath.ui.internal.editor;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import ru.cursor.edt.copypath.ui.internal.path.CopyPathClipboard;

public final class EditorFilePathResolver
{
    private EditorFilePathResolver()
    {
    }

    public static String resolveQuotedPath(ITextEditor editor)
    {
        if (editor == null)
        {
            return null;
        }
        IEditorInput input = editor.getEditorInput();
        if (input == null)
        {
            return null;
        }
        IFile file = resolveFile(input);
        if (file == null || !file.exists())
        {
            return null;
        }
        var location = file.getLocation();
        if (location == null)
        {
            return null;
        }
        return CopyPathClipboard.formatPath(location.toOSString());
    }

    private static IFile resolveFile(IEditorInput input)
    {
        if (input instanceof IFileEditorInput fileInput)
        {
            return fileInput.getFile();
        }
        return input.getAdapter(IFile.class);
    }
}
