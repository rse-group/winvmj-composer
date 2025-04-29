package de.ovgu.featureide.core.winvmj.ui.wizards;

import de.ovgu.featureide.core.winvmj.core.WinVMJProduct;
import de.ovgu.featureide.core.winvmj.core.impl.ProductToCompose;
import de.ovgu.featureide.core.winvmj.runtime.WinVMJConsole;
import de.ovgu.featureide.core.winvmj.templates.impl.MultiLevelConfiguration;
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
import org.eclipse.jface.wizard.IWizardPage;
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
import de.ovgu.featureide.core.winvmj.templates.impl.MultiLevelConfiguration;

import de.ovgu.featureide.core.IFeatureProject;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import java.nio.file.Path;

import org.eclipse.core.resources.IFile;
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
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.base.IFeatureStructure;
import de.ovgu.featureide.fm.core.base.impl.ConfigFormatManager;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.fm.core.FMCorePlugin;
import de.ovgu.featureide.fm.core.analysis.cnf.formula.FeatureModelFormula;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.io.EclipseFileSystem;
import de.ovgu.featureide.fm.core.io.IPersistentFormat;
import de.ovgu.featureide.fm.core.io.manager.SimpleFileHandler;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.monitor.IMonitor;
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
	private MultiLevelConfiguration multiStageConfiguration = new MultiLevelConfiguration();
	private SelectAllUvlWizardPage selectAllUvlWizardPage;
    private FeatureWizardPage featureWizardPage;
    private SelectFeaturesWizardPage selectFeaturesWizardPage;
	private ConfirmationSelectionWizardPage confirmationSelectionWizardPage;
    private final Map<String, Object> dataMap = new HashMap<String, Object>();
    private IFeatureProject project;
	private boolean hasSetupFeatureSelection = false;
	private ArrayList<String> selectedUvl =  new ArrayList<>();
    private Map<String, IWizardPage> pageMap = new HashMap<String, IWizardPage>();
	private String selectedPage;
	private Map<String, HashSet<String>> selectedFeaturesMap = new HashMap<>();
	private WinVMJProduct product;

    public FeatureWizard() {
        setWindowTitle("New Feature Wizard");
    }

    public void setProject(IFeatureProject project) {
        this.project = project;
    }
    
    public Map<String, Object> getDataMap() {
        return this.dataMap;
    }
    
    public Map<String, HashSet<String>> getSelectedFeaturesMap() {
    	return selectedFeaturesMap;
    }
    
    public void setSelectedFeaturesMap(Map<String, HashSet<String>> selectedFeaturesMap) {
        this.selectedFeaturesMap = selectedFeaturesMap;
    }
    
    public int getPageIndex(IWizardPage page) {
    	int index = 0;
    	for (IWizardPage p : getPages()) {
    		if (p == page) return index;
    		index++;
    	}
    	return -1;
    }


    @Override
    public void addPages() {
        projectNamePage = new ProjectNameWizardPage();
        addPage(projectNamePage);

		// Section for Sorting Model
		IFile chosenFile = null;

		for (IFile file : multiStageConfiguration.getAllFeatureModelNames(this.project)) {
            if (file.getName().equals("model.uvl")) {
				chosenFile = file;
			}
        }

		IFeature root = multiStageConfiguration.loadFeatureModel(chosenFile).getStructure().getRoot().getFeature();
		for (final IFeatureStructure feature : root.getStructure().getChildren()) {
			String featureName = feature.getFeature().getName().split("\\.")[1];
			SelectFeaturesWizardPage page = new SelectFeaturesWizardPage();
			page.setProject(this.project);
			page.setSelectedFile(featureName+".uvl");
			addPage(page);

		}
		hasSetupFeatureSelection = true;
    }
    
	@Override
	public IWizardPage getNextPage(IWizardPage currentPage) {
		if ((!hasSetupFeatureSelection) && (currentPage ==  confirmationSelectionWizardPage)) {
			for (int i = 0; i < selectAllUvlWizardPage.getSelected().size(); i++) {
				SelectFeaturesWizardPage selectPage = new SelectFeaturesWizardPage();
				selectPage.setProject(this.project);
				selectPage.setSelectedFile(selectAllUvlWizardPage.getSelected().get(i));
				if (i == 0) {
					selectPage.setFirst();
				}
				addPage(selectPage);
			}

			hasSetupFeatureSelection = true;
		}

		else if ((hasSetupFeatureSelection) && (currentPage instanceof SelectFeaturesWizardPage)) {
			SelectFeaturesWizardPage currentSelectPage = (SelectFeaturesWizardPage) currentPage;
			
			IWizardPage nextPage = super.getNextPage(currentPage);
			if (nextPage instanceof SelectFeaturesWizardPage) {
				SelectFeaturesWizardPage nextSelectPage = (SelectFeaturesWizardPage) nextPage;
				
				if (selectedFeaturesMap.isEmpty()) {
					nextSelectPage.setAllowedParent(currentSelectPage.getFeatureName());
				}
				else {
					HashSet<String> allSelectedFeatures = new HashSet<>();
					for (HashSet<String> features : selectedFeaturesMap.values()) {
						allSelectedFeatures.addAll(features);
					}
					
					nextSelectPage.setAllowedParent(allSelectedFeatures);
				}
				
				selectedFeaturesMap.put(currentSelectPage.getSelectedFile(), new HashSet<>(currentSelectPage.getFeatureName()));
				
				return (IWizardPage) nextSelectPage;
			}
		}
		
		return super.getNextPage(currentPage);
	}

    @Override
    public boolean performFinish() {
    	IWizardPage currentPage = getContainer().getCurrentPage();
        if (currentPage instanceof SelectFeaturesWizardPage) {
            SelectFeaturesWizardPage lastSelectPage = (SelectFeaturesWizardPage) currentPage;
            selectedFeaturesMap.put(lastSelectPage.getSelectedFile(), new HashSet<>(lastSelectPage.getFeatureName()));
        }
        
        String productName = projectNamePage.getProjectName();
        
        ArrayList<IFeature> featureList =  multiStageConfiguration.convertSelectedFeaturesToList(selectedFeaturesMap, project);
        
//        WinVMJConsole.println("Features " + featureList.toString());

		final IRunnableWithProgress op = new IRunnableWithProgress() {

			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					doFinish(productName, featureList);
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

    private void doFinish(String productName, ArrayList<IFeature> selectedFeature)
			throws CoreException {
        product = new ProductToCompose(project, productName, selectedFeature);
        final LongRunningMethod<Boolean> job = new LongRunningMethod<Boolean>() {
            @Override
            public Boolean execute(IMonitor<Boolean> workMonitor) throws Exception {
            	multiStageConfiguration.composeProduct(product, selectedFeature, project);
                return true;
            }
        };
        LongRunningWrapper.getRunner(job, "Compose Product").schedule();
	}
    

    private void throwCoreException(String message) throws CoreException {
		final IStatus status = new Status(IStatus.ERROR, UIPlugin.PLUGIN_ID, IStatus.OK, message, null);
		throw new CoreException(status);
	}

    @Override
    public boolean canFinish() {
    	IWizardPage currentPage = getContainer().getCurrentPage();
    	
        if (currentPage instanceof SelectFeaturesWizardPage) {
            return getPageIndex(currentPage) == getPageCount() - 1;
        }

        return false;
    }
}
