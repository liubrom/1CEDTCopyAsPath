package ru.cursor.edt.copypath.ui.internal.path;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import java.util.List;
import java.util.stream.Collectors;

public final class CopyPathClipboard
{
    private CopyPathClipboard()
    {
    }

    public static void copy(List<String> absolutePaths)
    {
        if (absolutePaths == null || absolutePaths.isEmpty())
        {
            return;
        }
        String text = absolutePaths.stream()
            .map(CopyPathClipboard::formatPath)
            .collect(Collectors.joining(System.lineSeparator()));
        copyText(text);
    }

    public static void copyText(String text)
    {
        if (text == null || text.isEmpty())
        {
            return;
        }
        Display display = Display.getCurrent();
        if (display == null)
        {
            display = Display.getDefault();
        }
        Clipboard clipboard = new Clipboard(display);
        try
        {
            clipboard.setContents(new Object[] {text}, new Transfer[] {TextTransfer.getInstance()});
        }
        finally
        {
            clipboard.dispose();
        }
    }

    public static String formatPath(String absolutePath)
    {
        return '"' + absolutePath + '"';
    }
}
