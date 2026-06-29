package ru.cursor.edt.copypath.ui.internal.path;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;

import java.lang.reflect.Method;

/**
 * Работа с адаптерами навигатора EDT без compile-time зависимости от
 * {@code com._1c.g5.v8.dt.navigator.adapters} (пакет не публикуется в p2 EDT).
 */
public final class NavigatorAdapterSupport
{
    private static final String COLLECTION_ADAPTER_CLASS =
        "com._1c.g5.v8.dt.navigator.adapters.CollectionNavigatorAdapterBase"; //$NON-NLS-1$
    private static final String MODEL_ADAPTER_CLASS =
        "com._1c.g5.v8.dt.navigator.adapters.ModelNavigatorAdapterBase"; //$NON-NLS-1$
    private static final String[] MODEL_GETTERS = {
        "getModel", "getTarget", "getData", "getElement", "getValue", "getObject", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "getBmModel", "getNotifier", "getSelectedElement" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    };

    private NavigatorAdapterSupport()
    {
    }

    public static boolean isCollectionFolderAdapter(Object element)
    {
        return isMetadataCollectionFolderAdapter(element);
    }

    public static boolean isMetadataCollectionFolderAdapter(Object element)
    {
        if (element == null)
        {
            return false;
        }
        if (isAssignableFrom(element, COLLECTION_ADAPTER_CLASS))
        {
            return true;
        }
        String name = element.getClass().getName();
        return name.contains("ExternalDataProcessorNavigatorAdapter$Folder") //$NON-NLS-1$
            || name.contains("ExternalReportNavigatorAdapter$Folder"); //$NON-NLS-1$
    }

    public static boolean isSubElementFolderAdapter(Object element)
    {
        if (element == null)
        {
            return false;
        }
        String name = element.getClass().getName();
        return name.contains("FormNavigatorAdapter$Folder") //$NON-NLS-1$
            || name.contains("CommandNavigatorAdapter$Folder") //$NON-NLS-1$
            || name.contains("TemplateNavigatorAdapter$Folder"); //$NON-NLS-1$
    }

    public static EObject unwrapToEObject(Object element)
    {
        if (element == null)
        {
            return null;
        }
        if (element instanceof EObject eObject)
        {
            return eObject;
        }
        if (element instanceof org.eclipse.core.runtime.IAdaptable adaptable)
        {
            EObject adapted = adaptable.getAdapter(EObject.class);
            if (adapted != null)
            {
                return adapted;
            }
        }
        if (isMetadataCollectionFolderAdapter(element) || isSubElementFolderAdapter(element))
        {
            return null;
        }
        return invokeModelAccessors(element);
    }

