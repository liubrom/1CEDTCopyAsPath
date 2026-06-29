package ru.cursor.edt.copypath.ui.internal.path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import com._1c.g5.v8.dt.metadata.mdclass.MdObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Пути к файлам хранения EMF-объекта по URI ресурса (как в эталонном плагине).
 */
public final class EmfStorageRelativePath
{
    private static final String SRC_FOLDER = "src"; //$NON-NLS-1$
    private static final String FORMS_DIR = "Forms"; //$NON-NLS-1$
    private static final String COMMANDS_DIR = "Commands"; //$NON-NLS-1$
    private static final String TEMPLATES_DIR = "Templates"; //$NON-NLS-1$

    private EmfStorageRelativePath()
    {
    }

    public static boolean hasStorageContext(EObject eObject)
    {
        return getRelativePath(eObject) != null || inferElementFolderRelative(eObject) != null;
    }

    public static String getRelativePath(EObject eObject)
    {
        URI uri = resourceUri(eObject);
        if (uri == null)
        {
            return null;
        }
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

    public static String elementFolderAbsolute(EObject eObject, String subDirName)
    {
        String relative = elementFolderRelative(eObject, subDirName);
        if (relative == null)
        {
            relative = inferElementFolderRelative(eObject, subDirName);
        }
        return absoluteFromRelative(eObject, relative);
    }

    public static String topObjectFolderAbsolute(EObject eObject)
    {
        String relative = topObjectFolderRelative(eObject);
        if (relative == null)
        {
            relative = inferTopObjectFolderRelative(eObject);
        }
        return absoluteFromRelative(eObject, relative);
    }

    public static String elementFolderRelative(EObject eObject, String subDirName)
    {
        String rel = getRelativePath(eObject);
        if (rel == null)
        {
            return null;
        }
        List<String> segments = splitPath(rel);
        int subDirIdx = indexOfSegment(segments, subDirName);
        if (subDirIdx >= 0 && subDirIdx + 2 <= segments.size())
        {
            return joinSegments(segments, 1, subDirIdx + 2);
        }
        return null;
    }

    public static String topObjectFolderRelative(EObject eObject)
    {
        String rel = getRelativePath(eObject);
        if (rel == null)
        {
            return null;
        }
        List<String> segments = splitPath(rel);
        for (String subDir : new String[] { FORMS_DIR, COMMANDS_DIR, TEMPLATES_DIR })
        {
            int subDirIdx = indexOfSegment(segments, subDir);
            if (subDirIdx >= 1)
            {
                return joinSegments(segments, 1, subDirIdx);
            }
        }
        if (segments.size() >= 3)
        {
            return joinSegments(segments, 1, 3);
        }
        return null;
    }

    private static String inferElementFolderRelative(EObject eObject)
    {
        return inferElementFolderRelative(eObject, null);
    }

    private static String inferElementFolderRelative(EObject eObject, String subDirName)
    {
        String top = inferTopObjectFolderRelative(eObject);
        if (top == null)
        {
            return null;
        }
        String name = ObjectFileIndex.metadataName(eObject);
        if (name == null || name.isBlank())
        {
            return null;
        }
        if (subDirName == null)
        {
            subDirName = inferSubDirName(eObject);
        }
        if (subDirName == null)
        {
            return null;
        }
        return top + '/' + subDirName + '/' + name;
    }

    private static String inferSubDirName(EObject eObject)
    {
        if (MetadataObjectClassifier.isSubElement(eObject, "forms")) //$NON-NLS-1$
        {
            return FORMS_DIR;
        }
        if (MetadataObjectClassifier.isSubElement(eObject, "commands")) //$NON-NLS-1$
        {
            return COMMANDS_DIR;
        }
        if (MetadataObjectClassifier.isSubElement(eObject, "templates")) //$NON-NLS-1$
        {
            return TEMPLATES_DIR;
        }
        return null;
    }

    private static String inferTopObjectFolderRelative(EObject eObject)
    {
        EObject current = eObject;
        while (current != null)
        {
            if (current instanceof MdObject && !(current instanceof com._1c.g5.v8.dt.metadata.mdclass.Configuration))
            {
                String rel = topObjectFolderRelative(current);
                if (rel != null)
                {
                    return rel;
                }
                String collection = MetadataCollectionDirs.dirForObjectType(current.eClass().getName());
                String name = ObjectFileIndex.metadataName(current);
                if (collection != null && name != null && !name.isBlank())
                {
                    return collection + '/' + name;
                }
            }
            current = current.eContainer();
        }
        return null;
    }

    public static String absoluteFromRelative(EObject eObject, String relative)
    {
        if (relative == null || relative.isBlank())
        {
            return null;
        }
        IContainer container = getContainer(eObject, relative);
        if (container != null && container.exists())
        {
            return container.getLocation().toOSString();
        }
        String fromApi = PathResolver.absolutePath(eObject);
        if (fromApi != null)
        {
            java.nio.file.Path candidate = java.nio.file.Path.of(fromApi);
            while (candidate != null)
            {
                String rel = toRelativeUnderSrc(candidate.toString());
                if (relative.equalsIgnoreCase(rel))
                {
                    return candidate.toString();
                }
                candidate = candidate.getParent();
            }
        }
        return null;
    }

    public static IFile findFile(EObject eObject, String relativeFile)
    {
        IProject project = getProject(eObject);
        if (project == null)
        {
            return null;
        }
        IPath full = new Path(SRC_FOLDER).append(relativeFile);
        IFile file = project.getFile(full);
        return file.exists() ? file : null;
    }

    private static String toRelativeUnderSrc(String absolute)
    {
        String normalized = absolute.replace('\\', '/');
        int idx = normalized.indexOf("/src/"); //$NON-NLS-1$
        if (idx < 0)
        {
            return null;
        }
        return normalized.substring(idx + "/src/".length()); //$NON-NLS-1$
    }

    private static IContainer getContainer(EObject eObject, String relative)
    {
        IProject project = getProject(eObject);
        if (project == null)
        {
            return null;
        }
        IPath full = new Path(SRC_FOLDER).append(relative);
        IResource resource = project.findMember(full);
        if (resource instanceof IContainer container && container.exists())
        {
            return container;
        }
        IFolder folder = project.getFolder(full);
        return folder.exists() ? folder : null;
    }

    private static IProject getProject(EObject eObject)
    {
        URI uri = resourceUri(eObject);
        if (uri == null)
        {
            return null;
        }
        String platform = uri.toPlatformString(true);
        if (platform == null)
        {
            return null;
        }
        String[] segments = platform.split("/"); //$NON-NLS-1$
        if (segments.length < 2)
        {
            return null;
        }
        String projectName = segments[1];
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        return project.exists() ? project : null;
    }

    private static URI resourceUri(EObject eObject)
    {
        Resource resource = eObject.eResource();
        return resource != null ? resource.getURI() : null;
    }

    private static List<String> splitPath(String path)
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

    private static int indexOfSegment(List<String> segments, String value)
    {
        for (int i = 0; i < segments.size(); i++)
        {
            if (value.equals(segments.get(i)))
            {
                return i;
            }
        }
        return -1;
    }

    private static String joinSegments(List<String> segments, int fromInclusive, int toExclusive)
    {
        StringBuilder builder = new StringBuilder();
        for (int i = fromInclusive; i < toExclusive; i++)
        {
            if (builder.length() > 0)
            {
                builder.append('/');
            }
            builder.append(segments.get(i));
        }
        return builder.toString();
    }
}
