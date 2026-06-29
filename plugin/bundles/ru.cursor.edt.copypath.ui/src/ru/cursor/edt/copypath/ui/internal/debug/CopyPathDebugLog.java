package ru.cursor.edt.copypath.ui.internal.debug;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Диагностический лог плагина (выключен по умолчанию).
 * Включение: файл {@code %USERPROFILE%\\copy-as-path-debug.on}
 * Лог: {@code %USERPROFILE%\\copy-as-path-debug.log}
 */
public final class CopyPathDebugLog
{
    private static final Path ENABLE_MARKER = Path.of(System.getProperty("user.home"), "copy-as-path-debug.on"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final Path HOME_LOG = Path.of(System.getProperty("user.home"), "copy-as-path-debug.log"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final Path TEMP_LOG = Path.of(System.getProperty("java.io.tmpdir"), "copy-as-path-debug.log"); //$NON-NLS-1$ //$NON-NLS-2$
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"); //$NON-NLS-1$

    private CopyPathDebugLog()
    {
    }

    public static boolean isEnabled()
    {
        return Files.isRegularFile(ENABLE_MARKER);
    }

    public static Path homeLogFile()
    {
        return HOME_LOG;
    }

    public static void log(String phase, String message)
    {
        if (!isEnabled())
        {
            return;
        }
        String line = TIME.format(LocalDateTime.now()) + " [" + phase + "] " + message + System.lineSeparator(); //$NON-NLS-1$ //$NON-NLS-2$
        writeLine(HOME_LOG, line);
        if (!HOME_LOG.equals(TEMP_LOG))
        {
            writeLine(TEMP_LOG, line);
        }
    }

    private static void writeLine(Path file, String line)
    {
        try
        {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
        catch (IOException e)
        {
            // Диагностика не должна ломать UI.
        }
    }

    public static String describe(Object value)
    {
        if (value == null)
        {
            return "null"; //$NON-NLS-1$
        }
        String type = value.getClass().getName();
        if (value instanceof org.eclipse.jface.viewers.IStructuredSelection selection)
        {
            return type + "{size=" + selection.size() //$NON-NLS-1$
                + ", first=" + describeElement(selection.getFirstElement()) + "}"; //$NON-NLS-1$ //$NON-NLS-2$
        }
        return describeElement(value);
    }

    private static String describeElement(Object element)
    {
        if (element == null)
        {
            return "null"; //$NON-NLS-1$
        }
        StringBuilder builder = new StringBuilder(element.getClass().getName());
        if (element instanceof org.eclipse.emf.ecore.EObject eObject)
        {
            builder.append('{').append(eObject.eClass().getName());
            if (eObject instanceof com._1c.g5.v8.dt.metadata.mdclass.MdObject mdObject)
            {
                builder.append(", name=").append(mdObject.getName()); //$NON-NLS-1$
            }
            org.eclipse.emf.ecore.EStructuralFeature feature = eObject.eContainingFeature();
            if (feature != null)
            {
                builder.append(", feature=").append(feature.getName()); //$NON-NLS-1$
            }
            builder.append('}');
        }
        return builder.toString();
    }
}
