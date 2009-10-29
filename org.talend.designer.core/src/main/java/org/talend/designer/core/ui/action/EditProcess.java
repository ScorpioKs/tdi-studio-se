// ============================================================================
//
// Copyright (C) 2006-2009 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.action;

import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.MessageBoxExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.utils.VersionUtils;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.ProcessItem;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.model.repository.IRepositoryObject;
import org.talend.core.ui.images.ECoreImage;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.i18n.Messages;
import org.talend.designer.core.ui.AbstractMultiPageTalendEditor;
import org.talend.designer.core.ui.MultiPageTalendEditor;
import org.talend.designer.core.ui.editor.ProcessEditorInput;
import org.talend.designer.runprocess.ItemCacheManager;
import org.talend.repository.ProjectManager;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.model.ProxyRepositoryFactory;
import org.talend.repository.model.RepositoryNode;
import org.talend.repository.model.RepositoryNodeUtilities;
import org.talend.repository.ui.actions.AContextualAction;

/**
 * DOC smallet class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class EditProcess extends AContextualAction implements IIntroAction {

    private static final String EDIT_LABEL = Messages.getString("EditProcess.editJob"); //$NON-NLS-1$

    private static final String OPEN_LABEL = Messages.getString("EditProcess.openJob"); //$NON-NLS-1$

    private static final String DBPROJECT_LABEL = "teneo";

    private Properties params;

    public EditProcess() {
        super();

        this.setText(EDIT_LABEL);
        this.setToolTipText(EDIT_LABEL);
        this.setImageDescriptor(ImageProvider.getImageDesc(ECoreImage.PROCESS_ICON));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    protected void doRun() {
        ISelection selection = getSelectedObject();
        if (selection == null) {
            return;
        }
        Object obj = ((IStructuredSelection) selection).getFirstElement();
        if (obj == null) {
            return;
        }
        RepositoryNode node = (RepositoryNode) obj;
        Property property = (Property) node.getObject().getProperty();
        ProcessItem processItem = null;

        ItemCacheManager.clearCache();
        Assert.isTrue(property.getItem() instanceof ProcessItem);

        // ProjectManager projectManager = ProjectManager.getInstance();
        // processItem = ItemCacheManager.getProcessItem(new Project(projectManager.getProject(property.getItem())),
        // property
        // .getId(), property.getVersion());

        Property updatedProperty = null;
        try {

            updatedProperty = ProxyRepositoryFactory.getInstance().getUptodateProperty(
                    new Project(ProjectManager.getInstance().getProject(property.getItem())), property);

        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        }
        // update the property of the node repository object
        node.getObject().setProperty(updatedProperty);
        // added by nma, to avoid lazy exceptions, order 9556.
        if (ProjectManager.getInstance().getCurrentProject().getEmfProject().getUrl() != null
                && ProjectManager.getInstance().getCurrentProject().getEmfProject().getUrl().startsWith(DBPROJECT_LABEL)) {
            try {
                processItem = (ProcessItem) ProxyRepositoryFactory.getInstance().getUptodateProperty(updatedProperty).getItem();
            } catch (PersistenceException e1) {
                e1.printStackTrace();
            }
        } else
            processItem = (ProcessItem) updatedProperty.getItem();

        IWorkbenchPage page = getActivePage();

        try {
            final ProcessEditorInput fileEditorInput = new ProcessEditorInput(processItem, true);

            IEditorPart editorPart = page.findEditor(fileEditorInput);

            if (editorPart == null) {
                fileEditorInput.setView(getViewPart());
                fileEditorInput.setRepositoryNode(node);
                editorPart = page.openEditor(fileEditorInput, MultiPageTalendEditor.ID, true);
                /* MultiPageTalendEditor openEditor = (MultiPageTalendEditor) */
                // List<AbstractProcessProvider> findAllProcessProviders =
                // AbstractProcessProvider.findAllProcessProviders();
                // boolean isImport = false;
                // for (AbstractProcessProvider abstractProcessProvider : findAllProcessProviders) {
                // if (abstractProcessProvider != null) {
                // boolean update = abstractProcessProvider.updateProcessContexts((Process) fileEditorInput
                // .getLoadedProcess());
                // if (update) {
                // isImport = true;
                // }
                // }
                // }
                // if (isImport) {
                // openEditor.getTalendEditor().getCommandStack().execute(new Command() {
                // });
                // }
            } else {
                ((MultiPageTalendEditor) editorPart).setReadOnly(fileEditorInput.setForceReadOnly(false));
                page.activate(editorPart);
            }
            // see the bug 6585,qli comment.
            if (editorPart instanceof AbstractMultiPageTalendEditor) {
                ((AbstractMultiPageTalendEditor) editorPart).updateTitleImage();
            }
            refresh(obj);
        } catch (PartInitException e) {
            MessageBoxExceptionHandler.process(e);
        } catch (PersistenceException e) {
            MessageBoxExceptionHandler.process(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.actions.ITreeContextualAction#init(org.eclipse.jface.viewers.TreeViewer,
     * org.eclipse.jface.viewers.IStructuredSelection)
     */
    public void init(TreeViewer viewer, IStructuredSelection selection) {
        boolean canWork = !selection.isEmpty() && selection.size() == 1;
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        if (factory.isUserReadOnlyOnCurrentProject()) {
            canWork = false;
        }
        if (canWork) {
            Object o = selection.getFirstElement();
            RepositoryNode node = (RepositoryNode) o;
            switch (node.getType()) {
            case REPOSITORY_ELEMENT:
                if (node.getObjectType() != ERepositoryObjectType.PROCESS) {
                    canWork = false;
                } else {
                    IRepositoryService service = DesignerPlugin.getDefault().getRepositoryService();
                    IProxyRepositoryFactory repFactory = service.getProxyRepositoryFactory();
                    if (repFactory.isPotentiallyEditable(node.getObject())) {
                        this.setText(EDIT_LABEL);
                    } else {
                        this.setText(OPEN_LABEL);
                    }
                }
                break;
            default:
                canWork = false;
            }
            if (canWork && !ProjectManager.getInstance().isInCurrentMainProject(node)) {
                canWork = false;
            }

            // If the editProcess action canwork is true, then detect that the job version is the latest verison or not.
            if (canWork) {
                canWork = isLastJobVersion(node);
            }

        }
        setEnabled(canWork);
    }

    /**
     * 
     * DOC YeXiaowei EditProcess class global comment. Detailled comment
     */
    @SuppressWarnings("unchecked")
    private static class IRepositoryObjectComparator implements Comparator {

        public int compare(Object o1, Object o2) {
            return VersionUtils.compareTo(((IRepositoryObject) o1).getVersion(), ((IRepositoryObject) o2).getVersion());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isLastJobVersion(RepositoryNode repositoryObject) {
        try {
            List<IRepositoryObject> allVersion = ProxyRepositoryFactory.getInstance().getAllVersion(repositoryObject.getId());
            if (allVersion == null || allVersion.isEmpty()) {
                return false;
            }
            // Collections.sort(allVersion, new IRepositoryObjectComparator());
            IRepositoryObject lastVersion = allVersion.get(allVersion.size() - 1);
            return lastVersion.getVersion().equals(repositoryObject.getObject().getVersion());
        } catch (PersistenceException e) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.repository.ui.actions.AContextualView#getClassForDoubleClick()
     */
    @Override
    public Class getClassForDoubleClick() {
        return ProcessItem.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.intro.config.IIntroAction#run(org.eclipse.ui.intro.IIntroSite, java.util.Properties)
     */
    public void run(IIntroSite site, Properties params) {
        this.params = params;
        PlatformUI.getWorkbench().getIntroManager().closeIntro(PlatformUI.getWorkbench().getIntroManager().getIntro());
        doRun();
    }

    private ISelection getSelectedObject() {
        if (params == null) {
            return getSelection();
        } else {
            RepositoryNode repositoryNode = RepositoryNodeUtilities.getRepositoryNode(params.getProperty("nodeId"), false);
            if (repositoryNode != null) {
                RepositoryNodeUtilities.expandParentNode(getViewPart(), repositoryNode);
                return new StructuredSelection(repositoryNode);
            }
            return null;

        }
    }
}
