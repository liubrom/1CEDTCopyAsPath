package ru.cursor.edt.copypath.ui.internal.path;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import org.eclipse.emf.ecore.EObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Индекс файлов объекта метаданных на диске. Меню строится из фактических путей,
 * а не только из EMF/API EDT.
 */
public final class ObjectFileIndex
{
    private final String objectFolder;
    private final List<String> allFiles;
    private final String metadataFile;
    private final Map<String, String> rootModules;
    private final Map<String, String> formFolders;
    private final Map<String, String> commandFolders;
    private final Map<String, String> templateFolders;

    private ObjectFileIndex(String objectFolder, List<String> allFiles, String metadataFile,
        Map<String, String> rootModules, Map<String, String> formFolders,
        Map<String, String> commandFolders, Map<String, String> templateFolders)
    {
        this.objectFolder = objectFolder;
        this.allFiles = allFiles;
        this.metadataFile = metadataFile;
        this.rootModules = rootModules;
        this.formFolders = formFolders;
        this.commandFolders = commandFolders;
        this.templateFolders = templateFolders;
    }

    public static ObjectFileIndex scan(String objectFolderPath)
    {
        if (objectFolderPath == null || objectFolderPath.isBlank())
        {
            return empty();
        }
        Path root = Path.of(objectFolderPath);
        if (!Files.isDirectory(root))
        {
            return empty();
        }
        List<String> allFiles = new ArrayList<>();
        collectFiles(root, allFiles);
        Collections.sort(allFiles);

        String metadataFile = null;
        Map<String, String> rootModules = new LinkedHashMap<>();
        for (String file : allFiles)
        {
            Path path = Path.of(file);
            if (!root.equals(path.getParent()))
            {
                continue;
            }
            String name = path.getFileName().toString();
            if (name.endsWith(".mdo")) //$NON-NLS-1$
            {
                metadataFile = file;
            }
            else if (name.endsWith(".bsl")) //$NON-NLS-1$
            {
                rootModules.put(name, file);
            }
        }

        return new ObjectFileIndex(
            objectFolderPath,
            allFiles,
            metadataFile,
            rootModules,
            collectSubFolders(root.resolve(MetadataCollectionDirs.FORMS)),
            collectSubFolders(root.resolve(MetadataCollectionDirs.COMMANDS)),
            collectSubFolders(root.resolve(MetadataCollectionDirs.TEMPLATES)));
    }

    public static ObjectFileIndex scanCached(String objectFolderPath)
    {
        return ObjectFileIndexCache.scan(objectFolderPath);
    }

    /**
     * Владелец подчинённого элемента (документ, отчёт и т.д.) — подъём по {@code eContainer()}.
     */
    public static EObject findOwnerMetadataObject(EObject eObject)
    {
        EObject owner = null;
        EObject current = eObject;
        while (current != null)
        {
            if (current instanceof MdObject mdObject && !(mdObject instanceof Configuration))
            {
                if (!MetadataObjectClassifier.isNonFileMetadataNode(current)
                    && !MetadataObjectClassifier.isAnySubElement(current))
                {
                    owner = current;
                }
            }
            current = current.eContainer();
        }
        return owner;
    }

    /**
     * Папка объекта на диске без тяжёлого {@link PathResolver} для форм/команд/макетов.
     */
    public static String locateOwnerObjectFolder(EObject owner)
    {
        if (owner == null)
        {
            return null;
        }
        String fromEmf = EmfStorageRelativePath.topObjectFolderAbsolute(owner);
        if (MetadataObjectFolderLocator.isObjectRootFolder(fromEmf))
        {
            return fromEmf;
        }
        String fromLocator = MetadataObjectFolderLocator.locateObjectFolder(owner);
        if (MetadataObjectFolderLocator.isObjectRootFolder(fromLocator))
        {
            return fromLocator;
        }
        return null;
    }

    public static ObjectFileIndex empty()
    {
        return new ObjectFileIndex(null, List.of(), null, Map.of(), Map.of(), Map.of(), Map.of());
    }

    public boolean isEmpty()
    {
        return objectFolder == null || allFiles.isEmpty();
    }

    public String getObjectFolder()
    {
        return objectFolder;
    }

    public List<String> getAllFiles()
    {
        return allFiles;
    }

    public String getMetadataFile()
    {
        return metadataFile;
    }

    public Map<String, String> getRootModules()
    {
        return rootModules;
    }

    public Map<String, String> getFormFolders()
    {
        return formFolders;
    }

    public Map<String, String> getCommandFolders()
    {
        return commandFolders;
    }

    public Map<String, String> getTemplateFolders()
    {
        return templateFolders;
    }

    public String findFormFolder(String formName)
    {
        if (formName == null)
        {
            return null;
        }
        String direct = formFolders.get(formName);
        if (direct != null)
        {
            return direct;
        }
        for (Map.Entry<String, String> entry : formFolders.entrySet())
        {
            if (formName.equalsIgnoreCase(entry.getKey()))
            {
                return entry.getValue();
            }
        }
        return null;
    }

