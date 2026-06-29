package ru.cursor.edt.copypath.ui.internal.path;

import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.metadata.mdclass.MdObject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public final class MetadataObjectClassifier
{
    private static final String[] ROOT_CONTAINMENT_FEATURES = {
        "documents", "catalogs", "enums", "reports", "dataProcessors", "informationRegisters", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
        "accumulationRegisters", "accountingRegisters", "calculationRegisters", "exchangePlans", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "businessProcesses", "tasks", "chartsOfCharacteristicTypes", "chartsOfAccounts", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
        "chartsOfCalculationTypes", "commonModules", "commonCommands", "commonForms", "commonTemplates", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "constants", "documentJournals", "filterCriteria", "settingsStorages", "webServices", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "httpServices", "wsReferences", "roles", "scheduledJobs", "sessionParameters", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        "definedTypes", "functionalOptions", "bots", "integrationServices" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    };

    private static final String[] SUB_ELEMENT_FEATURES = {
        "forms", "commands", "templates" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    };

    private MetadataObjectClassifier()
    {
    }

    public static boolean isNonFileMetadataNode(EObject eObject)
    {
        if (eObject instanceof com._1c.g5.v8.dt.metadata.mdclass.BasicTabularSection
            || eObject instanceof com._1c.g5.v8.dt.metadata.mdclass.Subsystem
            || eObject instanceof com._1c.g5.v8.dt.metadata.mdclass.CommandGroup)
        {
            return true;
        }
        if (eObject instanceof com._1c.g5.v8.dt.metadata.mdclass.CommonAttribute)
        {
            return true;
        }
        String simpleName = eObject.getClass().getSimpleName();
        if (simpleName.contains("TabularSection")) //$NON-NLS-1$
        {
            return true;
        }
        if (simpleName.endsWith("Attribute")) //$NON-NLS-1$
        {
            return true;
        }
        EStructuralFeature feature = eObject.eContainingFeature();
        if (feature == null)
        {
            return false;
        }
        String name = feature.getName();
        return "attributes".equals(name) //$NON-NLS-1$
            || "tabularSections".equals(name) //$NON-NLS-1$
            || "tabularSection".equals(name) //$NON-NLS-1$
            || "dimensions".equals(name) //$NON-NLS-1$
            || "resources".equals(name) //$NON-NLS-1$
            || "standardAttributes".equals(name) //$NON-NLS-1$
            || "characteristics".equals(name) //$NON-NLS-1$
            || "inputByString".equals(name) //$NON-NLS-1$
            || "registerRecords".equals(name) //$NON-NLS-1$
            || "addressingAttributes".equals(name) //$NON-NLS-1$
            || "accountingFlags".equals(name) //$NON-NLS-1$
            || "extDimensionAccountingFlags".equals(name); //$NON-NLS-1$
    }

    public static boolean isSubElement(EObject eObject, String featureName)
    {
        EStructuralFeature feature = eObject.eContainingFeature();
        return feature != null && featureName.equals(feature.getName());
    }

    public static boolean isAnySubElement(EObject eObject)
    {
        EStructuralFeature feature = eObject.eContainingFeature();
        if (feature == null)
        {
            return false;
        }
        String name = feature.getName();
        for (String subFeature : SUB_ELEMENT_FEATURES)
        {
            if (subFeature.equals(name))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isTopLevelMetadataObject(EObject eObject)
    {
        if (!(eObject instanceof MdObject) || eObject instanceof Configuration)
        {
            return false;
        }
        if (isNonFileMetadataNode(eObject) || isAnySubElement(eObject))
        {
            return false;
        }
        if (isDirectMetadataCollectionItem(eObject))
        {
            return true;
        }
        String folder = MetadataObjectFolderLocator.locateObjectFolder(eObject);
        return MetadataObjectFolderLocator.isObjectRootFolder(folder);
    }

    private static boolean isDirectMetadataCollectionItem(EObject eObject)
    {
        EObject container = eObject.eContainer();
        if (container instanceof Configuration)
        {
            return true;
        }
        EStructuralFeature feature = eObject.eContainingFeature();
        if (feature == null)
        {
            return false;
        }
        String featureName = feature.getName();
        for (String rootFeature : ROOT_CONTAINMENT_FEATURES)
        {
            if (rootFeature.equals(featureName))
            {
                return true;
            }
        }
        return false;
    }
}
