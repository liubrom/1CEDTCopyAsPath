package ru.cursor.edt.copypath.ui.internal.path;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import org.eclipse.emf.ecore.EObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Поиск папок объекта метаданных по путям в файловой системе.
 */
public final class MetadataObjectFolderLocator
{
    private static final String SRC = "src"; //$NON-NLS-1$
    private static final String FORMS_DIR = "Forms"; //$NON-NLS-1$
    private static final String COMMANDS_DIR = "Commands"; //$NON-NLS-1$
    private static final String TEMPLATES_DIR = "Templates"; //$NON-NLS-1$

    private MetadataObjectFolderLocator()
    {
    }

    public static String locateObjectFolder(EObject eObject)
    {
        if (eObject == null)
        {
            return null;
        }
        EObject current = eObject;
        while (current != null)
        {
            String folder = locateObjectFolderOnNode(current);
            if (folder != null)
            {
                return folder;
            }
            current = current.eContainer();
        }
        return null;
    }

    private static String locateObjectFolderOnNode(EObject eObject)
    {
        if (eObject == null)
        {
            return null;
        }
        String fromApi = PathResolver.objectFolderPath(eObject);
        if (isObjectRootFolder(fromApi))
        {
            return fromApi;
        }
        String fromProjectRelative = PathResolver.objectFolderFromStorage(eObject);
        if (isObjectRootFolder(fromProjectRelative))
        {
            return fromProjectRelative;
        }
        String fromStorage = objectRootFromAbsolutePath(PathResolver.absolutePath(eObject));
        if (isObjectRootFolder(fromStorage))
        {
            return fromStorage;
        }
        return null;
    }

    private static String locateFormFolderOnNode(EObject eObject)
    {
        String fromPath = subElementFolderFromAbsolutePath(PathResolver.absolutePath(eObject), FORMS_DIR);
        if (fromPath != null)
        {
            return fromPath;
        }
        fromPath = subElementFolderFromAbsolutePath(PathResolver.formMetadataPath(eObject), FORMS_DIR);
        if (fromPath != null)
        {
            return fromPath;
        }
        fromPath = subElementFolderFromAbsolutePath(PathResolver.formModulePath(eObject), FORMS_DIR);
        if (fromPath != null)
        {
            return fromPath;
        }
        String objectFolder = locateObjectFolderOnNode(eObject);
        String name = metadataObjectName(eObject);
        if (objectFolder != null && name != null)
        {
            Path candidate = Path.of(objectFolder, FORMS_DIR, name);
            if (Files.isDirectory(candidate))
            {
                return candidate.toString();
            }
        }
        String relative = PathResolver.relativeStoragePath(eObject);
        String formName = subElementNameFromRelativePath(relative, FORMS_DIR);
        if (objectFolder != null && formName != null)
        {
            Path candidate = Path.of(objectFolder, FORMS_DIR, formName);
            if (Files.isDirectory(candidate))
            {
                return candidate.toString();
            }
        }
        return null;
    }

    public static String locateFormFolder(EObject eObject)
    {
        EObject current = eObject;
        while (current != null)
        {
            String folder = locateFormFolderOnNode(current);
            if (folder != null)
            {
                return folder;
            }
            current = current.eContainer();
        }
        return null;
    }

    public static String locateCommandFolder(EObject eObject)
    {
        EObject current = eObject;
        while (current != null)
        {
            String folder = locateCommandFolderOnNode(current);
            if (folder != null)
            {
                return folder;
            }
            current = current.eContainer();
        }
        return null;
    }

    private static String locateCommandFolderOnNode(EObject eObject)
    {
        String fromPath = subElementFolderFromAbsolutePath(PathResolver.absolutePath(eObject), COMMANDS_DIR);
        if (fromPath != null)
        {
            return fromPath;
        }
        String objectFolder = locateObjectFolderOnNode(eObject);
        String name = metadataObjectName(eObject);
        if (objectFolder != null && name != null)
        {
            Path candidate = Path.of(objectFolder, COMMANDS_DIR, name);
            if (Files.isDirectory(candidate))
            {
                return candidate.toString();
            }
        }
        String relative = PathResolver.relativeStoragePath(eObject);
        String commandName = subElementNameFromRelativePath(relative, COMMANDS_DIR);
        if (objectFolder != null && commandName != null)
        {
            Path candidate = Path.of(objectFolder, COMMANDS_DIR, commandName);
            if (Files.isDirectory(candidate))
            {
                return candidate.toString();
            }
        }
        return null;
    }

