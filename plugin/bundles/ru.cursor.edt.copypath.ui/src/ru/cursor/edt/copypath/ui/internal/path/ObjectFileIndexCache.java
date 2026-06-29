package ru.cursor.edt.copypath.ui.internal.path;

import java.util.HashMap;
import java.util.Map;

/**
 * Кэш {@link ObjectFileIndex#scan(String)} на время построения одного подменю.
 */
public final class ObjectFileIndexCache
{
    private static final ThreadLocal<Map<String, ObjectFileIndex>> SESSION = new ThreadLocal<>();

    private ObjectFileIndexCache()
    {
    }

    public static void beginSession()
    {
        SESSION.set(new HashMap<>());
    }

    public static void endSession()
    {
        SESSION.remove();
    }

    public static ObjectFileIndex scan(String objectFolderPath)
    {
        Map<String, ObjectFileIndex> session = SESSION.get();
        if (session == null || objectFolderPath == null)
        {
            return ObjectFileIndex.scan(objectFolderPath);
        }
        return session.computeIfAbsent(objectFolderPath, ObjectFileIndex::scan);
    }
}