    public static String collectionFolderPath(Object adapter)
    {
        if (!isMetadataCollectionFolderAdapter(adapter))
        {
            return null;
        }
        if (isExternalCollectionFolderAdapter(adapter))
        {
            return externalCollectionFolderPath(adapter);
        }
        try
        {
            Method getModel = adapter.getClass().getMethod("getModel", boolean.class); //$NON-NLS-1$
            Object model = getModel.invoke(adapter, Boolean.FALSE);
            if (!(model instanceof EObject eObject))
            {
                return null;
            }
            Method getFeature = adapter.getClass().getMethod("getContentFeature"); //$NON-NLS-1$
            Object feature = getFeature.invoke(adapter);
            if (!(feature instanceof EReference eReference))
            {
                return null;
            }
            String fromApi = collectionFolderPath(eObject, eReference);
            if (fromApi != null)
            {
                return fromApi;
            }
            return collectionFolderPathFallback(eObject, eReference);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    private static boolean isExternalCollectionFolderAdapter(Object adapter)
    {
        String name = adapter.getClass().getName();
        return name.contains("ExternalDataProcessorNavigatorAdapter$Folder") //$NON-NLS-1$
            || name.contains("ExternalReportNavigatorAdapter$Folder"); //$NON-NLS-1$
    }

    private static String externalCollectionFolderPath(Object adapter)
    {
        String collectionDir = externalCollectionDirName(adapter);
        if (collectionDir == null)
        {
            return null;
        }
        IProject project = resolveProjectFromAdapter(adapter);
        if (project == null)
        {
            project = findUniqueOpenProjectWithCollectionDir(collectionDir);
        }
        if (project == null || !project.exists())
        {
            return null;
        }
        IFolder folder = project.getFolder(new Path(MetadataCollectionDirs.SRC).append(collectionDir));
        return folder.exists() ? folder.getLocation().toOSString() : null;
    }

    private static String externalCollectionDirName(Object adapter)
    {
        return MetadataCollectionDirs.dirForExternalNavigatorAdapter(adapter.getClass().getName());
    }

    private static IProject resolveProjectFromAdapter(Object adapter)
    {
        var activator = ru.cursor.edt.copypath.ui.internal.Activator.getDefault();
        if (activator == null || activator.getResourceLookup() == null)
        {
            return existingProject(resolveProjectWithoutActivator(adapter));
        }
        Object model = invokeAdapterModel(adapter);
        if (model instanceof EObject eObject)
        {
            IProject project = activator.getResourceLookup().getProject(eObject);
            if (project != null && project.exists())
            {
                return project;
            }
        }
        IProject direct = resolveProjectWithoutActivator(adapter);
        if (direct != null)
        {
            return direct;
        }
        Object outer = outerAdapterInstance(adapter);
        if (outer != null && outer != adapter)
        {
            Object outerModel = invokeAdapterModel(outer);
            if (outerModel instanceof EObject eObject)
            {
                IProject project = activator.getResourceLookup().getProject(eObject);
                if (project != null && project.exists())
                {
                    return project;
                }
            }
            return resolveProjectWithoutActivator(outer);
        }
        return null;
    }

    private static IProject resolveProjectWithoutActivator(Object adapter)
    {
        if (adapter instanceof org.eclipse.core.runtime.IAdaptable adaptable)
        {
            IProject project = adaptable.getAdapter(IProject.class);
            if (project != null && project.exists())
            {
                return project;
            }
        }
        for (String getter : new String[] { "getProject", "getIProject" }) //$NON-NLS-1$ //$NON-NLS-2$
        {
            try
            {
                Method method = adapter.getClass().getMethod(getter);
                Object result = method.invoke(adapter);
                if (result instanceof IProject project && project.exists())
                {
                    return project;
                }
            }
            catch (ReflectiveOperationException e)
            {
                // Пробуем следующий getter.
            }
        }
        return null;
    }

    private static Object invokeAdapterModel(Object adapter)
    {
        if (adapter == null)
        {
            return null;
        }
        try
        {
            Method getModel = adapter.getClass().getMethod("getModel", boolean.class); //$NON-NLS-1$
            return getModel.invoke(adapter, Boolean.FALSE);
        }
        catch (ReflectiveOperationException e)
        {
            // Пробуем getModel() без параметров.
        }
        try
        {
            Method getModel = adapter.getClass().getMethod("getModel"); //$NON-NLS-1$
            return getModel.invoke(adapter);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    private static Object outerAdapterInstance(Object adapter)
    {
        try
        {
            java.lang.reflect.Field outer = adapter.getClass().getDeclaredField("this$0"); //$NON-NLS-1$
            outer.setAccessible(true);
            return outer.get(adapter);
        }
        catch (ReflectiveOperationException e)
        {
            return null;
        }
    }

    private static IProject findUniqueOpenProjectWithCollectionDir(String collectionDir)
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject match = null;
        for (IProject project : root.getProjects())
        {
            if (!project.isOpen())
            {
                continue;
            }
            IFolder folder = project.getFolder(new Path(MetadataCollectionDirs.SRC).append(collectionDir));
            if (!folder.exists())
            {
                continue;
            }
            if (match != null)
            {
                return null;
            }
            match = project;
        }
        return match;
    }

    private static IProject existingProject(IProject project)
    {
        return project != null && project.exists() ? project : null;
    }

    private static String collectionFolderPath(EObject model, EReference feature)
    {
        var activator = ru.cursor.edt.copypath.ui.internal.Activator.getDefault();
        if (activator == null || activator.getFileSystemSupportProvider() == null
            || activator.getResourceLookup() == null)
        {
            return null;
        }
        var project = activator.getResourceLookup().getProject(model);
        if (project == null)
        {
            return null;
        }
        var support = activator.getFileSystemSupportProvider().getProjectFileSystemSupport(project);
        if (support == null)
        {
            return null;
        }
        var path = support.getPath(model, feature);
        return PathResolver.resolveProjectResourcePath(model, path);
    }

    private static String collectionFolderPathFallback(EObject model, EReference feature)
    {
        String collectionDir = MetadataCollectionDirs.dirForFeatureName(feature.getName());
        if (collectionDir == null)
        {
            return null;
        }
        var activator = ru.cursor.edt.copypath.ui.internal.Activator.getDefault();
        if (activator == null || activator.getResourceLookup() == null)
        {
            return null;
        }
        IProject project = activator.getResourceLookup().getProject(model);
        if (project == null || !project.exists())
        {
            return null;
        }
        IFolder folder = project.getFolder(new Path(MetadataCollectionDirs.SRC).append(collectionDir));
        return folder.exists() ? folder.getLocation().toOSString() : null;
    }

    private static boolean isAssignableFrom(Object element, String className)
    {
        if (element == null)
        {
            return false;
        }
        Class<?> type = element.getClass();
        while (type != null)
        {
            if (className.equals(type.getName()))
            {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static EObject invokeModelAccessors(Object element)
    {
        Object current = element;
        for (int depth = 0; depth < 6; depth++)
        {
            EObject direct = tryModelGetter(current);
            if (direct != null)
            {
                return direct;
            }
            Object next = unwrapOneLevel(current);
            if (next == null || next == current)
            {
                break;
            }
            current = next;
        }
        return null;
    }

    private static EObject tryModelGetter(Object element)
    {
        if (isAssignableFrom(element, MODEL_ADAPTER_CLASS))
        {
            try
            {
                Method getModel = element.getClass().getMethod("getModel", Object.class); //$NON-NLS-1$
                getModel.setAccessible(true);
                Object result = getModel.invoke(element, element);
                if (result instanceof EObject eObject)
                {
                    return eObject;
                }
            }
            catch (ReflectiveOperationException e)
            {
                // Пробуем другие getter'ы.
            }
        }
        try
        {
            Method getModel = element.getClass().getMethod("getModel", boolean.class); //$NON-NLS-1$
            Object result = getModel.invoke(element, Boolean.FALSE);
            if (result instanceof EObject eObject)
            {
                return eObject;
            }
        }
        catch (ReflectiveOperationException e)
        {
            // Пробуем другие getter'ы.
        }
        return unwrapOneLevelToEObject(element);
    }

    private static EObject unwrapOneLevelToEObject(Object element)
    {
        for (String getter : MODEL_GETTERS)
        {
            try
            {
                Method method = element.getClass().getMethod(getter);
                Object result = method.invoke(element);
                if (result instanceof EObject eObject)
                {
                    return eObject;
                }
            }
            catch (ReflectiveOperationException e)
            {
                // Пробуем следующий getter.
            }
        }
        return null;
    }

    private static Object unwrapOneLevel(Object element)
    {
        for (String getter : MODEL_GETTERS)
        {
            try
            {
                Method method = element.getClass().getMethod(getter);
                Object result = method.invoke(element);
                if (result != null)
                {
                    return result;
                }
            }
            catch (ReflectiveOperationException e)
            {
                // Пробуем следующий getter.
            }
        }
        return null;
    }
}
