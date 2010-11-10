package org.infinitest.eclipse.workspace;

import static java.io.File.*;
import static java.util.logging.Level.*;
import static org.eclipse.core.resources.IMarker.*;
import static org.eclipse.core.resources.IResource.*;
import static org.eclipse.jdt.core.IJavaModelMarker.*;
import static org.eclipse.jdt.launching.JavaRuntime.*;
import static org.infinitest.util.InfinitestUtils.*;

import java.io.File;
import java.net.URI;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;

class ProjectFacade implements EclipseProject
{
    private final IJavaProject project;

    public ProjectFacade(IJavaProject project)
    {
        this.project = project;
    }

    /**
     * Returns a File that corresponds to the absolute location of the project's root directory.
     */
    public File workingDirectory()
    {
        IPath workingPath = project.getProject().getLocation();
        return workingPath.toFile();
    }

    // RISK Untested
    public File getJvmHome() throws CoreException
    {
        IVMInstall jvmInstall = getVMInstall(project);

        try
        {
            int majorVersion = parseMajorVersion(jvmInstall);
            if (majorVersion < 5)
            {
                jvmInstall = findFirstJava5OrGreaterJvm(jvmInstall);
            }
        }
        catch (UnknownJvmVersionException e)
        {
            log(WARNING, "Error determining JVM version. Using default version.");
        }

        if (jvmInstall != null && jvmInstall.getInstallLocation().exists())
            return jvmInstall.getInstallLocation();
        return new File(System.getProperty("java.home"));
    }

    private IVMInstall findFirstJava5OrGreaterJvm(IVMInstall jvmInstall) throws UnknownJvmVersionException
    {
        IVMInstall[] installs = jvmInstall.getVMInstallType().getVMInstalls();
        for (int i = 0; i < installs.length; i++)
        {
            IVMInstall candidate = installs[i];
            if (parseMajorVersion(candidate) >= 5)
            {
                return candidate;
            }
        }
        return null;
    }

    private int parseMajorVersion(IVMInstall jvmInstall) throws UnknownJvmVersionException
    {
        if (!(jvmInstall instanceof IVMInstall2))
        {
            throw new UnknownJvmVersionException();
        }

        String version = ((IVMInstall2) jvmInstall).getJavaVersion();

        if (version == null || version.length() < 3)
        {
            throw new UnknownJvmVersionException();
        }

        try
        {
            if (version.startsWith("1"))
            {
                return Integer.parseInt(version.substring(2, 3));
            }
            return Integer.parseInt(version.substring(0, 1));
        }
        catch (NumberFormatException e)
        {
            throw new UnknownJvmVersionException();
        }
    }

    private static class UnknownJvmVersionException extends Exception
    {
    }

    public boolean isOpen()
    {
        return project.getProject().isOpen();
    }

    /**
     * Returns a URI that can be used to uniquely identify a particular project.
     */
    public URI getLocationURI()
    {
        return project.getProject().getLocationURI();
    }

    public IClasspathEntry[] getClasspathEntries()
    {
        try
        {
            return project.getResolvedClasspath(true);
        }
        catch (JavaModelException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String getName()
    {
        return project.getElementName();
    }

    public boolean hasErrors() throws CoreException
    {
        int severity = project.getProject().findMaxProblemSeverity(JAVA_MODEL_PROBLEM_MARKER, false, DEPTH_INFINITE);
        return severity == SEVERITY_ERROR;
    }

    public String rawClasspath() throws CoreException
    {
        return StringUtils.join(computeDefaultRuntimeClassPath(project), pathSeparatorChar);
    }

    public IPath getDefaultOutputLocation()
    {
        try
        {
            return project.getOutputLocation();
        }
        catch (JavaModelException e)
        {
            throw new RuntimeException(e);
        }
    }
}