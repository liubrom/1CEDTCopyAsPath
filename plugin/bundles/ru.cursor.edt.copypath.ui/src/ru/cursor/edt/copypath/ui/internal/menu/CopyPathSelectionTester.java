package ru.cursor.edt.copypath.ui.internal.menu;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.emf.ecore.EObject;

import ru.cursor.edt.copypath.ui.internal.debug.CopyPathDebugLog;
import ru.cursor.edt.copypath.ui.internal.path.MetadataObjectClassifier;
import ru.cursor.edt.copypath.ui.internal.path.MetadataSelectionResolver;
import ru.cursor.edt.copypath.ui.internal.path.NavigatorAdapterSupport;
import ru.cursor.edt.copypath.ui.internal.path.PathCatalogBuilder;
import ru.cursor.edt.copypath.ui.internal.path.SelectionKind;

public class CopyPathSelectionTester extends PropertyTester
{
    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue)
    {
        if (!"isSupported".equals(property)) //$NON-NLS-1$
        {
            return false;
        }
        try
        {
            boolean supported = hasMenuItems(receiver);
            CopyPathDebugLog.log("tester", "receiver=" + CopyPathDebugLog.describe(receiver) //$NON-NLS-1$ //$NON-NLS-2$
                + ", supported=" + supported); //$NON-NLS-1$
            return supported;
        }
        catch (RuntimeException e)
        {
            CopyPathDebugLog.log("tester-error", e.getClass().getSimpleName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
    }

    static boolean hasMenuItems(Object receiver)
    {
        Object element = MetadataSelectionResolver.unwrapMenuReceiver(receiver);
        CopyPathDebugLog.log("tester-resolve", "receiver=" + CopyPathDebugLog.describe(receiver) //$NON-NLS-1$ //$NON-NLS-2$
            + ", element=" + CopyPathDebugLog.describe(element)); //$NON-NLS-1$
        MetadataSelectionResolver.ResolvedSelection resolved =
            MetadataSelectionResolver.resolveForPropertyTest(receiver);
        if (resolved == null)
        {
            ContextMenuSelectionCache.clear();
            CopyPathDebugLog.log("tester", "supported=false (no context)"); //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        if (resolved.getKind() == SelectionKind.METADATA_COLLECTION_FOLDER)
        {
            if (!NavigatorAdapterSupport.isMetadataCollectionFolderAdapter(resolved.getSource()))
            {
                ContextMenuSelectionCache.clear();
                CopyPathDebugLog.log("tester", "supported=false (not collection folder)"); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
        }
        else
        {
            EObject eObject = resolved.getEObject();
            if (eObject == null || MetadataObjectClassifier.isNonFileMetadataNode(eObject))
            {
                ContextMenuSelectionCache.clear();
                CopyPathDebugLog.log("tester", "supported=false (non-file node)"); //$NON-NLS-1$ //$NON-NLS-2$
                return false;
            }
        }
        if (!PathCatalogBuilder.hasEntries(resolved))
        {
            ContextMenuSelectionCache.clear();
            CopyPathDebugLog.log("tester", "supported=false (no paths), kind=" + resolved.getKind()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            return false;
        }
        ContextMenuSelectionCache.rememberSelection(resolved);
        CopyPathDebugLog.log("tester", "supported=true, kind=" + resolved.getKind()); //$NON-NLS-1$ //$NON-NLS-2$
        return true;
    }
}
