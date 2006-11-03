// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.repository.ui.wizards.metadata.table.files;

import org.apache.log4j.Logger;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.ui.swt.dialogs.ErrorDialogWidthDetailArea;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.repository.i18n.Messages;
import org.talend.repository.model.IRepositoryFactory;
import org.talend.repository.model.RepositoryFactoryProvider;
import org.talend.repository.ui.wizards.RepositoryWizard;

/**
 * DOC ocarbone class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class FilePositionalTableWizard extends RepositoryWizard implements INewWizard {

    private static Logger log = Logger.getLogger(FilePositionalTableWizard.class);

    private FileTableWizardPage tableWizardpage;

    private ConnectionItem connectionItem;

    private MetadataTable metadataTable;

    /**
     * Constructor for TableWizard.
     * 
     * @param ISelection
     */
    @SuppressWarnings("unchecked")
    public FilePositionalTableWizard(IWorkbench workbench, boolean creation, ConnectionItem connectionItem,
            MetadataTable metadataTable) {
        super(workbench, creation);
        this.connectionItem = connectionItem;
        this.metadataTable = metadataTable;
        setNeedsProgressMonitor(true);

        isRepositoryObjectEditable();
        initLockStrategy();
    }

    /**
     * Adding the page to the wizard.
     */

    public void addPages() {
        setWindowTitle(Messages.getString("SchemaWizard.windowTitle"));

        tableWizardpage = new FileTableWizardPage(connectionItem, metadataTable, isRepositoryObjectEditable());
        addPage(tableWizardpage);

        if (creation) {
            tableWizardpage.setTitle(Messages.getString("FileTableWizardPage.titleCreate", connectionItem.getProperty()
                    .getLabel()));
            tableWizardpage.setDescription(Messages.getString("FileTableWizardPage.descriptionCreate"));
            tableWizardpage.setPageComplete(false);
        } else {
            tableWizardpage.setTitle(Messages.getString("FileTableWizardPage.titleUpdate", metadataTable.getLabel()));
            tableWizardpage.setDescription(Messages.getString("FileTableWizardPage.descriptionUpdate"));
            tableWizardpage.setPageComplete(isRepositoryObjectEditable());
        }
    }

    /**
     * This method determine if the 'Finish' button is enable This method is called when 'Finish' button is pressed in
     * the wizard. We will create an operation and run it using wizard as execution context.
     */
    public boolean performFinish() {
        if (!tableWizardpage.isPageComplete()) {
            return false;
        }

        closeLockStrategy();

        IRepositoryFactory factory = RepositoryFactoryProvider.getInstance();
        try {
            factory.save(repositoryObject.getProperty().getItem());
        } catch (PersistenceException e) {
            String detailError = e.toString();
            new ErrorDialogWidthDetailArea(getShell(), PID, Messages.getString("CommonWizard.persistenceException"), detailError);
            log.error(Messages.getString("CommonWizard.persistenceException") + "\n" + detailError);
        }
        return true;
    }

    /**
     * We will accept the selection in the workbench to see if we can initialize from it.
     * 
     * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
     */
    public void init(final IWorkbench workbench, final IStructuredSelection selection) {
        this.selection = selection;
    }

}
