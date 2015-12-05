package jp.codic.plugins.eclipse.views;

import jp.codic.plugins.eclipse.CodicPlugin;
import jp.codic.plugins.eclipse.propertyPages.ProjectPropertyPage;
import jp.codic.plugins.eclipse.utils.APIException;
import jp.codic.plugins.eclipse.utils.APIUtils;
import jp.codic.plugins.eclipse.utils.CEDEntry;
import jp.codic.plugins.eclipse.utils.Translation;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.*;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.*;
import org.eclipse.swt.SWT;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */

public class TranslationView extends ViewPart implements ModifyListener {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "jp.codic.eclipse.namingplugin.views.Translation";

	private Action action1;
	private Action action2;
	// private Action doubleClickAction;
	private Text filterText;
	private TableViewer tableViewer;
	private CEDEntry[] translations = null;
	private String apiError = null;
	private String accessKey;
	private String dictionary;
	private IPartListener activationListener;
	private Job translateJob;

	/**
	 * The constructor.
	 */
	public TranslationView() {

	}

	/*
	 * The content provider class is responsible for providing objects to the
	 * view. It can wrap existing objects in adapters or simply return objects
	 * as-is. These objects may be sensitive to the current input of the view,
	 * or ignore it and always show the same content (like Task List, for
	 * example).
	 */

	class ViewContentProvider implements IStructuredContentProvider {
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		public void dispose() {
		}

		public Object[] getElements(Object parent) {
			return new String[] { "One", "Two", "Three" };
		}
	}

