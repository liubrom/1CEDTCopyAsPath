package ru.cursor.edt.copypath.ui.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS
{
    private static final String BUNDLE_NAME = "ru.cursor.edt.copypath.ui.internal.messages"; //$NON-NLS-1$

    public static String Menu_copyAsPath;
    public static String Entry_objectFolder;
    public static String Entry_managerModule;
    public static String Entry_objectModule;
    public static String Entry_recordSetModule;
    public static String Entry_commonModule;
    public static String Entry_formModule;
    public static String Entry_commandModule;
    public static String Entry_module;
    public static String Entry_managedApplicationModule;
    public static String Entry_sessionModule;
    public static String Entry_externalConnectionModule;
    public static String Entry_ordinaryApplicationModule;
    public static String Entry_metadataFile;
    public static String Entry_formFolder;
    public static String Entry_commandFolder;
    public static String Entry_templateFolder;
    public static String Entry_form;
    public static String Entry_command;
    public static String Entry_template;
    public static String Entry_allObjectFiles;
    public static String Editor_lineSingle;
    public static String Editor_linesRange;

    static
    {
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }

    public static String metadataFile(String extension)
    {
        return NLS.bind(Entry_metadataFile, extension);
    }

    public static String form(String name)
    {
        return NLS.bind(Entry_form, name);
    }

    public static String command(String name)
    {
        return NLS.bind(Entry_command, name);
    }

    public static String template(String name)
    {
        return NLS.bind(Entry_template, name);
    }

    public static String editorLineSingle(int line)
    {
        return NLS.bind(Editor_lineSingle, line);
    }

    public static String editorLinesRange(int startLine, int endLine)
    {
        return NLS.bind(Editor_linesRange, startLine, endLine);
    }
}
