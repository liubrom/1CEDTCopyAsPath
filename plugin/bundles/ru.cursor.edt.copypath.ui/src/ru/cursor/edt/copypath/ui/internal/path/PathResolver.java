package ru.cursor.edt.copypath.ui.internal.path;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupport;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.metadata.mdclass.AbstractForm;
import com._1c.g5.v8.dt.metadata.mdclass.BasicCommand;
import com._1c.g5.v8.dt.metadata.mdclass.BasicForm;
import com._1c.g5.v8.dt.metadata.mdclass.BasicTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import ru.cursor.edt.copypath.ui.internal.Activator;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PathResolver
{
    private static final String FORMS_DIR = "Forms"; //$NON-NLS-1$
    private static final String COMMANDS_DIR = "Commands"; //$NON-NLS-1$
    private static final String TEMPLATES_DIR = "Templates"; //$NON-NLS-1$
    private static final String BSL_EXT = ".bsl"; //$NON-NLS-1$
    private static final String FORM_FILE = "Form.form"; //$NON-NLS-1$
    private static final String COMMAND_MODULE_FILE = "CommandModule.bsl"; //$NON-NLS-1$
    private static final String FORM_MODULE_FILE = "Module.bsl"; //$NON-NLS-1$

    private PathResolver()
    {
    }

    public static String absolutePath(EObject context, EReference feature)
    {
        IProjectFileSystemSupport support = fileSystemSupport(context);
        if (support == null)
        {
            return null;
        }
        IPath path = support.getPath(context, feature);
        String resolved = resolveResourcePath(context, path);
        if (resolved != null && !isOwnerMetadataPath(context, resolved))
        {
            return resolved;
        }
        return fileToAbsolutePath(support.getFile(context, feature));
    }

    public static String absolutePath(EObject eObject)
    {
        if (eObject instanceof Module module)
        {
            return moduleAbsolutePath(module);
        }
        IProjectFileSystemSupport support = fileSystemSupport(eObject);
        if (support != null)
        {
            IPath path = support.getPath(eObject);
            String resolved = resolveResourcePath(eObject, path);
            if (resolved != null && !isOwnerMetadataPath(eObject, resolved))
            {
                return resolved;
            }
        }
        String platform = fileToAbsolutePath(platformFile(eObject));
        if (platform != null && !isOwnerMetadataPath(eObject, platform))
        {
            return platform;
        }
        return filePathFromStorageUri(eObject);
    }

    public static String moduleAbsolutePath(Module module)
    {
        String result = null;
        IProjectFileSystemSupport support = fileSystemSupport(module);
        if (support != null)
        {
            IPath path = support.getPath(module);
            result = resolveResourcePath(module, path);
            if (!isBslFile(result))
            {
                result = fileToAbsolutePath(support.getFile(module));
            }
        }
        if (!isBslFile(result))
        {
            result = fileToAbsolutePath(platformFile(module));
        }
        if (!isBslFile(result))
        {
            result = filePathFromStorageUri(module);
        }
        if (!isBslFile(result))
        {
            result = inferredModulePath(module);
        }
        return isBslFile(result) ? result : null;
    }

    public static String formFolderPath(EObject formObject)
    {
        String result = folderPath(formObject);
        if (isSubElementFolder(result, FORMS_DIR))
        {
            return result;
        }
        result = subElementFolder(formObject, FORMS_DIR);
        if (result != null)
        {
            return result;
        }
        if (formObject instanceof BasicForm basicForm)
        {
            AbstractForm abstractForm = basicForm.getForm();
            if (abstractForm != null)
            {
                result = folderPath(abstractForm);
                if (isSubElementFolder(result, FORMS_DIR))
                {
                    return result;
                }
            }
        }
        return null;
    }

    public static String formMetadataPath(EObject formObject)
    {
        if (formObject instanceof BasicForm basicForm)
        {
            AbstractForm abstractForm = basicForm.getForm();
            if (abstractForm != null)
            {
                String path = absolutePath(abstractForm, MdClassPackage.Literals.ABSTRACT_FORM__MD_FORM);
                if (isFormMetadataPath(path))
                {
                    return path;
                }
                path = absolutePath(basicForm, MdClassPackage.Literals.BASIC_FORM__FORM);
                if (isFormMetadataPath(path))
                {
                    return path;
                }
            }
        }
        if (formObject instanceof AbstractForm abstractForm)
        {
            String path = absolutePath(abstractForm, MdClassPackage.Literals.ABSTRACT_FORM__MD_FORM);
            if (isFormMetadataPath(path))
            {
                return path;
            }
        }
        String folder = formFolderPath(formObject);
        String found = findFileInFolder(folder, FORM_FILE);
        if (found != null)
        {
            return found;
        }
        return findFileEndingWith(folder, ".form"); //$NON-NLS-1$
    }

    public static String formModulePath(EObject formObject)
    {
        Module module = null;
        if (formObject instanceof AbstractForm abstractForm)
        {
            module = abstractForm.getModule();
        }
        else if (formObject instanceof BasicForm basicForm && basicForm.getForm() != null)
        {
            module = basicForm.getForm().getModule();
        }
        if (module != null)
        {
            String path = moduleAbsolutePath(module);
            if (path != null)
            {
                return path;
            }
        }
        if (formObject instanceof AbstractForm abstractForm)
        {
            String path = absolutePath(abstractForm, MdClassPackage.Literals.ABSTRACT_FORM__MODULE);
            if (isBslFile(path))
            {
                return path;
            }
        }
        return findFileInFolder(formFolderPath(formObject), FORM_MODULE_FILE);
    }

    public static String commandFolderPath(BasicCommand command)
    {
        String result = folderPath(command);
        if (isSubElementFolder(result, COMMANDS_DIR))
        {
            return result;
        }
        return subElementFolder(command, COMMANDS_DIR);
    }

    public static String commandModulePath(BasicCommand command)
    {
        Module module = command.getCommandModule();
        if (module != null)
        {
            String path = moduleAbsolutePath(module);
            if (path != null)
            {
                return path;
            }
        }
        String path = absolutePath(command, MdClassPackage.Literals.BASIC_COMMAND__COMMAND_MODULE);
        if (isBslFile(path))
        {
            return path;
        }
        return findFileInFolder(commandFolderPath(command), COMMAND_MODULE_FILE);
    }

    public static String templateFolderPath(BasicTemplate template)
    {
        String result = folderPath(template);
        if (isSubElementFolder(result, TEMPLATES_DIR))
        {
            return result;
        }
        return subElementFolder(template, TEMPLATES_DIR);
    }

    public static String templateMetadataPath(BasicTemplate template)
    {
        String path = absolutePath(template, MdClassPackage.Literals.BASIC_TEMPLATE__TEMPLATE);
        if (path != null && !isOwnerMetadataPath(template, path))
        {
            return path;
        }
        if (template.getTemplate() != null)
        {
            path = absolutePath(template.getTemplate());
            if (path != null && !isOwnerMetadataPath(template, path))
            {
                return path;
            }
        }
        String folder = templateFolderPath(template);
        return findFileStartingWith(folder, "Template."); //$NON-NLS-1$
    }

    public static boolean isFormStorageContext(EObject eObject)
    {
        return containsStorageSegment(eObject, FORMS_DIR);
    }

    public static boolean isCommandStorageContext(EObject eObject)
    {
        return containsStorageSegment(eObject, COMMANDS_DIR);
    }

    public static boolean isTemplateStorageContext(EObject eObject)
    {
        return containsStorageSegment(eObject, TEMPLATES_DIR);
    }

    public static String folderPath(EObject eObject)
    {
        IProjectFileSystemSupport support = fileSystemSupport(eObject);
        if (support != null)
        {
            IPath path = support.getPath(eObject);
            String folder = resolveFolderPath(eObject, path);
            if (folder != null)
            {
                return folder;
            }
        }
        String filePath = absolutePath(eObject);
        if (filePath == null)
        {
            return null;
        }
        java.nio.file.Path path = java.nio.file.Path.of(filePath);
        if (Files.isDirectory(path))
        {
            return filePath;
        }
        java.nio.file.Path parent = path.getParent();
        return parent != null ? parent.toString() : null;
    }

    public static String objectFolderFromStorage(EObject eObject)
    {
        String rel = relativeStoragePath(eObject);
        if (rel == null)
        {
            return null;
        }
        List<String> segments = splitSegments(rel);
        if (segments.size() < 3 || !"src".equals(segments.get(0))) //$NON-NLS-1$
        {
            return null;
        }
        for (int i = 2; i < segments.size(); i++)
        {
            String segment = segments.get(i);
            if (FORMS_DIR.equals(segment) || COMMANDS_DIR.equals(segment) || TEMPLATES_DIR.equals(segment))
            {
                return resolveProjectPath(eObject, joinSegments(segments, 0, i));
            }
        }
        return resolveProjectPath(eObject, joinSegments(segments, 0, 3));
    }

    public static String resolveProjectResourcePath(EObject eObject, IPath path)
    {
        if (path == null)
        {
            return null;
        }
        String folder = resolveFolderPath(eObject, path);
        if (folder != null)
        {
            return folder;
        }
        return resolveResourcePath(eObject, path);
    }

    public static String metadataExtension(EObject eObject)
    {
        String path = absolutePath(eObject);
        if (path == null)
        {
            return null;
        }
        String fileName = java.nio.file.Path.of(path).getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0)
        {
            return "";
        }
        return fileName.substring(dot + 1);
    }

    public static List<String> allFilesUnderObject(EObject metadataRoot)
    {
        String objectFolder = objectFolderPath(metadataRoot);
        if (objectFolder == null)
        {
            return Collections.emptyList();
        }
        java.nio.file.Path root = java.nio.file.Path.of(objectFolder);
        if (!Files.isDirectory(root))
        {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        try
        {
            Files.walkFileTree(root, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(java.nio.file.Path file, BasicFileAttributes attrs)
                {
                    result.add(file.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (IOException e)
        {
            return Collections.emptyList();
        }
        Collections.sort(result);
        return result;
    }

    public static String objectFolderPath(EObject metadataRoot)
    {
        String metadataFile = absolutePath(metadataRoot);
        if (metadataFile == null)
        {
            return folderPath(metadataRoot);
        }
        java.nio.file.Path parent = java.nio.file.Path.of(metadataFile).getParent();
        return parent != null ? parent.toString() : null;
    }

    public static String moduleLabelFromPath(String path)
    {
        if (path == null)
        {
            return null;
        }
        String baseName = java.nio.file.Path.of(path).getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        String stem = dot > 0 ? baseName.substring(0, dot) : baseName;
        return switch (stem)
        {
            case "ObjectModule" -> "object"; //$NON-NLS-1$
            case "ManagerModule" -> "manager"; //$NON-NLS-1$
            case "RecordSetModule" -> "recordSet"; //$NON-NLS-1$
            case "CommandModule" -> "command"; //$NON-NLS-1$
            case "Module" -> "form"; //$NON-NLS-1$
            default -> stem.endsWith("Module") ? stem : null; //$NON-NLS-1$
        };
    }

    private static String inferredModulePath(Module module)
    {
        EObject container = module.eContainer();
        if (container instanceof BasicCommand command)
        {
            return findFileInFolder(commandFolderPath(command), COMMAND_MODULE_FILE);
        }
        if (container instanceof AbstractForm abstractForm)
        {
            return findFileInFolder(formFolderPath(abstractForm), FORM_MODULE_FILE);
        }
        return null;
    }

    private static String subElementFolder(EObject child, String subDir)
    {
        String rel = relativeStoragePath(child);
        if (rel != null)
        {
            String projectRelative = folderRelativeFromStoragePath(rel, subDir);
            if (projectRelative != null)
            {
                String resolved = resolveProjectPath(child, projectRelative);
                if (resolved != null)
                {
                    return resolved;
                }
            }
        }
        EObject owner = findOwningMetadataObject(child);
        if (owner == null)
        {
            return null;
        }
        String ownerFolder = objectFolderPath(owner);
        String name = metadataObjectName(child);
        if (ownerFolder == null || name == null)
        {
            return null;
        }
        java.nio.file.Path candidate = java.nio.file.Path.of(ownerFolder, subDir, name);
        return Files.isDirectory(candidate) ? candidate.toString() : null;
    }

    private static EObject findOwningMetadataObject(EObject child)
    {
        EObject current = child.eContainer();
        while (current != null)
        {
            if (MetadataObjectClassifier.isTopLevelMetadataObject(current))
            {
                return current;
            }
            current = current.eContainer();
        }
        return null;
    }

    public static String metadataObjectName(EObject eObject)
    {
        if (eObject instanceof MdObject mdObject)
        {
            return mdObject.getName();
        }
        try
        {
            Method getName = eObject.getClass().getMethod("getName"); //$NON-NLS-1$
            Object value = getName.invoke(eObject);
            if (value instanceof String name && !name.isEmpty())
            {
                return name;
            }
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
        return null;
    }

    public static String relativeStoragePath(EObject eObject)
    {
        if (eObject == null || eObject.eResource() == null)
        {
            return null;
        }
        URI uri = eObject.eResource().getURI();
        String platform = uri.toPlatformString(true);
        if (platform == null || platform.length() <= 1)
        {
            return null;
        }
        String[] segments = platform.split("/"); //$NON-NLS-1$
        if (segments.length <= 2)
        {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 2; i < segments.length; i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(segments[i]);
        }
        return builder.toString();
    }

    private static boolean containsStorageSegment(EObject eObject, String segment)
    {
        String rel = relativeStoragePath(eObject);
        if (rel == null)
        {
            return false;
        }
        for (String part : rel.split("/")) //$NON-NLS-1$
        {
            if (segment.equals(part))
            {
                return true;
            }
        }
        return false;
    }

    private static String folderRelativeFromStoragePath(String relPath, String subDir)
    {
        List<String> segments = splitSegments(relPath);
        for (int i = 0; i < segments.size(); i++)
        {
            if (subDir.equals(segments.get(i)) && i + 1 < segments.size())
            {
                return joinSegments(segments, 0, i + 2);
            }
        }
        return null;
    }

    private static String resolveProjectPath(EObject eObject, String projectRelative)
    {
        IProject project = project(eObject);
        if (project == null || !project.exists() || project.getLocation() == null)
        {
            return null;
        }
        IPath path = new Path(projectRelative);
        IFolder folder = project.getFolder(path);
        if (folder.exists())
        {
            return folder.getLocation().toOSString();
        }
        IFile file = project.getFile(path);
        if (file.exists())
        {
            IContainer parent = file.getParent();
            return parent != null ? parent.getLocation().toOSString() : null;
        }
        java.nio.file.Path nioPath = java.nio.file.Path.of(project.getLocation().append(path).toOSString());
        if (Files.isDirectory(nioPath))
        {
            return nioPath.toString();
        }
        if (Files.isRegularFile(nioPath))
        {
            java.nio.file.Path parent = nioPath.getParent();
            return parent != null ? parent.toString() : null;
        }
        return null;
    }

    private static String filePathFromStorageUri(EObject eObject)
    {
        String rel = relativeStoragePath(eObject);
        if (rel == null)
        {
            return null;
        }
        return resolveProjectPath(eObject, rel);
    }

    private static boolean isOwnerMetadataPath(EObject child, String candidate)
    {
        if (candidate == null || !candidate.endsWith(".mdo")) //$NON-NLS-1$
        {
            return false;
        }
        if (!(child instanceof BasicForm || child instanceof AbstractForm
            || child instanceof BasicCommand || child instanceof BasicTemplate
            || child instanceof Module))
        {
            return false;
        }
        EObject owner = findOwningMetadataObject(child);
        if (owner == null)
        {
            return false;
        }
        String ownerPath = null;
        IProjectFileSystemSupport support = fileSystemSupport(owner);
        if (support != null)
        {
            IPath path = support.getPath(owner);
            ownerPath = resolveResourcePath(owner, path);
        }
        if (ownerPath == null)
        {
            ownerPath = fileToAbsolutePath(platformFile(owner));
        }
        return candidate.equals(ownerPath);
    }

    private static boolean isSubElementFolder(String path, String subDir)
    {
        if (path == null)
        {
            return false;
        }
        String normalized = path.replace('\\', '/');
        return normalized.contains('/' + subDir + '/'); //$NON-NLS-1$
    }

    private static boolean isBslFile(String path)
    {
        return path != null && path.toLowerCase().endsWith(BSL_EXT);
    }

    private static boolean isFormMetadataPath(String path)
    {
        return path != null && path.toLowerCase().endsWith(".form"); //$NON-NLS-1$
    }

    private static String findFileInFolder(String folderPath, String fileName)
    {
        if (folderPath == null || fileName == null)
        {
            return null;
        }
        java.nio.file.Path file = java.nio.file.Path.of(folderPath, fileName);
        return Files.isRegularFile(file) ? file.toString() : null;
    }

    private static String findFileEndingWith(String folderPath, String suffix)
    {
        if (folderPath == null)
        {
            return null;
        }
        java.nio.file.Path folder = java.nio.file.Path.of(folderPath);
        if (!Files.isDirectory(folder))
        {
            return null;
        }
        try (var stream = Files.list(folder))
        {
            return stream.filter(Files::isRegularFile)
                .map(java.nio.file.Path::toString)
                .filter(path -> path.toLowerCase().endsWith(suffix.toLowerCase()))
                .sorted()
                .findFirst()
                .orElse(null);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static String findFileStartingWith(String folderPath, String prefix)
    {
        if (folderPath == null)
        {
            return null;
        }
        java.nio.file.Path folder = java.nio.file.Path.of(folderPath);
        if (!Files.isDirectory(folder))
        {
            return null;
        }
        try (var stream = Files.list(folder))
        {
            return stream.filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .filter(name -> name.startsWith(prefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .map(name -> folder.resolve(name).toString())
                .findFirst()
                .orElse(null);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static List<String> splitSegments(String path)
    {
        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/")) //$NON-NLS-1$
        {
            if (!segment.isEmpty())
            {
                segments.add(segment);
            }
        }
        return segments;
    }

    private static String joinSegments(List<String> segments, int from, int to)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < to; i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(segments.get(i));
        }
        return builder.toString();
    }

    private static String resolveFolderPath(EObject eObject, IPath path)
    {
        if (path == null)
        {
            return null;
        }
        IProject project = project(eObject);
        if (project == null || !project.exists())
        {
            return null;
        }
        IFolder folder = project.getFolder(path);
        if (folder.exists())
        {
            return folder.getLocation().toOSString();
        }
        IFile file = project.getFile(path);
        if (file.exists())
        {
            IContainer parent = file.getParent();
            return parent != null ? parent.getLocation().toOSString() : null;
        }
        return null;
    }

    private static String resolveResourcePath(EObject eObject, IPath path)
    {
        if (path == null)
        {
            return null;
        }
        IProject project = project(eObject);
        if (project == null || !project.exists() || project.getLocation() == null)
        {
            return null;
        }
        IFile file = project.getFile(path);
        if (file.exists())
        {
            return file.getLocation().toOSString();
        }
        IFolder folder = project.getFolder(path);
        if (folder.exists())
        {
            return folder.getLocation().toOSString();
        }
        IResource resource = project.getWorkspace().getRoot().getFileForLocation(project.getLocation().append(path));
        if (resource instanceof IFile fileResource && fileResource.exists())
        {
            return fileResource.getLocation().toOSString();
        }
        if (resource instanceof IContainer container && container.exists())
        {
            return container.getLocation().toOSString();
        }
        java.nio.file.Path nioPath = java.nio.file.Path.of(project.getLocation().append(path).toOSString());
        if (Files.exists(nioPath))
        {
            return nioPath.toString();
        }
        return null;
    }

    private static IProjectFileSystemSupport fileSystemSupport(EObject eObject)
    {
        Activator activator = Activator.getDefault();
        if (activator == null || activator.getFileSystemSupportProvider() == null)
        {
            return null;
        }
        IProject project = project(eObject);
        if (project == null)
        {
            return null;
        }
        return activator.getFileSystemSupportProvider().getProjectFileSystemSupport(project);
    }

    private static IProject project(EObject eObject)
    {
        Activator activator = Activator.getDefault();
        if (activator == null || activator.getResourceLookup() == null)
        {
            return null;
        }
        return activator.getResourceLookup().getProject(eObject);
    }

    private static IFile platformFile(EObject eObject)
    {
        Activator activator = Activator.getDefault();
        if (activator == null || activator.getResourceLookup() == null)
        {
            return null;
        }
        return activator.getResourceLookup().getPlatformResource(eObject);
    }

    private static String fileToAbsolutePath(IFile file)
    {
        if (file == null || !file.exists())
        {
            return null;
        }
        return file.getLocation().toOSString();
    }
}
