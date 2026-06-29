package ru.cursor.edt.copypath.ui.internal.path;

/**
 * Имена папок коллекций метаданных под {@code src/}.
 */
public final class MetadataCollectionDirs
{
    public static final String SRC = "src"; //$NON-NLS-1$
    public static final String FORMS = "Forms"; //$NON-NLS-1$
    public static final String COMMANDS = "Commands"; //$NON-NLS-1$
    public static final String TEMPLATES = "Templates"; //$NON-NLS-1$

    private MetadataCollectionDirs()
    {
    }

    public static String dirForFeatureName(String featureName)
    {
        if (featureName == null || featureName.isBlank())
        {
            return null;
        }
        return switch (featureName)
        {
            case "externalReports" -> "ExternalReports"; //$NON-NLS-1$ //$NON-NLS-2$
            case "externalDataProcessors" -> "ExternalDataProcessors"; //$NON-NLS-1$ //$NON-NLS-2$
            case "documents" -> "Documents"; //$NON-NLS-1$ //$NON-NLS-2$
            case "catalogs" -> "Catalogs"; //$NON-NLS-1$ //$NON-NLS-2$
            case "reports" -> "Reports"; //$NON-NLS-1$ //$NON-NLS-2$
            case "dataProcessors" -> "DataProcessors"; //$NON-NLS-1$ //$NON-NLS-2$
            case "enums" -> "Enums"; //$NON-NLS-1$ //$NON-NLS-2$
            case "informationRegisters" -> "InformationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "accumulationRegisters" -> "AccumulationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "accountingRegisters" -> "AccountingRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "calculationRegisters" -> "CalculationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "exchangePlans" -> "ExchangePlans"; //$NON-NLS-1$ //$NON-NLS-2$
            case "businessProcesses" -> "BusinessProcesses"; //$NON-NLS-1$ //$NON-NLS-2$
            case "tasks" -> "Tasks"; //$NON-NLS-1$ //$NON-NLS-2$
            case "chartsOfCharacteristicTypes" -> "ChartsOfCharacteristicTypes"; //$NON-NLS-1$ //$NON-NLS-2$
            case "chartsOfAccounts" -> "ChartsOfAccounts"; //$NON-NLS-1$ //$NON-NLS-2$
            case "chartsOfCalculationTypes" -> "ChartsOfCalculationTypes"; //$NON-NLS-1$ //$NON-NLS-2$
            case "commonModules" -> "CommonModules"; //$NON-NLS-1$ //$NON-NLS-2$
            case "commonCommands" -> "CommonCommands"; //$NON-NLS-1$ //$NON-NLS-2$
            case "commonForms" -> "CommonForms"; //$NON-NLS-1$ //$NON-NLS-2$
            case "commonTemplates" -> "CommonTemplates"; //$NON-NLS-1$ //$NON-NLS-2$
            case "constants" -> "Constants"; //$NON-NLS-1$ //$NON-NLS-2$
            case "documentJournals" -> "DocumentJournals"; //$NON-NLS-1$ //$NON-NLS-2$
            case "filterCriteria" -> "FilterCriteria"; //$NON-NLS-1$ //$NON-NLS-2$
            case "settingsStorages" -> "SettingsStorages"; //$NON-NLS-1$ //$NON-NLS-2$
            default -> capitalizeFeatureName(featureName);
        };
    }

    public static String dirForObjectType(String objectTypeName)
    {
        if (objectTypeName == null || objectTypeName.isBlank())
        {
            return null;
        }
        return switch (objectTypeName)
        {
            case "Document" -> "Documents"; //$NON-NLS-1$ //$NON-NLS-2$
            case "Catalog" -> "Catalogs"; //$NON-NLS-1$ //$NON-NLS-2$
            case "Report" -> "Reports"; //$NON-NLS-1$ //$NON-NLS-2$
            case "DataProcessor" -> "DataProcessors"; //$NON-NLS-1$ //$NON-NLS-2$
            case "Enum" -> "Enums"; //$NON-NLS-1$ //$NON-NLS-2$
            case "InformationRegister" -> "InformationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "AccumulationRegister" -> "AccumulationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "AccountingRegister" -> "AccountingRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "CalculationRegister" -> "CalculationRegisters"; //$NON-NLS-1$ //$NON-NLS-2$
            case "ExchangePlan" -> "ExchangePlans"; //$NON-NLS-1$ //$NON-NLS-2$
            case "BusinessProcess" -> "BusinessProcesses"; //$NON-NLS-1$ //$NON-NLS-2$
            case "Task" -> "Tasks"; //$NON-NLS-1$ //$NON-NLS-2$
            case "ChartOfCharacteristicTypes" -> "ChartsOfCharacteristicTypes"; //$NON-NLS-1$ //$NON-NLS-2$
            case "ChartOfAccounts" -> "ChartsOfAccounts"; //$NON-NLS-1$ //$NON-NLS-2$
            case "ChartOfCalculationTypes" -> "ChartsOfCalculationTypes"; //$NON-NLS-1$ //$NON-NLS-2$
            case "CommonModule" -> "CommonModules"; //$NON-NLS-1$ //$NON-NLS-2$
            case "CommonCommand" -> "CommonCommands"; //$NON-NLS-1$ //$NON-NLS-2$
            case "CommonForm" -> "CommonForms"; //$NON-NLS-1$ //$NON-NLS-2$
            case "CommonTemplate" -> "CommonTemplates"; //$NON-NLS-1$ //$NON-NLS-2$
            case "Constant" -> "Constants"; //$NON-NLS-1$ //$NON-NLS-2$
            case "DocumentJournal" -> "DocumentJournals"; //$NON-NLS-1$ //$NON-NLS-2$
            case "FilterCriterion" -> "FilterCriteria"; //$NON-NLS-1$ //$NON-NLS-2$
            case "SettingsStorage" -> "SettingsStorages"; //$NON-NLS-1$ //$NON-NLS-2$
            default -> null;
        };
    }

    public static String dirForExternalNavigatorAdapter(String adapterClassName)
    {
        if (adapterClassName == null)
        {
            return null;
        }
        if (adapterClassName.contains("ExternalDataProcessorNavigatorAdapter")) //$NON-NLS-1$
        {
            return "ExternalDataProcessors"; //$NON-NLS-1$
        }
        if (adapterClassName.contains("ExternalReportNavigatorAdapter")) //$NON-NLS-1$
        {
            return "ExternalReports"; //$NON-NLS-1$
        }
        return null;
    }

    private static String capitalizeFeatureName(String featureName)
    {
        if (featureName.length() < 2)
        {
            return null;
        }
        return Character.toUpperCase(featureName.charAt(0)) + featureName.substring(1);
    }
}
