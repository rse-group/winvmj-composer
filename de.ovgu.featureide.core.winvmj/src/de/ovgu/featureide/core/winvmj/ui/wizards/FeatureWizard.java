package de.ovgu.featureide.core.winvmj.ui.wizards;

import de.ovgu.featureide.core.winvmj.ui.wizards.pages.*;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.FileHandler;

import org.eclipse.swt.widgets.List;
import org.eclipse.core.resources.*;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

import org.eclipse.jface.wizard.Wizard;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import de.ovgu.featureide.fm.ui.wizards.WizardConstants;
import de.ovgu.featureide.fm.ui.wizards.AbstractWizard;

import de.ovgu.featureide.core.IFeatureProject;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import java.nio.file.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import de.ovgu.featureide.core.CorePlugin;
import de.ovgu.featureide.fm.core.base.impl.ConfigFormatManager;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FMCorePlugin;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
import de.ovgu.featureide.fm.core.io.IPersistentFormat;
import de.ovgu.featureide.fm.core.io.manager.SimpleFileHandler;
import de.ovgu.featureide.fm.ui.handlers.base.SelectionWrapper;
import de.ovgu.featureide.ui.UIPlugin;
import de.ovgu.featureide.fm.core.configuration.Selection;
import java.io.IOException;
import java.nio.file.Paths;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;

import static de.ovgu.featureide.fm.core.localization.StringTable.CONTAINER_DOES_NOT_EXIST_;
import static de.ovgu.featureide.fm.core.localization.StringTable.CREATING;
import static de.ovgu.featureide.fm.core.localization.StringTable.NEW_CONFIGURATION;
import static de.ovgu.featureide.fm.core.localization.StringTable.OPENING_FILE_FOR_EDITING___;

public class FeatureWizard extends Wizard {

    private ProjectNameWizardPage projectNamePage;
    private FeatureWizardPage featureWizardPage;
    private SelectFeaturesWizardPage selectFeaturesWizardPage;
    private final Map<String, Object> dataMap = new HashMap<String, Object>();
    private IFeatureProject project;
    

    public FeatureWizard() {
        setWindowTitle("New Feature Wizard");
    }

    public void setProject(IFeatureProject project) {
        this.project = project;
    }
    
    public Map<String, Object> getDataMap() {
        return this.dataMap;
    }

    @Override
    public void addPages() {
        projectNamePage = new ProjectNameWizardPage();
        addPage(projectNamePage);

        featureWizardPage = new FeatureWizardPage();
        featureWizardPage.setProject(this.project);
        addPage(featureWizardPage);

        selectFeaturesWizardPage = new SelectFeaturesWizardPage();
        selectFeaturesWizardPage.setProject(this.project);
        selectFeaturesWizardPage.setDataMap(featureWizardPage.getDataMap());
        addPage(selectFeaturesWizardPage);
    }

    @Override
    public boolean performFinish() {
        String projectName = projectNamePage.getProjectName();
        HashSet<String> featureNames = selectFeaturesWizardPage.getFeatureName();
		final FeatureModelFormula featureModel = this.project.getFeatureModelManager().getPersistentFormula();
		final IPersistentFormat<Configuration> format = ConfigFormatManager.getInstance().getDefaultFormat();

		final String suffix = "." + format.getSuffix();
		final String name = projectName;
		final String fileName = name + (name.endsWith(suffix) ? "" : suffix);

		final IRunnableWithProgress op = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(fileName, featureModel, format, monitor, featureNames);
				} catch (final CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (final InterruptedException e) {
			return false;
		} catch (final InvocationTargetException e) {
			final Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
        
        return true;
    }

    private void doFinish(String fileName, FeatureModelFormula featureModel, IPersistentFormat<Configuration> format, 
	IProgressMonitor monitor, HashSet<String> selectedFeature)
			throws CoreException {
		// create a sample file
		monitor.beginTask(CREATING + fileName, 2);
		final IFolder configFolder = this.project.getConfigFolder();
		final IContainer container = configFolder == null ? this.project.getProject() : configFolder;
		if (!container.exists()) {
			if (this.project.getProject().isAccessible()) {
				FMCorePlugin.createFolder(this.project.getProject(), container.getProjectRelativePath().toString());
			} else {
				throwCoreException(CONTAINER_DOES_NOT_EXIST_);
			}
		}

		final Path configPath = EclipseFileSystem.getPath(container);
		final Path file = configPath.resolve(fileName);
		Configuration config = new Configuration(featureModel);
		for (String feature : selectedFeature) {
			config.setManual(feature, Selection.SELECTED);
		}
		SimpleFileHandler.save(configPath.resolve(fileName), config, format);

		this.project.setCurrentConfiguration(file);

		monitor.worked(1);
		monitor.setTaskName(OPENING_FILE_FOR_EDITING___);
		getShell().getDisplay().asyncExec(new Runnable() {

			@Override
			public void run() {
				final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, (IFile) EclipseFileSystem.getResource(file), true);
				} catch (final PartInitException e) {}
			}
		});
		monitor.worked(1);
	}

    private void throwCoreException(String message) throws CoreException {
		final IStatus status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}

    @Override
    public boolean canFinish() {
        return getContainer().getCurrentPage() == selectFeaturesWizardPage && selectFeaturesWizardPage.isPageComplete();
    }
}