    public static String locateTemplateFolder(EObject eObject)
    {
        EObject current = eObject;
        while (current != null)
        {
            String folder = locateTemplateFolderOnNode(current);
            if (folder != null)
            {
                return folder;
            }
            current = current.eContainer();
        }
        return null;
    }

    private static String locateTemplateFolderOnNode(EObject eObject)
    {
        String fromPath = subElementFolderFromAbsolutePath(PathResolver.absolutePath(eObject), TEMPLATES_DIR);
        if (fromPath != null)
        {
            return fromPath;
        }
        String objectFolder = locateObjectFolderOnNode(eObject);
        String name = metadataObjectName(eObject);
        if (objectFolder != null && name != null)
        {
            Path candidate = Path.of(objectFolder, TEMPLATES_DIR, name);
            if (Files.isDirectory(candidate))
            {
                return candidate.toString();
            }
        }
        String relative = PathResolver.relativeStoragePath(eObject);
        String templateName = subElementNameFromRelativePath(relative, TEMPLATES_DIR);
        if (objectFolder != null && templateName != null)
        {
            Path candidate = Path.of(objectFolder, TEMPLATES_DIR, templateName);
            if (Files.isDirectory(candidate))
            {
                return candidate.toString();
            }
        }
        return null;
    }

    public static boolean isObjectRootFolder(String folderPath)
    {
        if (folderPath == null || folderPath.isBlank())
        {
            return false;
        }
        Path folder = Path.of(folderPath);
        if (!Files.isDirectory(folder))
        {
            return false;
        }
        try (var stream = Files.list(folder))
        {
            return stream.anyMatch(path -> Files.isRegularFile(path)
                && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mdo")); //$NON-NLS-1$
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static String metadataObjectName(EObject eObject)
    {
        if (eObject == null)
        {
            return null;
        }
        if (eObject instanceof MdObject mdObject)
        {
            return mdObject.getName();
        }
        return PathResolver.metadataObjectName(eObject);
    }

    private static String objectRootFromAbsolutePath(String absolutePath)
    {
        if (absolutePath == null)
        {
            return null;
        }
        String normalized = absolutePath.replace('\\', '/');
        int srcIndex = indexOfSrcSegment(normalized);
        if (srcIndex < 0)
        {
            return null;
        }
        String afterSrc = normalized.substring(srcIndex + "/src/".length()); //$NON-NLS-1$
        String[] parts = afterSrc.split("/"); //$NON-NLS-1$
        if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty())
        {
            return null;
        }
        String prefix = normalized.substring(0, srcIndex + "/src/".length()); //$NON-NLS-1$
        return Path.of(prefix, parts[0], parts[1]).toString();
    }

    private static String subElementFolderFromAbsolutePath(String absolutePath, String subDir)
    {
        if (absolutePath == null)
        {
            return null;
        }
        String normalized = absolutePath.replace('\\', '/');
        String marker = "/" + subDir + "/"; //$NON-NLS-1$ //$NON-NLS-2$
        int index = normalized.indexOf(marker);
        if (index < 0)
        {
            return null;
        }
        String tail = normalized.substring(index + marker.length());
        int slash = tail.indexOf('/');
        String elementName = slash >= 0 ? tail.substring(0, slash) : tail;
        if (elementName.isEmpty())
        {
            return null;
        }
        return Path.of(normalized.substring(0, index + marker.length() + elementName.length())).toString();
    }

    private static String subElementNameFromRelativePath(String relativePath, String subDir)
    {
        if (relativePath == null)
        {
            return null;
        }
        String[] parts = relativePath.split("/"); //$NON-NLS-1$
        for (int i = 0; i < parts.length - 1; i++)
        {
            if (subDir.equals(parts[i]))
            {
                return parts[i + 1];
            }
        }
        return null;
    }

    private static int indexOfSrcSegment(String normalizedPath)
    {
        return normalizedPath.indexOf("/src/"); //$NON-NLS-1$
    }
}
