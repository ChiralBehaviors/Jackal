/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

*/
package org.smartfrog.services.anubis.partition.test.mainconsole;



import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import org.smartfrog.services.anubis.partition.util.Identity;

public class AsymetryReportFrame extends JFrame {

    private JPanel       jPanel1       = new JPanel();
    private JScrollPane  jScrollPane   = new JScrollPane(jPanel1);
    private BorderLayout borderLayout1 = new BorderLayout();
    private TitledBorder titledBorder1;
    private JTable       asymetryTable = null;
    private Controller   controller;
    private int          magic;

    public AsymetryReportFrame(Controller controller, Map nodes, Identity id) throws HeadlessException {
        this.controller = controller;
        this.magic      = id.magic;
        asymetryTable   = getTable(nodes);
        try {
            jbInit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    private void jbInit() throws Exception {

        setTitle("Asymetry Report (where Y sees X but not seen by X)");
        setSize(new Dimension(250, 200));
        jScrollPane.setBorder(BorderFactory.createRaisedBevelBorder());
        getContentPane().add(jScrollPane, BorderLayout.CENTER);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                controller.removeAsymetryReport();
            }
        });
//        asymetryTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        jPanel1.add(asymetryTable);
        setVisible(true);
    }

    public void recalculate(Map nodes) {
        jPanel1.remove(asymetryTable);
        asymetryTable = getTable(nodes);
        jPanel1.add(asymetryTable);
        jPanel1.updateUI();
    }


    private JTable getTable(Map nodes) {

        int    highestId   = getHighestNodeId(nodes);
        Vector columnNames = new Vector(highestId+2);
        Vector rowData     = new Vector(highestId+2);

        columnNames.add("\\");
        for(int i=0; i<highestId; i++) {
            columnNames.add(new Integer(i).toString());
        }

        rowData.add(columnNames);

        for(int i=0; i<highestId; i++) {
            rowData.add(getRow(highestId, i, nodes));
        }

        Iterator iter = nodes.values().iterator();
        NodeData nodeData;
        while( iter.hasNext() ) {
            nodeData = (NodeData)iter.next();
            // addData( rowData, nodeData, nodes );
        }

        return new SnugTable(rowData, columnNames);
    }

    private Vector getRow(int highestId, int id, Map nodes) {

        NodeData nodeData = (NodeData)nodes.get(identity(id));
        Vector   vector   = new Vector(highestId+2);
        boolean  itseesme;
        boolean  iseeit;
        NodeData it;

        vector.add(String.valueOf(id));

        if( nodeData == null ) {

            for(int i=0; i<highestId+1; i++) vector.add("");
            return vector;

        } else {

            for(int i=0; i<highestId; i++) {
                iseeit   = nodeData.getView().contains(i);
                it       = (NodeData)nodes.get(identity(i));
                itseesme = (it == null) ? false : it.getView().contains(id);
                if( iseeit && !itseesme ) {
                    vector.add("X");
                } else {
                    vector.add("");
                }
            }
            return vector;
        }
    }

    private int getHighestNodeId(Map nodes) {
        int      highestSoFar  = -1;
        int      highest;
        Iterator iter          = nodes.values().iterator();

        while( iter.hasNext() ) {
            highest = highestIdInData((NodeData)iter.next());
            if( highest > highestSoFar )
                highestSoFar = highest;
        }

        return highestSoFar;
    }

    private int highestIdInData(NodeData nodeData) {
        int id = nodeData.getIdentity().id;
        int sz = nodeData.getView().size();
        return (id > sz) ? id : sz;
    }

    /**
     * Creates an identity that is equal to the node with the given id
     * in this system (same magic).
     * @param id
     * @return
     */
    private Identity identity(int id) {
        return new Identity(magic, id, 0);
    }
}
