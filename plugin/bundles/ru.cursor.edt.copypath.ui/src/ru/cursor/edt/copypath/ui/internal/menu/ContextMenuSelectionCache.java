package ru.cursor.edt.copypath.ui.internal.menu;

import ru.cursor.edt.copypath.ui.internal.path.MetadataSelectionResolver;

/**
 * Кэш выделения между {@code PropertyTester} и {@link CopyPathDynamicMenu#fill}.
 */
public final class ContextMenuSelectionCache
{
    private static final ThreadLocal<MetadataSelectionResolver.ResolvedSelection> CACHED = new ThreadLocal<>();

    private ContextMenuSelectionCache()
    {
    }

    public static void rememberSelection(MetadataSelectionResolver.ResolvedSelection selection)
    {
        if (selection != null)
        {
            CACHED.set(selection);
        }
    }

    public static MetadataSelectionResolver.ResolvedSelection takeSelection()
    {
        MetadataSelectionResolver.ResolvedSelection selection = CACHED.get();
        CACHED.remove();
        return selection;
    }

    public static void clear()
    {
        CACHED.remove();
    }
}
