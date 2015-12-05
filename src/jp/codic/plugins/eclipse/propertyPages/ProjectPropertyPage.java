package jp.codic.plugins.eclipse.propertyPages;

import jp.codic.plugins.eclipse.CodicPlugin;
import jp.codic.plugins.eclipse.utils.APIUtils;
import jp.codic.plugins.eclipse.utils.UserProject;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;


public class ProjectPropertyPage extends PropertyPage {
	public static final String APIKEY = "APIKEY"; //$NON-NLS-1$
	public static final String PROJECT = "PROJECT"; //$NON-NLS-1$

	private Text accessKeyText;
	private Combo dictCombo;
	private UserProject[] projects = new UserProject[0];
//	private boolean modified = false;

	public ProjectPropertyPage() {
		setDescription(CodicPlugin.ProjectPropertyPage_DESCRIPTION);
	}

	/**
	 * Returns project.
	 * @return Active project
	 */
	private IProject getProject() {
		IAdaptable element = getElement();
		  if (element instanceof IProject) {
		    return (IProject)element;
		  }
		  if (element instanceof IJavaProject) {
		    return ((IJavaProject) element).getProject();
		  }
		  return null;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		IProject project = getProject();

//		org.eclipse.dltk.internal.core.ScriptProject

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);
		
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(CodicPlugin.ProjectPropertyPage_API_KEY);

		accessKeyText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		accessKeyText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		String value = getValue(project, APIKEY);
		if (value != null) {
			accessKeyText.setText(value);
		}

//		accessKeyText.addModifyListener(new ModifyListener() {
//			public void modifyText(ModifyEvent arg0) {
//				modified = true;
//			}
//		});
		
		accessKeyText.addListener(SWT.FocusOut, new Listener() {
			public void handleEvent(Event ev) {
				try {
					updateDictionariesCombo(null);	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		Label label_1 = new Label(composite, SWT.NONE);
		label_1.setText(CodicPlugin.ProjectPropertyPage_PROJECT);
		GridData data2 = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 200;
		dictCombo = new Combo(composite, SWT.READ_ONLY);
		dictCombo.setLayoutData(data2);
		updateDictionariesCombo(getValue(project, PROJECT));
		
		super.noDefaultAndApplyButton();
		return composite;
	}

	private void updateDictionariesCombo(String selected) {
		setErrorMessage(null);
		String accessKey = accessKeyText.getText();
		if (accessKey != null && accessKey.length() > 0) {
			try {
				this.projects = APIUtils.getUserProjects(accessKey);
				String names[] = new String[projects.length];
				int selectedIndex = -1;
				for (int i = 0; i < projects.length; i++) {
					if (projects[i].id.toString().equals(selected)) 
						selectedIndex = i;
					names[i] = projects[i].name;
				}
				// Select first option.
				if (selectedIndex == -1 && projects.length > 0)
					selectedIndex = 0; 
				dictCombo.setItems(names);
				dictCombo.setEnabled(true);
				dictCombo.select(selectedIndex);
			} catch (Exception e) {
				this.projects = new UserProject[0];
				setErrorMessage(e.getMessage());
				dictCombo.setItems(new String[0]);
				dictCombo.clearSelection();
				dictCombo.setEnabled(false);
//				setValid(true);
			}
		} else {
			dictCombo.setItems(new String[0]);
			dictCombo.clearSelection();
			dictCombo.setEnabled(false);
//			setValid(false);
		}
	}
	
	@Override
	public boolean performOk() {
//		if (!modified)
//			return true;
		
		if (!doValidation()) {
			return false;
		}

		IProject project = getProject();
		setValue(project, APIKEY, accessKeyText.getText());
		setValue(project, PROJECT, projects[dictCombo.getSelectionIndex()].id.toString());
		return true;
	}

	private boolean doValidation() {
		setErrorMessage(null);
		if (CodicPlugin.isEmpty(this.accessKeyText.getText())) {
			setErrorMessage(CodicPlugin.ProjectPropertyPage_1);
			return false;
		}
		if (dictCombo.getItems().length == 0) {
			setErrorMessage("プロジェクトを選択してください。");
			return false;
		}
		if (dictCombo.getSelectionIndex() < 0) {
			setErrorMessage("プロジェクトを選択してください。");
			return false;
		}
		return true;
	}
	
	private String getValue(IProject project, String key) {
		try {
			return project.getPersistentProperty(new QualifiedName(
					CodicPlugin.PLUGIN_ID, key));
		} catch (CoreException e) {
			ILog log = CodicPlugin.getDefault().getLog();
			log.log(e.getStatus());
			return null;
		}
	}

	private void setValue(IProject project, String key, String value) {
		try {
			project.setPersistentProperty(new QualifiedName(
					CodicPlugin.PLUGIN_ID, key), value);
		} catch (CoreException e) {
			ILog log = CodicPlugin.getDefault().getLog();
			log.log(e.getStatus());
		}
	}
}
