package ru.cursor.edt.copypath.ui.internal.path;

import com._1c.g5.v8.dt.bsl.model.Module;
import org.eclipse.emf.ecore.EObject;

import ru.cursor.edt.copypath.ui.internal.Messages;
import ru.cursor.edt.copypath.ui.internal.debug.CopyPathDebugLog;
import ru.cursor.edt.copypath.ui.internal.path.MenuEntry.MenuGroup;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Строит пункты меню на основе фактического списка файлов объекта метаданных.
 */
public final class PathCatalogBuilder
{
    private final List<MenuEntry> entries = new ArrayList<>();

    /**
     * Быстрая проверка для {@code PropertyTester} без полного сканирования дерева файлов.
     */
    public static boolean hasEntries(MetadataSelectionResolver.ResolvedSelection selection)
    {
        if (selection == null)
        {
            return false;
        }
        return switch (selection.getKind())
        {
            case METADATA_COLLECTION_FOLDER -> NavigatorAdapterSupport.isMetadataCollectionFolderAdapter(selection.getSource())
                && NavigatorAdapterSupport.collectionFolderPath(selection.getSource()) != null;
            case METADATA_ROOT -> {
                String folder = ObjectFileIndex.locateOwnerObjectFolder(selection.getEObject());
                yield MetadataObjectFolderLocator.isObjectRootFolder(folder);
            }
            case FORM -> hasSubElementFolder(selection.getEObject(), MetadataCollectionDirs.FORMS);
            case COMMAND -> hasSubElementFolder(selection.getEObject(), MetadataCollectionDirs.COMMANDS);
            case TEMPLATE -> hasSubElementFolder(selection.getEObject(), MetadataCollectionDirs.TEMPLATES);
            case MODULE -> {
                String path = PathResolver.moduleAbsolutePath((Module)selection.getEObject());
                yield path != null && !path.isBlank();
            }
            default -> false;
        };
    }

    public List<MenuEntry> build(MetadataSelectionResolver.ResolvedSelection selection)
    {
        entries.clear();
        if (selection == null)
        {
            return List.of();
        }
        switch (selection.getKind())
        {
            case METADATA_ROOT:
                buildMetadataRoot(selection.getEObject());
                break;
            case METADATA_COLLECTION_FOLDER:
                buildMetadataCollectionFolder(selection);
                break;
            case FORM:
                buildForm(selection.getEObject());
                break;
            case COMMAND:
                buildCommand(selection.getEObject());
                break;
            case TEMPLATE:
                buildTemplate(selection.getEObject());
                break;
            case MODULE:
                buildModule((Module)selection.getEObject());
                break;
            default:
                break;
        }
        return List.copyOf(entries);
    }

    private static boolean hasSubElementFolder(EObject subElement, String subDirName)
    {
        EObject owner = ObjectFileIndex.findOwnerMetadataObject(subElement);
        String name = ObjectFileIndex.metadataName(subElement);
        if (owner == null || name == null || name.isBlank())
        {
            return false;
        }
        String objectFolder = ObjectFileIndex.locateOwnerObjectFolder(owner);
        if (objectFolder == null)
        {
            return false;
        }
        return ObjectFileIndex.findSubElementFolderPath(objectFolder, subDirName, name) != null;
    }

    private void buildMetadataCollectionFolder(MetadataSelectionResolver.ResolvedSelection selection)
    {
        if (NavigatorAdapterSupport.isMetadataCollectionFolderAdapter(selection.getSource()))
        {
            addPath(Messages.Entry_objectFolder,
                NavigatorAdapterSupport.collectionFolderPath(selection.getSource()),
                MenuGroup.FOLDER_AND_MODULES);
        }
    }

    private void buildMetadataRoot(EObject root)
    {
        ObjectFileIndex index = ObjectFileIndex.scanCached(ObjectFileIndex.locateOwnerObjectFolder(root));
        if (index.isEmpty())
        {
            return;
        }
        addPath(Messages.Entry_objectFolder, index.getObjectFolder(), MenuGroup.FOLDER_AND_MODULES);
        for (Map.Entry<String, String> module : index.getRootModules().entrySet())
        {
            addPath(resolveRootModuleLabel(module.getKey()), module.getValue(), MenuGroup.FOLDER_AND_MODULES);
        }
        if (index.getMetadataFile() != null)
        {
            String ext = extensionOf(index.getMetadataFile());
            addPath(Messages.metadataFile(ext), index.getMetadataFile(), MenuGroup.METADATA);
        }
        for (Map.Entry<String, String> form : index.getFormFolders().entrySet())
        {
            addPath(Messages.form(form.getKey()), form.getValue(), MenuGroup.FORMS);
        }
        for (Map.Entry<String, String> command : index.getCommandFolders().entrySet())
        {
            addPath(Messages.command(command.getKey()), command.getValue(), MenuGroup.COMMANDS);
        }
        for (Map.Entry<String, String> template : index.getTemplateFolders().entrySet())
        {
            addPath(Messages.template(template.getKey()), template.getValue(), MenuGroup.TEMPLATES);
        }
        if (!index.getAllFiles().isEmpty())
        {
            entries.add(MenuEntry.action(Messages.Entry_allObjectFiles, index.getAllFiles(), MenuGroup.ALL_FILES));
        }
    }

