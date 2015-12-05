package jp.codic.plugins.eclipse.dialogs;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import jp.codic.plugins.eclipse.CodicPlugin;
import jp.codic.plugins.eclipse.propertyPages.ProjectPropertyPage;
import jp.codic.plugins.eclipse.utils.APIException;
import jp.codic.plugins.eclipse.utils.APIUtils;
import jp.codic.plugins.eclipse.utils.CEDEntry;
import jp.codic.plugins.eclipse.utils.Translation;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommand;
import org.eclipse.ui.commands.ICommandManager;
import org.eclipse.ui.commands.IKeySequenceBinding;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.keys.KeySequence;
import org.eclipse.ui.keys.SWTKeySupport;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class QuicklookDialog extends PopupDialog implements ModifyListener {

	private static final int LOOKUP_DELAY = 400;
	private static final String COMMAND_ID = CodicPlugin.PLUGIN_ID+  ".commands.quicklook"; //$NON-NLS-1$
	private static final String PROPERTY_PAGE_ID = "jp.codic.cep.property.page"; //$NON-NLS-1$
	private static ConcurrentHashMap<String, Casing> casingDefaults = new ConcurrentHashMap<String, Casing>();

	private Text filterText;
	private org.eclipse.swt.widgets.Table table;
	private TableViewer tableViewer;
	private Entry[] translations = null;
	private String apiErrorMessage = null;
	private int apiErrorCode;

	private Composite viewMenuButtonComposite;
	private Job translateJob;
	private String accessToken;
	private String project;
	private boolean accessTokenIsValid;
	private IAction showViewMenuAction;
	private Combo casingCombo ;
	private KeySequence[] fInvokingCommandKeySequences;
	private ICommand invokingCommand;
	private ImageRegistry imageRegistry;
	private Link errorLink;
	private Composite dialogAreaComposite;

	public QuicklookDialog(Shell parent, int shellStyle, boolean takeFocusOnOpen,
			boolean persistSize, boolean persistLocation, boolean showPersistActions,
			String titleText, String infoText) {
		super(parent, shellStyle, takeFocusOnOpen, persistSize, persistLocation, true,
				showPersistActions, titleText, infoText);

		ICommandManager commandManager = PlatformUI.getWorkbench().getCommandSupport()
				.getCommandManager();
		invokingCommand = commandManager.getCommand(COMMAND_ID);
		if (invokingCommand != null && !invokingCommand.isDefined()) {
			invokingCommand = null;
		} else
			// Pre-fetch key sequence - do not change because scope will
			// change later.
			getInvokingCommandKeySequences();

		setInfoText(getStatusFieldText());
		imageRegistry = CodicPlugin.getDefault().getImageRegistry();
	}

	/**
	 * Open dialog with initial filter text.
	 * @param value Text to search.
	 * @return
	 */
	public int open(String value) {
		int ret = this.open();
		filterText.setText(value);
		filterText.forceFocus();
		filterText.selectAll();
		return ret;
	}


	@Override
	public int open() {
		int ret = super.open();

		// Get access key that sets was active project property. 
		getProjectSetting();

		if (!accessTokenIsValid) {
			errorLink.setText(CodicPlugin.QuicklookDialog_SET_ACCESS_TOKEN);
			errorLink.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					dispose();
					PreferencesUtil.createPropertyDialogOn(getShell(),
							CodicPlugin.getActiveEditorProject(), PROPERTY_PAGE_ID, null, null)
							.open();
				}
			});
			setActiveStack(errorLink);
		}
		return ret;
	}

	private void setActiveStack(Control control) {
		((StackLayout) dialogAreaComposite.getLayout()).topControl = control;
		dialogAreaComposite.layout();
	}

	/**
	 * Get acccess key setting in project preference.
	 */
	private void getProjectSetting() {
		try {
			accessToken = CodicPlugin.getActiveEditorProject().getPersistentProperty(
					new QualifiedName(CodicPlugin.PLUGIN_ID, ProjectPropertyPage.APIKEY));
			project = CodicPlugin.getActiveEditorProject().getPersistentProperty(
					new QualifiedName(CodicPlugin.PLUGIN_ID, ProjectPropertyPage.PROJECT));
		} catch (CoreException e) {
		}
		accessTokenIsValid = (accessToken != null && !accessToken.equals(""));
	}

	/**
	 * Pack column width.
	 */
	private void packTableColumns() {
		Rectangle area = getShell().getClientArea();
		if (tableViewer != null) {
			int margin = 20;
			TableColumn column0 = tableViewer.getTable().getColumn(0);
			TableColumn column1 = tableViewer.getTable().getColumn(1);
			column0.pack();
			int width0 = column0.getWidth();
			column0.setWidth(width0 + margin);
			column1.setWidth(area.width - width0 - margin);
		}
	}

	private String getStatusFieldText() {
		KeySequence[] sequences = getInvokingCommandKeySequences();
		if (sequences == null || sequences.length == 0)
			return ""; //$NON-NLS-1$

		String keySequence = sequences[0].format();
		return MessageFormat.format(CodicPlugin.QuicklookDialog_1, keySequence);
	}

	protected KeySequence[] getInvokingCommandKeySequences() {
		if (fInvokingCommandKeySequences == null) {
			if (getInvokingCommand() != null) {
				List<IKeySequenceBinding> list = getInvokingCommand().getKeySequenceBindings();
				if (!list.isEmpty()) {
					fInvokingCommandKeySequences = new KeySequence[list.size()];
					for (int i = 0; i < fInvokingCommandKeySequences.length; i++) {
						fInvokingCommandKeySequences[i] = ((IKeySequenceBinding) list.get(i))
								.getKeySequence();
					}
					return fInvokingCommandKeySequences;
				}
			}
		}
		return fInvokingCommandKeySequences;
	}

	protected ICommand getInvokingCommand() {
		return invokingCommand;
	}

	@Override
	protected Control createTitleControl(Composite parent) {
		filterText = createFilterText(parent);

		showViewMenuAction = new Action("showViewMenu") { //$NON-NLS-1$
			public void run() {
				showDialogMenu();
			}
		};
		showViewMenuAction.setEnabled(true);
		showViewMenuAction.setActionDefinitionId("org.eclipse.ui.window.showViewMenu"); //$NON-NLS-1$

		return filterText;
	}

	protected Text createFilterText(Composite parent) {
		filterText = new Text(parent, SWT.NONE);
		Dialog.applyDialogFont(filterText);

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.CENTER;
		filterText.setLayoutData(data);
		
		filterText.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN && tableViewer.getInput() != null) {
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
		filterText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.keyCode == 0x0D) // return
					gotoSelectedElement();
			}
		});
		filterText.addKeyListener(getKeyAdapter());
		filterText.addModifyListener(this);
		return filterText;
	}

	@Override
	public void modifyText(ModifyEvent arg0) {
		doTranslation();
	}

	public void doTranslation() {
		if (!accessTokenIsValid)
			return;

		if (translateJob != null) {
			translateJob.cancel();
		}
		translateJob = new QuicklookJob(filterText.getText(), CASE_OPTIONS[casingCombo.getSelectionIndex()]);
		translateJob.schedule(LOOKUP_DELAY);
	}

	// class TranslateJob extends Job {
	//
	// private String query;
	// private String accessKey;
	//
	// public TranslateJob(String accessKey, String query) {
	// super("API invoking job");
	// this.accessKey = accessKey;
	// this.query = query;
	// }
	//
	// @Override
	// protected IStatus run(IProgressMonitor arg0) {
	// APIHelper apiHelper = new APIHelper();
	// try {
	// DictionaryEntry[] dictionaryEntries= apiHelper.lookup(accessKey, query);
	// } catch (APIException e) {
	//
	// }
	//
	// return Status.OK_STATUS;
	// }
	// }

	// private String getAccessKey() {
	// try {
	// return NamingPlugin
	// .getDefault()
	// .getActiveEditorProject()
	// .getPersistentProperty(
	// new QualifiedName(NamingPlugin.PLUGIN_ID,
	// ProjectPropertyPage.APIKEY));
	// } catch (CoreException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	// return null;
	// }

	private String getActiveEditorContentType() {
		IWorkbenchWindow window = CodicPlugin.getDefault().getWorkbench()
				.getActiveWorkbenchWindow();
		window.getActivePage().getActiveEditor();

		IEditorPart part = window.getActivePage().getActiveEditor();
		if (!(part instanceof AbstractTextEditor))
			return "unknown";

		//		Platform.getContentTypeManager().findContentTypeFor(fileName)
		AbstractTextEditor editor = (AbstractTextEditor) part;

		//IEditorInput input = editor.getEditorInput();
		IDocumentProvider dp = editor.getDocumentProvider();
		IEditorInput eiEditorInput = editor.getEditorInput();
		if (eiEditorInput instanceof FileEditorInput) {
			return ((FileEditorInput) eiEditorInput).getFile().getFileExtension();
		} else {
			return "unknown";
		}
	}

	private void gotoSelectedElement() {
		Object selectedElement = getSelectedElement();
		if (selectedElement != null) {
			dispose();
			//AStyleRange range = new StyleRange();
			//range.underlineStyle
			Entry t = (Entry) selectedElement;
			IWorkbenchWindow window = CodicPlugin.getDefault().getWorkbench()
					.getActiveWorkbenchWindow();
			window.getActivePage().getActiveEditor();

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
					String newText = t.title;
					try {
						doc.replace(textSel.getOffset(), textSel.getLength(), newText);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					// Move cursor
					editor.getSelectionProvider().setSelection(
							new TextSelection(textSel.getOffset() + newText.length(), 0));
				}
			}
		}
	}

	// /**
	// * Returns <code>true</code> if the control has a header,
	// <code>false</code>
	// * otherwise.
	// * <p>
	// * The default is to return <code>false</code>.
	// * </p>
	// *
	// * @return <code>true</code> if the control has a header
	// */
	// protected boolean hasHeader() {
	// // default is to have no header
	// return false;
	// }

	/**
	 * Implementers can modify
	 * @return the selected element
	 */
	protected Object getSelectedElement() {
		if (tableViewer == null)
			return null;
		return ((IStructuredSelection) tableViewer.getSelection()).getFirstElement();
		//		return resultText.getText();
	}

