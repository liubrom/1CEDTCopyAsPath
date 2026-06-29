package ru.cursor.edt.copypath.ui.internal;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;

public class Activator extends Plugin
{
    public static final String PLUGIN_ID = "ru.cursor.edt.copypath.ui"; //$NON-NLS-1$

    private static Activator plugin;

    private IResourceLookup resourceLookup;
    private IProjectFileSystemSupportProvider fileSystemSupportProvider;

    @Override
    public void start(BundleContext context) throws Exception
    {
        super.start(context);
        plugin = this;
        resourceLookup = getService(context, IResourceLookup.class);
        fileSystemSupportProvider = getService(context, IProjectFileSystemSupportProvider.class);
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        plugin = null;
        resourceLookup = null;
        fileSystemSupportProvider = null;
        super.stop(context);
    }

    public static Activator getDefault()
    {
        return plugin;
    }

    public IResourceLookup getResourceLookup()
    {
        return resourceLookup;
    }

    public IProjectFileSystemSupportProvider getFileSystemSupportProvider()
    {
        return fileSystemSupportProvider;
    }

    private static <T> T getService(BundleContext context, Class<T> clazz)
    {
        ServiceReference<T> reference = context.getServiceReference(clazz);
        if (reference == null)
        {
            return null;
        }
        return context.getService(reference);
    }
}
