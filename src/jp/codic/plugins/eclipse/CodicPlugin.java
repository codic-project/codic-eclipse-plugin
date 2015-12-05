package jp.codic.plugins.eclipse;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * 
 * @author Kenji Namba
 */
public class CodicPlugin extends AbstractUIPlugin {

	/** The plug-in ID */
	public static final String PLUGIN_ID = "jp.codic.cep.core";

	/** The shared instance */
	private static CodicPlugin plugin;

	private static final String BUNDLE_NAME = CodicPlugin.class.getClass() + ".messages"; //$NON-NLS-1$

	public static String ProjectPropertyPage_1;
	public static String ProjectPropertyPage_DESCRIPTION;
	public static String ProjectPropertyPage_API_KEY;
	public static String ProjectPropertyPage_PROJECT;
	public static String QuicklookDialog_SET_ACCESS_TOKEN;
	public static String QuicklookDialog_API_FAILED;
	public static String QuicklookDialog_1;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, CodicPlugin.class);
	}

	/**
	 * The constructor
	 */
	public CodicPlugin() {

	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * @return the shared instance
	 */
	public static CodicPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public IProject getActiveProject() {
		IEditorPart editorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
				.getActivePage().getActiveEditor();
		if (editorPart != null) {
			IFileEditorInput input = (FileEditorInput) editorPart.getEditorInput();
			IFile file = input.getFile();
			return file.getProject();
			// activeProjectName = activeProject.getName();
		}
		return null;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry reg) {
		reg.put("CodicPlugin.IMG_FONT", getImageDescriptor("/icons/font.gif"));
	}

	/**
	 * Activate view.
	 */
	public static void activateView(String id) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window.getActivePage();
		//		TranslationView view = (TranslationView) page.findView(TranslationView.ID);
		try {
			page.showView(id);
		} catch (PartInitException e) {
		}

	}

	public static IEditorPart getActiveEditor(IWorkbenchWindow window) {
		return window.getActivePage().getActiveEditor();
	}
	
	public static IProject getActiveEditorProject() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IEditorInput input = editor.getEditorInput();
		if (!(input instanceof IFileEditorInput))
			return null;
		return ((IFileEditorInput) input).getFile().getProject();
	}

	public static void log(String message) {
		System.out.println(message);
	}

	public static String defaultString(String str) {
		return str == null ? "" : str;
	}
	
	public static boolean isEmpty(String str) {
		return str == null || str.equals("");
	}
}
