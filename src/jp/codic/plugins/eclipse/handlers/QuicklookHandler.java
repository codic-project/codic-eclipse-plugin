package jp.codic.plugins.eclipse.handlers;

import jp.codic.plugins.eclipse.CodicPlugin;
import jp.codic.plugins.eclipse.dialogs.QuicklookDialog;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class QuicklookHandler extends AbstractHandler {
	/**
	 * The constructor.
	 */
	public QuicklookHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {

//		HandlerUtil.getActiveWorkbenchWindowChecked(event).getWorkbench().getA
		IWorkbenchWindow window = HandlerUtil
				.getActiveWorkbenchWindowChecked(event);
//		if (!hasActiveEditor(window))
//			return null;
		
		IEditorPart editor = CodicPlugin.getActiveEditor(window);
		if (editor == null) 
			return null;
		
		String selection = getEditorSelection(editor);
		if (selection == null) {
			return null;
		}
		
		
		Shell shell = editor.getSite().getShell();
		
		int shellStyle = PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE;
		boolean takeFocusOnOpen = true;
		boolean persistSize = true;
		boolean persistLocation = true;
//		boolean showDialogMenu = true;
		boolean showPersistActions = true;
		String titleText = null;
		String infoText = "infoText";
		QuicklookDialog dialog = new QuicklookDialog(shell,
				shellStyle, takeFocusOnOpen, persistSize, persistLocation,
				showPersistActions, titleText, infoText);
		// MessageDialog.openInformation(
		// window.getShell(),
		// "NamingPlugin",
		// "Hello, Eclipse world");
		dialog.open(selection);
		return null;
	}

	private String getEditorSelection(IEditorPart editor) {
		ISelection iSelection = null;
		IEditorSite iEditorSite = editor
				.getEditorSite();
		if (iEditorSite != null) {
			ISelectionProvider iSelectionProvider = iEditorSite
					.getSelectionProvider();
			if (iSelectionProvider != null) {
				iSelection = iSelectionProvider.getSelection();
				String text = ((ITextSelection) iSelection).getText();
				return (text == null ? "" : text);
			}
		}
		return null;
	}
}
