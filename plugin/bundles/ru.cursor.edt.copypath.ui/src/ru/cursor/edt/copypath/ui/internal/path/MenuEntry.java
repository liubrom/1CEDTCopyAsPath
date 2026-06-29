package ru.cursor.edt.copypath.ui.internal.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MenuEntry
{
    /** Группа для визуального разделителя в подменю. */
    public enum MenuGroup
    {
        /** Папка объекта и модули. */
        FOLDER_AND_MODULES,
        /** Файл метаданных. */
        METADATA,
        /** Формы. */
        FORMS,
        /** Команды. */
        COMMANDS,
        /** Макеты. */
        TEMPLATES,
        /** Все файлы объекта. */
        ALL_FILES
    }

    private final String label;
    private final List<String> paths;
    private final MenuGroup group;

    private MenuEntry(String label, List<String> paths, MenuGroup group)
    {
        this.label = label;
        this.paths = paths;
        this.group = group;
    }

    public static MenuEntry action(String label, String path)
    {
        return action(label, path, MenuGroup.FOLDER_AND_MODULES);
    }

    public static MenuEntry action(String label, String path, MenuGroup group)
    {
        List<String> paths = new ArrayList<>();
        paths.add(path);
        return new MenuEntry(label, Collections.unmodifiableList(paths), group);
    }

    public static MenuEntry action(String label, List<String> paths)
    {
        return action(label, paths, MenuGroup.ALL_FILES);
    }

    public static MenuEntry action(String label, List<String> paths, MenuGroup group)
    {
        return new MenuEntry(label, Collections.unmodifiableList(new ArrayList<>(paths)), group);
    }

    public String getLabel()
    {
        return label;
    }

    public List<String> getPaths()
    {
        return paths;
    }

    public MenuGroup getGroup()
    {
        return group;
    }
}