	class ViewLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
			return PlatformUI.getWorkbench().getSharedImages()
					.getImage(ISharedImages.IMG_OBJ_ELEMENT);
		}
	}

	// class NameSorter extends ViewerSorter {
	// }

	private void setActivationListener() {
		if (activationListener != null)
			return;

		activationListener = new IPartListener() {
			@Override
			public void partActivated(IWorkbenchPart part) {
				getAccessKey();
			}

			@Override
			public void partBroughtToTop(IWorkbenchPart part) {
			}

			@Override
			public void partClosed(IWorkbenchPart part) {
			}

			@Override
			public void partDeactivated(IWorkbenchPart part) {
			}

			@Override
			public void partOpened(IWorkbenchPart part) {
			}

		};
		getViewSite().getPage().addPartListener(activationListener);
	}

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent) {
		// viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL |
		// SWT.V_SCROLL);
		// viewer.setContentProvider(new ViewContentProvider());
		// viewer.setLabelProvider(new ViewLabelProvider());
		// viewer.setSorter(new NameSorter());
		// viewer.setInput(getViewSite());

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		parent.setLayout(gridLayout);
		// initializeToolBar(parent);
		createFilterText(parent);
		createTableControl(parent);

		// Create the help context id for the viewer's control
		// PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(),
		// "xxxxx.viewer");
		makeActions();
		hookContextMenu();
		// hookDoubleClickAction();
		contributeToActionBars();

		setActivationListener();
	}

	private void initializeToolBar(Composite parent) {
		// IToolBarManager tbm =
		// getViewSite().getActionBars().getToolBarManager();
		// tbm.add(new ControlContribution("xxx") {
		// @Override
		// protected Control createControl(Composite parent) {
		// filterText = new Text(parent, SWT.NONE | SWT.BORDER | SWT.SINGLE);
		// return filterText;
		// }
		//
		// });
		// tbm.add(new CheckBoxA)

		// ToolBar toolbar = new ToolBar(parent, SWT.NONE);
		// filterText = new Text(toolbar, SWT.NONE | SWT.BORDER | SWT.SINGLE);
		// Dialog.applyDialogFont(filterText);
		// ToolItem refresh = new ToolItem(toolbar, SWT.SEPARATOR);
		// refresh.setWidth(filterText.getSize().x);
		// refresh.setControl(filterText);
		// toolbar.pack();

		// IToolBarManager tbm=
		// getViewSite().getActionBars().getToolBarManager();
		// tbm.add(item);
		// createToolBarGroups(tbm);
		// tbm.appendToGroup(IContextMenuConstants.GROUP_SEARCH,
		// fSearchAgainAction);
		// tbm.appendToGroup(IContextMenuConstants.GROUP_SEARCH, fCancelAction);
		// tbm.appendToGroup(IContextMenuConstants.GROUP_SEARCH,
		// fSearchesDropDownAction);
		// tbm.appendToGroup(IContextMenuConstants.GROUP_SEARCH,
		// fPinSearchViewAction);
		// getViewSite().getActionBars().updateActionBars();
	}

	private Text createFilterText(Composite parent) {
		filterText = new Text(parent, SWT.BORDER | SWT.SINGLE);
		// Dialog.applyDialogFont(filterText);

		// SiteManager.getHttpProxyPort()

		GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING
				| GridData.FILL_HORIZONTAL);
		filterText.setLayoutData(gd);

		filterText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == 0x0D) // return
					gotoSelectedElement();
				if (e.keyCode == SWT.ARROW_DOWN
						&& tableViewer.getInput() != null) {
					tableViewer.getTable().setFocus();
				}
				if (e.keyCode == SWT.ARROW_UP && tableViewer.getInput() != null)
					tableViewer.getTable().setFocus();
				if (e.character == 0x1B) // ESC
					dispose();
			}

			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});
		// filterText.addKeyListener(getKeyAdapter());
		filterText.addModifyListener(this);
		return filterText;
	}

	public void modifyText(ModifyEvent arg0) {
		if (translateJob != null) {
			translateJob.cancel();
		}
		translateJob = new QuicklookJob(filterText.getText());
		translateJob.schedule();
	}

	private Control createTableControl(Composite parent) {
		// Composite parent = (Composite)super.createDialogArea(parent_);
		// StackLayout dialogAreaStack = new StackLayout();
		// parent.setLayout(dialogAreaStack);

		// errorLabel = new Label(parent, SWT.NONE);
		// errorLabel.setText("");

		// int shellStyle = SWT.RESIZE;
		// int treeStyle = SWT.V_SCROLL | SWT.H_SCROLL;
		Table table = new Table(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = table.getItemHeight() * 12;
		table.setLayoutData(gd);
		// table.addKeyListener(getKeyAdapter());
		table.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				// do nothing
			}

			public void widgetDefaultSelected(SelectionEvent e) {
				gotoSelectedElement();
			}
		});

		table.setLinesVisible(false);
		table.setHeaderVisible(false);
		TableColumn col1 = new TableColumn(table, SWT.NONE);
		col1.setResizable(false);
		TableColumn col2 = new TableColumn(table, SWT.NONE);
		col2.setResizable(false);

		// dialogAreaStack.topControl = table;
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			public void inputChanged(Viewer viewer, Object oldInput,
					Object newInput) {
			}

			public Object[] getElements(Object inputElement) {
				return (Translation[]) inputElement;
			}

			public void dispose() {
			}
		});

		tableViewer.setLabelProvider(new TableViewerLabelProvider());
		return tableViewer.getControl();
	}

	private void hookContextMenu() {
		// MenuManager menuMgr = new MenuManager("#PopupMenu");
		// menuMgr.setRemoveAllWhenShown(true);
		// menuMgr.addMenuListener(new IMenuListener() {
		// public void menuAboutToShow(IMenuManager manager) {
		// TranslationView.this.fillContextMenu(manager);
		// }
		// });
		// Menu menu = menuMgr.createContextMenu(viewer.getControl());
		// viewer.getControl().setMenu(menu);
		// getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		// fillLocalPullDown(bars.getMenuManager());
		// fillLocalToolBar(bars.getToolBarManager());
	}

	// private void fillLocalPullDown(IMenuManager manager) {
	// manager.add(action1);
	// manager.add(new Separator());
	// manager.add(action2);
	// }
	//
	// private void fillContextMenu(IMenuManager manager) {
	// manager.add(action1);
	// manager.add(action2);
	// // Other plug-ins can contribute there actions here
	// manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	// }

	// private void fillLocalToolBar(IToolBarManager manager) {
	// manager.add(action1);
	// manager.add(action2);
	// }

	private void makeActions() {
		action1 = new Action() {
			public void run() {
				showMessage("Action 1 executed");
			}
		};
		action1.setText("Action 1");
		action1.setToolTipText("Action 1 tooltip");
		action1.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));

		action2 = new Action() {
			public void run() {
				showMessage("Action 2 executed");
			}
		};
		action2.setText("Action 2");
		action2.setToolTipText("Action 2 tooltip");
		action2.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		// doubleClickAction = new Action() {
		// public void run() {
		// // ISelection selection = viewer.getSelection();
		// // Object obj =
		// // ((IStructuredSelection)selection).getFirstElement();
		// // showMessage("Double-click detected on "+obj.toString());
		// }
		// };
	}

	/**
	 * Get acccess key setting in project preference.
	 */
	private void getAccessKey() {
		try {
			accessKey = getEditorProject().getPersistentProperty(
					new QualifiedName(CodicPlugin.PLUGIN_ID,
							ProjectPropertyPage.APIKEY));
			dictionary = getEditorProject().getPersistentProperty(
					new QualifiedName(CodicPlugin.PLUGIN_ID,
							ProjectPropertyPage.PROJECT));
		} catch (CoreException e) {
		}
	}

	private IProject getEditorProject() {
		IEditorPart editor = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IEditorInput input = editor.getEditorInput();
		if (!(input instanceof IFileEditorInput))
			return null;
		return ((IFileEditorInput) input).getFile().getProject();
	}

	// private void hookDoubleClickAction() {
	// // viewer.addDoubleClickListener(new IDoubleClickListener() {
	// // public void doubleClick(DoubleClickEvent event) {
	// // doubleClickAction.run();
	// // }
	// // });
	// }

	private void showMessage(String message) {
		MessageDialog.openInformation(tableViewer.getControl().getShell(),
				"Sample View", message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		// viewer.getControl().setFocus();
		filterText.setFocus();
	}

	/**
	 * Pack column width.
	 */
	private void packTableColumns() {
		// Rectangle area = getShell().getClientArea();
		if (tableViewer != null) {
			int holeWidth = tableViewer.getControl().getSize().x;
			int margin = 20;
			TableColumn column0 = tableViewer.getTable().getColumn(0);
			TableColumn column1 = tableViewer.getTable().getColumn(1);
			column0.pack();
			int width0 = column0.getWidth();
			column0.setWidth(width0 + margin);
			column1.setWidth(holeWidth - width0 - margin);
		}
	}

	private class TableViewerLabelProvider extends LabelProvider implements
			ITableLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			Translation translation = (Translation) element;
			if (columnIndex == 0) {
				return translation.translatedText;
			} else if (columnIndex == 1) {
				return translation.description == null ? ""
						: translation.description;
			} else {
				return null;
			}
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	private class QuicklookJob extends Job {
		private String query;

		public QuicklookJob(String query) {
			super("Quicklook job");
			this.query = normalizeQuery(query);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			translations = null;
			apiError = null;
			if (query != null && !query.isEmpty()) {
				try {
					translations = APIUtils.lookup(accessKey, query);
				} catch (APIException e) {
					apiError = e.getMessage();
				}
			}
			Display.getDefault().asyncExec(new ListViewUpdateThread());
			return Status.OK_STATUS;
		}

		private String normalizeQuery(String input) {
			if (input == null || input.isEmpty())
				return null;
			return input.replace('-', ' ').replace('_', ' ').trim();
		}

		private boolean isLetters(String input) {
			if (input == null || input.isEmpty())
				return true;
			return input.matches("[A-Za-z0-9\\s]+");
		}
	}

	private void gotoSelectedElement() {
		Object selectedElement = getSelectedElement();
		if (selectedElement != null) {

			Translation t = (Translation) selectedElement;
			IWorkbenchWindow window = CodicPlugin.getDefault().getWorkbench()
					.getActiveWorkbenchWindow();
			// window.getActivePage().getActiveEditor();

			IEditorPart part = window.getActivePage().getActiveEditor();
			if (!(part instanceof AbstractTextEditor))
				return;
			AbstractTextEditor editor = (AbstractTextEditor) part;
			// editor.getContentDescription()
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());
			ISelection sel = editor.getSelectionProvider().getSelection();
			if (sel != null) {
				if (sel instanceof TextSelection) {
					final TextSelection textSel = (TextSelection) sel;
					String newText = t.translatedText;
					try {
						doc.replace(textSel.getOffset(), textSel.getLength(),
								newText);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					// Move cursor
					editor.getSelectionProvider().setSelection(
							new TextSelection(textSel.getOffset()
									+ newText.length(), 0));
				}
				window.getActivePage().activate(part);
			}
		}
	}

	/**
	 * Implementers can modify
	 * 
	 * @return the selected element
	 */
	private Object getSelectedElement() {
		if (tableViewer == null || !tableViewer.getTable().isFocusControl())
			return null;
		return ((IStructuredSelection) tableViewer.getSelection())
				.getFirstElement();
	}

	/**
	 * Display update thread.
	 */
	private class ListViewUpdateThread implements Runnable {
		public void run() {
			if (apiError != null) {
				// dialogAreaStack.topControl = errorLabel;
				// errorLabel.setText(apiError);
			} else {
				// dialogAreaStack.topControl = table;
				tableViewer.setInput(translations);
				packTableColumns();
				if (translations == null || translations.length > 0) {
					tableViewer.getTable().setSelection(0);
				}
			}
		}
	}
}