//	//@Override
//	protected Control createDialogArea_(final Composite parent_) {
//		Composite parent = (Composite) super.createDialogArea(parent_);
//		dialogAreaComposite = parent;
//		dialogAreaComposite.setLayout(new StackLayout());
//
//		errorLink = new Link(parent, SWT.WRAP);
//		errorLink.setText("");
//		FontData fontData = errorLink.getFont().getFontData()[0];
//		org.eclipse.swt.graphics.Font font = new Font(Display.getDefault(), new FontData(
//				fontData.getName(), fontData.getHeight(), SWT.ITALIC));
//		errorLink.setFont(font);
//
//		resultText = new StyledText(parent, SWT.WRAP | SWT.READ_ONLY);
//		fontData = resultText.getFont().getFontData()[0];
//		font = new Font(Display.getDefault(), new FontData(fontData.getName(),
//				fontData.getHeight() * 2, SWT.NORMAL));
//		resultText.setFont(font);
//		resultText.setMargins(20, 20, 20, 20);
//		resultText.setLayoutData(new GridData(GridData.FILL_BOTH));
//
//		//		parent_.setLayout(new StackLayout());
//		return parent_;
//	}

	@Override
	protected Control createDialogArea(final Composite parent_) {
		Composite parent = (Composite) super.createDialogArea(parent_);
		dialogAreaComposite = parent;
		dialogAreaComposite.setLayout(new StackLayout());

		errorLink = new Link(parent, SWT.WRAP);
		errorLink.setText("");
		FontData fontData = errorLink.getFont().getFontData()[0];
		org.eclipse.swt.graphics.Font font = new Font(Display.getDefault(), new FontData(
				fontData.getName(), fontData.getHeight(), SWT.ITALIC));
		errorLink.setFont(font);

		table = new org.eclipse.swt.widgets.Table(parent, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = table.getItemHeight() * 12;

		table.setLayoutData(gd);
		table.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.character == 0x1B) // ESC
					dispose();
			}

			public void keyReleased(KeyEvent e) {
				// do nothing
			}
		});
		table.addKeyListener(getKeyAdapter());
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

		((StackLayout) dialogAreaComposite.getLayout()).topControl = table;
		tableViewer = new TableViewer(table);
		tableViewer.setContentProvider(new IStructuredContentProvider() {
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			public Object[] getElements(Object inputElement) {
				return (Entry[]) inputElement;
			}

			public void dispose() {
			}
		});

		tableViewer.setLabelProvider(new TableViewerLabelProvider());
		// listViewer.setContentProvider(new ArrayContentProvider());
		// listViewer.setInput(listContents);

		// Button submit = new Button(table, SWT.PUSH);
		return tableViewer.getControl();
	}

	private KeyAdapter fKeyAdapter;

	private KeyAdapter getKeyAdapter() {
		if (fKeyAdapter == null) {
			fKeyAdapter = new KeyAdapter() {
				public void keyPressed(KeyEvent e) {
					if (e.character == 0x1B) { // ESC
						dispose();
						return;
					}

					int accelerator = SWTKeySupport.convertEventToUnmodifiedAccelerator(e);
					KeySequence keySequence = KeySequence.getInstance(SWTKeySupport
							.convertAcceleratorToKeyStroke(accelerator));
					KeySequence[] sequences = getInvokingCommandKeySequences();
					if (sequences == null)
						return;
					for (int i = 0; i < sequences.length; i++) {
						if (sequences[i].equals(keySequence)) {
							e.doit = false;
//							scrollCasing();
							//CodicPlugin.activateView(TranslationView.ID);
							return;
						}
					}
				}
			};
		}
		return fKeyAdapter;
	}

	private void dispose() {
		close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Control createTitleMenuArea(Composite parent) {
		viewMenuButtonComposite = (Composite) super.createTitleMenuArea(parent);

		// Insert additional menu item.
		insertCustomToolbarMenu();

		return viewMenuButtonComposite;
	}

	private ToolBar findToolbar() {
		Control[] controls = viewMenuButtonComposite.getChildren();
		if (controls.length >= 2 && controls[1] instanceof ToolBar) {
			return (ToolBar) controls[1];
		}
		return null;
	}

	private Casing[] CASE_OPTIONS = { 
			new Casing("pascal", "PascalCase", "Aa"),
			new Casing("camel", "camelCase", "aA"),
			new Casing("lower underscore", "snake_case (小文字)", "a_a"),
			new Casing("upper underscore", "SNAKE_CASE (大文字)", "A_A"),
			new Casing("hyphen", "ハイフネーション", "a-a"),
			new Casing("", "変換なし", "a a"), 
		};

	private void insertCustomToolbarMenu() {
		// Add toolbar item.
		Control[] controls = viewMenuButtonComposite.getChildren();
		//new Combo(viewMenuButtonComposite, SWT.None);

		if (controls.length >= 2 && controls[1] instanceof ToolBar) {
			ToolBar toolbar = (ToolBar) controls[1];
			toolbar.getItem(0);

			// Remove current mouse listener.
			Listener[] listeners = toolbar.getListeners(SWT.MouseDown);
			if (listeners != null) {
				for (Listener listener : listeners) {
					toolbar.removeListener(SWT.MouseDown, listener);
				}
			}

//			if (this.casing == null) {
//				this.casing = CASE_OPTIONS[0];
//			}
			
			String fileType = getActiveEditorContentType();
			Casing casing = casingDefaults.get(fileType);
			if (casing == null) {
				casing = CASE_OPTIONS[0];
			}
			
			ToolItem sep = new ToolItem(toolbar, SWT.SEPARATOR, 0);
			casingCombo = new Combo(toolbar, SWT.READ_ONLY);
			//casingCombo.setDa
			int defaultSelected = 0;
			for (int i = 0; i < CASE_OPTIONS.length; i++) {
				if (casing.id.equals(CASE_OPTIONS[i].id)) {
					defaultSelected = i;
				}
				casingCombo.add(CASE_OPTIONS[i].label);
			}
			casingCombo.select(defaultSelected);
			casingCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					Casing casing = CASE_OPTIONS[casingCombo.getSelectionIndex()];
					String fileType = getActiveEditorContentType();
					casingDefaults.put(fileType, casing);
					setCurrentLetterCase(casing);
				}
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
				}
			});
			casingCombo.pack();
			//sep.setWidth(combo.getSize().x);
			sep.setWidth(50);
			sep.setControl(casingCombo);
			toolbar.pack();
		}
	}

	/**
	 * The dialog's menu manager.
	 */
	private MenuManager letterCaseMenuManager = null;

	/**
	 * Show the dialog's menu. This message has no effect if the receiver was
	 * not configured to show a menu. Clients may call this method in order to
	 * trigger the menu via keystrokes or other gestures. Subclasses typically
	 * do not override method.
	 */
	protected void showCasingMenu() {

		if (letterCaseMenuManager == null) {
			letterCaseMenuManager = new MenuManager();
			fillCasingMenu(letterCaseMenuManager);
		}
		// Setting this flag works around a problem that remains on X only,
		// whereby activating the menu deactivates our shell.
		//		listenToDeactivate = !Util.isGtk();

		ToolBar toolBar = findToolbar();
		if (toolBar != null) {
			Menu menu = letterCaseMenuManager.createContextMenu(getShell());
			Rectangle bounds = toolBar.getBounds();
			Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
			topLeft = getShell().toDisplay(topLeft);
			menu.setLocation(topLeft.x, topLeft.y);
			menu.setVisible(true);
		}
	}

	protected void fillCasingMenu(IMenuManager dialogMenu) {
		for (int i = 0; i < CASE_OPTIONS.length; i++) {
//			dialogMenu.add(new CasingAction(CASE_OPTIONS[i]));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void setTabOrder(Composite composite) {
		// if (hasHeader()) {
		// composite.setTabList(new Control[] { filterText,
		// tableViewer.getTable() });
		// } else {
		viewMenuButtonComposite.setTabList(new Control[] { filterText });
		//		composite.setTabList(new Control[] { viewMenuButtonComposite,
		//				tableViewer.getTable() });
		// }
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected IDialogSettings getDialogSettings() {
		String sectionName = "jp.codic.eclipse.cep.Quicklookup"; //$NON-NLS-1$
		IDialogSettings settings = JavaPlugin.getDefault().getDialogSettings()
				.getSection(sectionName);
		if (settings == null)
			settings = JavaPlugin.getDefault().getDialogSettings().addNewSection(sectionName);

		return settings;
	}

	/**
	 * Display update thread.
	 */
	private class ListViewUpdateThread implements Runnable {
		public void run() {
			if (apiErrorMessage != null) {
				setActiveStack(errorLink);
				if (apiErrorCode == 429) {
					errorLink.setText("レートリミットを超えました。");
				} else {
					errorLink.setText("API接続に失敗しました : " + apiErrorMessage);
				}
			} else {
				setActiveStack(table);
				tableViewer.setInput(translations);
				packTableColumns();
				if (translations == null || translations.length > 0) {
					tableViewer.getTable().setSelection(0);
				}
			}
		}
	}

	private class QuicklookJob extends Job {
		private String query;
		private Casing casing;

		public QuicklookJob(String query, Casing casing) {
			super("Quicklook job");
			this.query = normalizeQuery(query);
			this.casing = casing;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			//CodicPlugin.log("Jon#run");
			translations = null;
			apiErrorMessage = null;
			if (query != null && !query.isEmpty()) {
				try {
					if (isLetters(query)) {
						translations = mapCedEntries(APIUtils.lookup(accessToken, query));
					} else {
						translations = mapTranslatons(APIUtils.translate(accessToken, project,
								query, casing != null ? casing.id : null));
					}

				} catch (APIException e) {
					apiErrorMessage = e.getMessage();
					apiErrorCode = e.getCode();
				}
			}
			Control control = getContents();
			if (control != null) {
				control.getDisplay().asyncExec(new ListViewUpdateThread());
			}
			return Status.OK_STATUS;
		}

		private Entry[] mapTranslatons(Translation[] translations) {
			Translation translation = translations[0];
			if (translation.words.length == 1 && translation.words[0].successful) {
				Entry[] entries = new Entry[translation.words[0].candidates.length];
				for (int i = 0; i < translation.words[0].candidates.length; i++) {
					entries[i] = new Entry();
					entries[i].title = translation.words[0].candidates[i].text;
					entries[i].description = null;
				}
				return entries;
			} else {
				Entry[] entries = new Entry[1];
				entries[0] = new Entry();
				entries[0].title = translation.translatedText;
				entries[0].description = null;
				return entries;
			}

		}

		private Entry[] mapCedEntries(CEDEntry[] lookup) {
			Entry[] entries = new Entry[lookup.length];
			for (int i = 0; i < lookup.length; i++) {
				entries[i] = new Entry();
				entries[i].title = lookup[i].title;
				entries[i].description = lookup[i].digest;
			}
			return entries;
		}

		private String normalizeQuery(String input) {
			if (input == null || input.isEmpty())
				return null;
			return input.replace('-', ' ').replace('_', ' ').trim();
		}

		private boolean isLetters(String input) {
			if (input == null || input.isEmpty())
				return true;
			return input.matches("[A-Za-z0-9\\s\\-]+");
		}
	}

	private class TableViewerLabelProvider extends LabelProvider implements ITableLabelProvider {
		@Override
		public String getColumnText(Object element, int columnIndex) {
			Entry translation = (Entry) element;
			if (columnIndex == 0) {
				return translation.title;
			} else if (columnIndex == 1) {
				return translation.description == null ? "" : translation.description;
			} else {
				return null;
			}
		}

		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
	}

	private void setCurrentLetterCase(Casing casing) {
		doTranslation();
	}

	public class Entry {
		public String title;
		public String description;
	}

	public class Casing {
		private String id;
		private String label;
		private String shortLabel;

		public Casing(String id, String label, String shortLabel) {
			super();
			this.id = id;
			this.label = label;
			this.shortLabel = shortLabel;
		}
	}
}