    public String findCommandFolder(String commandName)
    {
        if (commandName == null)
        {
            return null;
        }
        String direct = commandFolders.get(commandName);
        if (direct != null)
        {
            return direct;
        }
        for (Map.Entry<String, String> entry : commandFolders.entrySet())
        {
            if (commandName.equalsIgnoreCase(entry.getKey()))
            {
                return entry.getValue();
            }
        }
        return null;
    }

    public String findTemplateFolder(String templateName)
    {
        if (templateName == null)
        {
            return null;
        }
        String direct = templateFolders.get(templateName);
        if (direct != null)
        {
            return direct;
        }
        for (Map.Entry<String, String> entry : templateFolders.entrySet())
        {
            if (templateName.equalsIgnoreCase(entry.getKey()))
            {
                return entry.getValue();
            }
        }
        return null;
    }

    public String findFormMetadataFile(String formFolder)
    {
        return findFileInFolder(formFolder, "Form.form"); //$NON-NLS-1$
    }

    public String findFormModuleFile(String formFolder)
    {
        return findFileInFolder(formFolder, "Module.bsl"); //$NON-NLS-1$
    }

    public String findCommandModuleFile(String commandFolder)
    {
        return findFileInFolder(commandFolder, "CommandModule.bsl"); //$NON-NLS-1$
    }

    public String findTemplateMetadataFile(String templateFolder)
    {
        if (templateFolder == null)
        {
            return null;
        }
        Path folder = Path.of(templateFolder);
        if (!Files.isDirectory(folder))
        {
            return null;
        }
        try (Stream<Path> stream = Files.list(folder))
        {
            return stream.filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(path -> Path.of(path).getFileName().toString().startsWith("Template.")) //$NON-NLS-1$
                .sorted()
                .findFirst()
                .orElse(null);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public static SelectionKind detectSubElementKind(EObject eObject)
    {
        if (MetadataObjectClassifier.isTopLevelMetadataObject(eObject))
        {
            return null;
        }
        EObject owner = findOwnerMetadataObject(eObject);
        String objectFolder = locateOwnerObjectFolder(owner);
        if (objectFolder == null)
        {
            return null;
        }
        String name = metadataName(eObject);
        if (name == null || name.isBlank())
        {
            return null;
        }
        if (findSubElementFolderPath(objectFolder, MetadataCollectionDirs.FORMS, name) != null)
        {
            return SelectionKind.FORM;
        }
        if (findSubElementFolderPath(objectFolder, MetadataCollectionDirs.COMMANDS, name) != null)
        {
            return SelectionKind.COMMAND;
        }
        if (findSubElementFolderPath(objectFolder, MetadataCollectionDirs.TEMPLATES, name) != null)
        {
            return SelectionKind.TEMPLATE;
        }
        return null;
    }

    public static String findSubElementFolderPath(String objectFolder, String subDirName, String elementName)
    {
        if (objectFolder == null || subDirName == null || elementName == null || elementName.isBlank())
        {
            return null;
        }
        Path direct = Path.of(objectFolder, subDirName, elementName);
        if (Files.isDirectory(direct))
        {
            return direct.toString();
        }
        Path subDir = Path.of(objectFolder, subDirName);
        if (!Files.isDirectory(subDir))
        {
            return null;
        }
        try (Stream<Path> stream = Files.list(subDir))
        {
            return stream.filter(Files::isDirectory)
                .filter(path -> elementName.equalsIgnoreCase(path.getFileName().toString()))
                .map(Path::toString)
                .findFirst()
                .orElse(null);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public static String metadataName(EObject eObject)
    {
        if (eObject instanceof MdObject mdObject)
        {
            return mdObject.getName();
        }
        return MetadataObjectFolderLocator.metadataObjectName(eObject);
    }

    public static String findFileInFolder(String folderPath, String fileName)
    {
        if (folderPath == null || fileName == null)
        {
            return null;
        }
        Path file = Path.of(folderPath, fileName);
        return Files.isRegularFile(file) ? file.toString() : null;
    }

    public static String findFileEndingWith(String folderPath, String suffix)
    {
        if (folderPath == null || suffix == null)
        {
            return null;
        }
        try (Stream<Path> stream = Files.list(Path.of(folderPath)))
        {
            return stream.filter(Files::isRegularFile)
                .map(Path::toString)
                .filter(path -> path.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT)))
                .sorted()
                .findFirst()
                .orElse(null);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static Map<String, String> collectSubFolders(Path collectionDir)
    {
        Map<String, String> result = new LinkedHashMap<>();
        if (!Files.isDirectory(collectionDir))
        {
            return result;
        }
        try (Stream<Path> stream = Files.list(collectionDir))
        {
            stream.filter(Files::isDirectory)
                .sorted((a, b) -> a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString()))
                .forEach(path -> result.put(path.getFileName().toString(), path.toString()));
        }
        catch (IOException e)
        {
            return result;
        }
        return result;
    }

    private static void collectFiles(Path root, List<String> files)
    {
        try (Stream<Path> stream = Files.walk(root))
        {
            stream.filter(Files::isRegularFile)
                .map(Path::toString)
                .forEach(files::add);
        }
        catch (IOException e)
        {
            // Пустой список.
        }
    }
}