    private void buildForm(EObject formObject)
    {
        buildSubElement(formObject, MetadataCollectionDirs.FORMS, Messages.Entry_formFolder,
            Messages.Entry_formModule, "Module.bsl", ".form", true); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void buildCommand(EObject commandObject)
    {
        buildSubElement(commandObject, MetadataCollectionDirs.COMMANDS, Messages.Entry_commandFolder,
            Messages.Entry_commandModule, "CommandModule.bsl", null, false); //$NON-NLS-1$
    }

    private void buildTemplate(EObject templateObject)
    {
        buildSubElement(templateObject, MetadataCollectionDirs.TEMPLATES, Messages.Entry_templateFolder,
            null, null, null, false);
    }

    private void buildSubElement(EObject subElement, String subDirName, String folderLabel,
        String moduleLabel, String moduleFileName, String metadataSuffix, boolean metadataFromIndex)
    {
        EObject owner = ObjectFileIndex.findOwnerMetadataObject(subElement);
        String elementName = ObjectFileIndex.metadataName(subElement);
        if (owner == null || elementName == null || elementName.isBlank())
        {
            return;
        }
        String objectFolder = ObjectFileIndex.locateOwnerObjectFolder(owner);
        CopyPathDebugLog.log("buildSubElement", "subDir=" + subDirName //$NON-NLS-1$ //$NON-NLS-2$
            + ", owner=" + CopyPathDebugLog.describe(owner) + ", name=" + elementName); //$NON-NLS-1$ //$NON-NLS-2$
        if (objectFolder == null)
        {
            return;
        }
        String elementFolder = ObjectFileIndex.findSubElementFolderPath(objectFolder, subDirName, elementName);
        if (elementFolder == null)
        {
            ObjectFileIndex index = ObjectFileIndex.scanCached(objectFolder);
            elementFolder = switch (subDirName)
            {
                case MetadataCollectionDirs.FORMS -> index.findFormFolder(elementName);
                case MetadataCollectionDirs.COMMANDS -> index.findCommandFolder(elementName);
                case MetadataCollectionDirs.TEMPLATES -> index.findTemplateFolder(elementName);
                default -> null;
            };
        }
        if (elementFolder == null)
        {
            return;
        }
        addPath(folderLabel, elementFolder, MenuGroup.FOLDER_AND_MODULES);
        if (moduleLabel != null)
        {
            ObjectFileIndex index = ObjectFileIndex.scanCached(objectFolder);
            String module = switch (subDirName)
            {
                case MetadataCollectionDirs.FORMS -> index.findFormModuleFile(elementFolder);
                case MetadataCollectionDirs.COMMANDS -> index.findCommandModuleFile(elementFolder);
                default -> null;
            };
            if (module == null && moduleFileName != null)
            {
                module = ObjectFileIndex.findFileInFolder(elementFolder, moduleFileName);
            }
            addPath(moduleLabel, module, MenuGroup.FOLDER_AND_MODULES);
        }
        String metadata = null;
        if (metadataSuffix != null)
        {
            ObjectFileIndex index = ObjectFileIndex.scanCached(objectFolder);
            if (metadataFromIndex && MetadataCollectionDirs.FORMS.equals(subDirName))
            {
                metadata = index.findFormMetadataFile(elementFolder);
            }
            if (metadata == null)
            {
                metadata = ObjectFileIndex.findFileEndingWith(elementFolder, metadataSuffix);
            }
            if (metadata == null && MetadataCollectionDirs.TEMPLATES.equals(subDirName))
            {
                metadata = index.findTemplateMetadataFile(elementFolder);
            }
            if (metadata != null)
            {
                addPath(Messages.metadataFile(extensionOf(metadata)), metadata, MenuGroup.METADATA);
            }
        }
    }

    private void buildModule(Module module)
    {
        addPath(resolveModuleLabel(module), PathResolver.moduleAbsolutePath(module), MenuGroup.FOLDER_AND_MODULES);
    }

    private String resolveRootModuleLabel(String fileName)
    {
        String stem = Path.of(fileName).getFileName().toString();
        int dot = stem.lastIndexOf('.');
        if (dot > 0)
        {
            stem = stem.substring(0, dot);
        }
        return switch (stem)
        {
            case "ObjectModule" -> Messages.Entry_objectModule; //$NON-NLS-1$
            case "ManagerModule" -> Messages.Entry_managerModule; //$NON-NLS-1$
            case "RecordSetModule" -> Messages.Entry_recordSetModule; //$NON-NLS-1$
            case "ValueManagerModule" -> Messages.Entry_objectModule; //$NON-NLS-1$
            default -> Messages.Entry_module;
        };
    }

    private String resolveModuleLabel(Module module)
    {
        String path = PathResolver.moduleAbsolutePath(module);
        String kind = PathResolver.moduleLabelFromPath(path);
        if (kind == null)
        {
            return Messages.Entry_module;
        }
        return switch (kind)
        {
            case "object" -> Messages.Entry_objectModule; //$NON-NLS-1$
            case "manager" -> Messages.Entry_managerModule; //$NON-NLS-1$
            case "recordSet" -> Messages.Entry_recordSetModule; //$NON-NLS-1$
            case "command" -> Messages.Entry_commandModule; //$NON-NLS-1$
            case "form" -> Messages.Entry_formModule; //$NON-NLS-1$
            default -> Messages.Entry_module;
        };
    }

    private String extensionOf(String path)
    {
        int dot = path.lastIndexOf('.');
        return dot >= 0 ? path.substring(dot + 1) : ""; //$NON-NLS-1$
    }

    private void addPath(String label, String path, MenuGroup group)
    {
        if (path != null && !path.isBlank())
        {
            entries.add(MenuEntry.action(label, path, group));
        }
    }
}
