package ru.cursor.edt.copypath.ui.internal.menu;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import ru.cursor.edt.copypath.ui.internal.debug.CopyPathDebugLog;
import ru.cursor.edt.copypath.ui.internal.path.CopyPathClipboard;
import ru.cursor.edt.copypath.ui.internal.path.MenuEntry;
import ru.cursor.edt.copypath.ui.internal.path.MetadataSelectionResolver;
import ru.cursor.edt.copypath.ui.internal.path.ObjectFileIndexCache;
import ru.cursor.edt.copypath.ui.internal.path.PathCatalogBuilder;

import java.util.List;

/**
 * Наполняет подменю «Копировать как путь» (родительский каскад создаёт Eclipse).
 */
public class CopyPathDynamicMenu extends ContributionItem
{
    private final PathCatalogBuilder catalogBuilder = new PathCatalogBuilder();

    public CopyPathDynamicMenu()
    {
        super();
    }

    public CopyPathDynamicMenu(String id)
    {
        super(id);
    }

    @Override
    public void fill(Menu menu, int index)
    {
        ObjectFileIndexCache.beginSession();
        try
        {
            List<MenuEntry> menuEntries = resolveMenuEntries();
            if (menuEntries.isEmpty())
            {
                CopyPathDebugLog.log("fill", "skip: empty entries, index=" + index); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            CopyPathDebugLog.log("fill", "entries=" + menuEntries.size() + ", index=" + index); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

            int insertIndex = index;
            MenuEntry.MenuGroup previousGroup = null;
            for (MenuEntry entry : menuEntries)
            {
                if (previousGroup != null && entry.getGroup() != previousGroup)
                {
                    if (insertIndex < 0)
                    {
                        new MenuItem(menu, SWT.SEPARATOR);
                    }
                    else
                    {
                        new MenuItem(menu, SWT.SEPARATOR, insertIndex++);
                    }
                }
                previousGroup = entry.getGroup();
                String clipboardText = prepareClipboardText(entry);
                if (clipboardText == null || clipboardText.isEmpty())
                {
                    continue;
                }
                MenuItem item = insertIndex < 0
                    ? new MenuItem(menu, SWT.PUSH)
                    : new MenuItem(menu, SWT.PUSH, insertIndex++);
                item.setText(entry.getLabel());
                item.addSelectionListener(new SelectionAdapter()
                {
                    @Override
                    public void widgetSelected(SelectionEvent event)
                    {
                        CopyPathClipboard.copyText(clipboardText);
                    }
                });
            }
        }
        catch (RuntimeException e)
        {
            CopyPathDebugLog.log("fill-error", e.getClass().getSimpleName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        finally
        {
            ObjectFileIndexCache.endSession();
        }
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    private List<MenuEntry> resolveMenuEntries()
    {
        MetadataSelectionResolver.ResolvedSelection selection = ContextMenuSelectionCache.takeSelection();
        if (selection == null)
        {
            CopyPathDebugLog.log("fill", "resolve start"); //$NON-NLS-1$ //$NON-NLS-2$
            selection = MetadataSelectionResolver.resolveForFill();
        }
        else
        {
            CopyPathDebugLog.log("fill", "source=cache, kind=" + selection.getKind()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        if (selection == null)
        {
            CopyPathDebugLog.log("fill", "selection=null"); //$NON-NLS-1$ //$NON-NLS-2$
            return List.of();
        }
        List<MenuEntry> entries = catalogBuilder.build(selection);
        CopyPathDebugLog.log("fill", "kind=" + selection.getKind() + ", entries=" + entries.size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return entries;
    }

    private String prepareClipboardText(MenuEntry entry)
    {
        List<String> paths = entry.getPaths();
        if (paths == null || paths.isEmpty())
        {
            return null;
        }
        if (paths.size() == 1)
        {
            return CopyPathClipboard.formatPath(paths.get(0));
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < paths.size(); i++)
        {
            if (i > 0)
            {
                builder.append(System.lineSeparator());
            }
            builder.append(CopyPathClipboard.formatPath(paths.get(i)));
        }
        return builder.toString();
    }
}
