/*
 * RecordExplorerPanel.java
 *
 * Created on �����, 2005, �������� 7, 18:47
 */
package org.hypergraphdb.viewer.dialogs;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import org.hypergraphdb.HGHandle;
import org.hypergraphdb.HGPersistentHandle;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.type.HGCompositeType;
import org.hypergraphdb.type.HGProjection;
import org.hypergraphdb.type.Record;
import org.hypergraphdb.viewer.HGVKit;
import org.hypergraphdb.viewer.hg.HGVUtils;
import org.hypergraphdb.viewer.props.AbstractProperty;
import org.hypergraphdb.viewer.props.AbstractPropertySupport;
import org.hypergraphdb.viewer.props.PropertiesTableModel;
import org.hypergraphdb.viewer.props.PropertyCellEditor;

/**
 * 
 * @author User
 */
public class RecordExplorerPanel extends javax.swing.JPanel
{
	HGCompositeType[] recordTypes;
	String[] recordTypeNames;
	HGHandle[] recordTypeHandles;
	HyperGraph hg;

	/** Creates new form RecordExplorerPanel */
	public RecordExplorerPanel()
	{
		initComponents();
		initComponents2();
	}

	private void initComponents2()
	{
		hg = HGVKit.getCurrentView().getHyperGraph();
		recordTypeHandles = HGVUtils.getAllRecordTypes(hg);
		recordTypes = new HGCompositeType[recordTypeHandles.length];
		recordTypeNames = new String[recordTypeHandles.length];
		System.out.println("RecTypes: " + recordTypeHandles.length);
		for (int i = 0; i < recordTypeHandles.length; i++)
		{
			// recordTypes[i] = HGUtils.getRecordType(hg, recordTypeHandles[i]);
			recordTypes[i] = (HGCompositeType) hg.get(recordTypeHandles[i]);
			recordTypeNames[i] = HGVUtils.deduceAliasName(hg,
					recordTypeHandles[i]);
		}
		this.recordTypeCombo
				.setModel(new DefaultComboBoxModel(recordTypeNames));
		if (recordTypes.length > 0)
		{
			this.recordTypeCombo.setSelectedIndex(0);
			recordTypeComboActionPerformed(null);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	private void initComponents()// GEN-BEGIN:initComponents
	{
		java.awt.GridBagConstraints gridBagConstraints;
		jLabel1 = new javax.swing.JLabel();
		recordTypeCombo = new javax.swing.JComboBox();
		jScrollPane1 = new javax.swing.JScrollPane();
		recordsTable = new javax.swing.JTable();
		setLayout(new java.awt.GridBagLayout());
		setAlignmentX(1.0F);
		setAlignmentY(1.0F);
		setMinimumSize(new java.awt.Dimension(365, 200));
		setPreferredSize(new java.awt.Dimension(365, 300));
		jLabel1.setText("RecordType: ");
		jLabel1.setAlignmentX(1.0F);
		jLabel1.setMaximumSize(new java.awt.Dimension(100, 20));
		jLabel1.setMinimumSize(new java.awt.Dimension(80, 15));
		jLabel1.setPreferredSize(new java.awt.Dimension(80, 15));
		jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
		gridBagConstraints.insets = new java.awt.Insets(5, 3, 10, 0);
		add(jLabel1, gridBagConstraints);
		recordTypeCombo.setMinimumSize(new java.awt.Dimension(100, 21));
		recordTypeCombo.setPreferredSize(new java.awt.Dimension(100, 21));
		recordTypeCombo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				recordTypeComboActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 0, 10, 5);
		add(recordTypeCombo, gridBagConstraints);
		jScrollPane1.setMinimumSize(new java.awt.Dimension(200, 90));
		recordsTable.setModel(new javax.swing.table.DefaultTableModel(
				new Object[][] { { null, null, null, null },
						{ null, null, null, null }, { null, null, null, null },
						{ null, null, null, null } }, new String[] { "Title 1",
						"Title 2", "Title 3", "Title 4" }));
		jScrollPane1.setViewportView(recordsTable);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.gridwidth = 2;
		gridBagConstraints.gridheight = java.awt.GridBagConstraints.RELATIVE;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		add(jScrollPane1, gridBagConstraints);
	}// GEN-END:initComponents

	private void recordTypeComboActionPerformed(java.awt.event.ActionEvent evt)// GEN-FIRST:event_recordTypeComboActionPerformed
	{// GEN-HEADEREND:event_recordTypeComboActionPerformed
		if (selectedRecordTypeIndex == recordTypeCombo.getSelectedIndex())
			return;
		selectedRecordTypeIndex = recordTypeCombo.getSelectedIndex();
		ExplorerTableModel model = new ExplorerTableModel();
		model.create(recordTypeHandles[selectedRecordTypeIndex]);
		recordsTable.setModel(model);
		PropertyCellEditor pce = new PropertyCellEditor();
		for (int i = 0; i < recordsTable.getColumnCount(); i++)
			recordsTable.getColumnModel().getColumn(i)
					.setCellEditor(pce);
	}// GEN-LAST:event_recordTypeComboActionPerformed
	private int selectedRecordTypeIndex = -1;
	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JComboBox recordTypeCombo;
	private javax.swing.JTable recordsTable;

	// End of variables declaration//GEN-END:variables
	class ExplorerTableModel extends PropertiesTableModel
	{
		Record[] records;
		String[] col_labels;
		// RecordType rtype;
		HGCompositeType rtype;
		HGHandle[] recHandles;

		
		public AbstractProperty[][] getData()
		{
			HGHandle recTypeHandle = (HGHandle) bean;
			rtype = HGVUtils.getRecordType(hg, recTypeHandle);
			List<String> labels = new LinkedList<String>();
			for (Iterator<String> lab = rtype.getDimensionNames(); lab.hasNext();)
				labels.add(lab.next());
			col_labels = (String[]) labels.toArray(new String[labels.size()]);// new
																				// String[rtype.slotCount()];
			recHandles = HGVUtils.getAllForType(hg, recTypeHandle);
			AbstractProperty[][] data0 = new AbstractProperty[col_labels.length][recHandles.length];
			for (int j = 0; j < col_labels.length; j++)
			  for (int i = 0; i < recHandles.length; i++)
			  {
				Object obj = hg.get(recHandles[i]);
				if (!(obj instanceof Record))
				{
					HGPersistentHandle handle = hg.getStore().getLink(
							hg.getPersistentHandle(recHandles[i]))[1]; // tova ti dava "value" za record type
					obj = rtype.make(handle, null, null);
				}
				data0[j][i] = makeProperty(rtype.getProjection(col_labels[j]),
						    (Record) obj);
			}
			return data0;
		}

		public String getColumnName(int col)
		{
			return col_labels[col];
		}

		private AbstractProperty makeProperty(final HGProjection proj, final Record rec)
		{
			return new AbstractPropertySupport() {
				
				public Object getValue()
				{
					return proj.project(rec);
				}

//				public void setValue(Object value)
//				{
//					proj.inject(rec, value);
//				}
			};
		}
	}
}
