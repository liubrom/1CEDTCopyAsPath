package ru.cursor.edt.copypath.ui.internal.path;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.metadata.mdclass.AbstractForm;
import com._1c.g5.v8.dt.metadata.mdclass.BasicCommand;
import com._1c.g5.v8.dt.metadata.mdclass.BasicForm;
import com._1c.g5.v8.dt.metadata.mdclass.BasicTemplate;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.ISources;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;

import ru.cursor.edt.copypath.ui.internal.debug.CopyPathDebugLog;

import java.lang.reflect.Method;
import java.util.Collection;

public final class MetadataSelectionResolver
{
    private static final ThreadLocal<Boolean> RESOLVING_MENU_SELECTION = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final String[] UNWRAP_GETTERS = {
        "getModel", "getTarget", "getData", "getElement", "getValue", "getObject", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "getBmModel", "getNotifier", "getSelectedElement" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    };

    private MetadataSelectionResolver()
    {
    }

    public static ResolvedSelection resolve(ISelection selection)
    {
        if (!(selection instanceof IStructuredSelection structured) || structured.isEmpty())
        {
            return null;
        }
        return resolveElement(structured.getFirstElement());
    }

    /**
     * Разворачивает {@code receiver} из {@code visibleWhen}/{@code PropertyTester}:
     * иногда Eclipse передаёт {@link IStructuredSelection}, а не элемент дерева.
     */
    public static Object unwrapMenuReceiver(Object receiver)
    {
        if (receiver instanceof IStructuredSelection structured)
        {
            if (structured.isEmpty())
            {
                return null;
            }
            return structured.getFirstElement();
        }
        return receiver;
    }

    /**
     * Только для {@code PropertyTester}: без {@code IEvaluationService} и без fallback.
     */
    public static ResolvedSelection resolveForPropertyTest(Object receiver)
    {
        Object element = unwrapMenuReceiver(receiver);
        if (element != null)
        {
            ResolvedSelection resolved = resolveElement(element);
            if (resolved != null)
            {
                return resolved;
            }
        }
        if (receiver != null && receiver != element)
        {
            return resolveElement(receiver);
        }
        return null;
    }

    /**
     * Для {@link ru.cursor.edt.copypath.ui.internal.menu.CopyPathDynamicMenu#fill}:
     * узел под курсором, затем выделение навигатора.
     */
    public static ResolvedSelection resolveForFill()
    {
        ResolvedSelection fromMenu = resolveActiveMenuSelection();
        if (fromMenu != null)
        {
            return fromMenu;
        }
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
        {
            return null;
        }
        return resolve(window.getSelectionService().getSelection());
    }

    public static ResolvedSelection resolveActiveNavigatorSelection()
    {
        ResolvedSelection fromMenu = resolveActiveMenuSelection();
        if (fromMenu != null)
        {
            return fromMenu;
        }
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null)
        {
            return null;
        }
        return resolve(window.getSelectionService().getSelection());
    }

    private static ResolvedSelection resolveActiveMenuSelection()
    {
        if (Boolean.TRUE.equals(RESOLVING_MENU_SELECTION.get()))
        {
            CopyPathDebugLog.log("menu-selection", "skip reentrant activeMenuSelection read"); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        RESOLVING_MENU_SELECTION.set(Boolean.TRUE);
        try
        {
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null)
            {
                return null;
            }
            IWorkbenchPage page = window.getActivePage();
            if (page == null)
            {
                return null;
            }
            IEvaluationService evaluationService = window.getService(IEvaluationService.class);
            if (evaluationService == null)
            {
                return null;
            }
            Object menuSelection = evaluationService.getCurrentState().getVariable(ISources.ACTIVE_MENU_SELECTION_NAME);
            CopyPathDebugLog.log("menu-selection", "activeMenuSelection=" + CopyPathDebugLog.describe(menuSelection)); //$NON-NLS-1$ //$NON-NLS-2$
            return resolveMenuSelection(menuSelection);
        }
        finally
        {
            RESOLVING_MENU_SELECTION.set(Boolean.FALSE);
        }
    }

    private static ResolvedSelection resolveMenuSelection(Object menuSelection)
    {
        if (menuSelection instanceof ISelection selection)
        {
            return resolve(selection);
        }
        if (menuSelection instanceof Collection<?> collection && !collection.isEmpty())
        {
            return resolveElement(collection.iterator().next());
        }
        return null;
    }

    public static ResolvedSelection resolveElement(Object element)
    {
        Object unwrapped = unwrapMenuReceiver(element);
        if (unwrapped != null && unwrapped != element)
        {
            element = unwrapped;
        }
        if (element instanceof IProject)
        {
            return null;
        }
        if (NavigatorAdapterSupport.isMetadataCollectionFolderAdapter(element))
        {
            return new ResolvedSelection(element, null, SelectionKind.METADATA_COLLECTION_FOLDER);
        }
        EObject eObject = toEObject(element);
        if (eObject == null)
        {
            return null;
        }
        SelectionKind kind = detectKind(eObject);
        if (kind == SelectionKind.UNSUPPORTED)
        {
            return null;
        }
        return new ResolvedSelection(element, eObject, kind);
    }

    private static EObject toEObject(Object element)
    {
        EObject unwrapped = NavigatorAdapterSupport.unwrapToEObject(element);
        if (unwrapped != null)
        {
            return unwrapped;
        }
        if (element == null)
        {
            return null;
        }
        Object current = element;
        for (int depth = 0; depth < 6; depth++)
        {
            if (current instanceof EObject eObject)
            {
                return eObject;
            }
            if (current instanceof org.eclipse.core.runtime.IAdaptable adaptable)
            {
                EObject adapted = adaptable.getAdapter(EObject.class);
                if (adapted != null)
                {
                    return adapted;
                }
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

    private static Object unwrapOneLevel(Object element)
    {
        for (String getter : UNWRAP_GETTERS)
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

    private static SelectionKind detectKind(EObject eObject)
    {
        if (eObject instanceof Configuration)
        {
            return SelectionKind.UNSUPPORTED;
        }
        if (MetadataObjectClassifier.isNonFileMetadataNode(eObject))
        {
            return SelectionKind.UNSUPPORTED;
        }
        if (eObject instanceof Module)
        {
            return SelectionKind.MODULE;
        }
        if (MetadataObjectClassifier.isSubElement(eObject, "forms") || isFormContext(eObject)) //$NON-NLS-1$
        {
            return SelectionKind.FORM;
        }
        if (MetadataObjectClassifier.isSubElement(eObject, "commands") || isCommandContext(eObject)) //$NON-NLS-1$
        {
            return SelectionKind.COMMAND;
        }
        if (MetadataObjectClassifier.isSubElement(eObject, "templates") || isTemplateContext(eObject)) //$NON-NLS-1$
        {
            return SelectionKind.TEMPLATE;
        }
        if (MetadataObjectClassifier.isTopLevelMetadataObject(eObject))
        {
            return SelectionKind.METADATA_ROOT;
        }
        SelectionKind fileIndexKind = ObjectFileIndex.detectSubElementKind(eObject);
        if (fileIndexKind != null)
        {
            return fileIndexKind;
        }
        return SelectionKind.UNSUPPORTED;
    }

    private static boolean isFormContext(EObject eObject)
    {
        if (eObject instanceof BasicForm || eObject instanceof AbstractForm)
        {
            return true;
        }
        return PathResolver.isFormStorageContext(eObject);
    }

    private static boolean isCommandContext(EObject eObject)
    {
        if (eObject instanceof BasicCommand)
        {
            return true;
        }
        return PathResolver.isCommandStorageContext(eObject);
    }

    private static boolean isTemplateContext(EObject eObject)
    {
        if (eObject instanceof BasicTemplate)
        {
            return true;
        }
        return PathResolver.isTemplateStorageContext(eObject);
    }

    public static final class ResolvedSelection
    {
        private final Object source;
        private final EObject eObject;
        private final SelectionKind kind;

        public ResolvedSelection(Object source, EObject eObject, SelectionKind kind)
        {
            this.source = source;
            this.eObject = eObject;
            this.kind = kind;
        }

        public Object getSource()
        {
            return source;
        }

        public EObject getEObject()
        {
            return eObject;
        }

        public SelectionKind getKind()
        {
            return kind;
        }
    }
}
