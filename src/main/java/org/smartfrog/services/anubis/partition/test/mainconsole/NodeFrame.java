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


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import org.smartfrog.services.anubis.partition.test.msg.StatsMsg;
import org.smartfrog.services.anubis.partition.test.msg.ThreadsMsg;
import org.smartfrog.services.anubis.partition.views.View;


public class NodeFrame extends JFrame {
    private NodeData nodeData;


    private JPanel jPanel1 = new JPanel();
    private JButton jButton2 = new JButton();
    private JButton jButton3 = new JButton();
    private JButton jButton4 = new JButton();
    private JButton jButton1 = new JButton();
    private JPanel jPanel2 = new JPanel();
    private JTextField jTextField1 = new JTextField();
    private BorderLayout borderLayout1 = new BorderLayout();
    private BorderLayout borderLayout2 = new BorderLayout();
    private TitledBorder titledBorder1;

    private JScrollPane jScrollPane1 = new JScrollPane();
    private JTextArea jTextArea1 = new JTextArea();
    private JButton jButton7 = new JButton();
    private JButton jButton8 = new JButton();

    public NodeFrame(NodeData nodeData) {
        this.nodeData = nodeData;
        try {
            jbInit();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder("");
        jButton2.setText("Noop");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton2_actionPerformed(e);
            }
        });
        jButton3.setText("Noop");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton3_actionPerformed(e);
            }
        });
        jButton4.setText("Threads");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton4_actionPerformed(e);
            }
        });
        jButton1.setText("Ignore");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton1_actionPerformed(e);
            }
        });
        this.getContentPane().setLayout(borderLayout1);
        jPanel2.setLayout(borderLayout2);
        jPanel1.setBorder(BorderFactory.createRaisedBevelBorder());
        jPanel2.setBorder(BorderFactory.createRaisedBevelBorder());
        jTextArea1.setBorder(BorderFactory.createLoweredBevelBorder());
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton7_actionPerformed(e);
            }
        });
        jButton7.setActionCommand("+ Listener");
        jButton7.setText("Noop");
        jButton8.setMaximumSize(new Dimension(60, 27));
        jButton8.setActionCommand("Local");
        jButton8.setMinimumSize(new Dimension(60, 27));
        jButton8.setText("Stats");
        jButton8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton8_actionPerformed(e);
            }
        });
        jPanel1.add(jButton8, null);
        jPanel1.add(jButton1, null);
        jPanel1.add(jButton2, null);
        jPanel1.add(jButton7, null);
        jPanel1.add(jButton3, null);
        jPanel1.add(jButton4, null);
        this.getContentPane().add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(jTextField1,  BorderLayout.SOUTH);
        jPanel2.add(jScrollPane1, BorderLayout.CENTER);
        this.getContentPane().add(jPanel1, BorderLayout.SOUTH);
        jScrollPane1.getViewport().add(jTextArea1, null);

        this.setSize(new Dimension(499, 300));

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { closeEvent(); }
        });
    }

    void jButton1_actionPerformed(ActionEvent e) {
        String text = stripErrorPart(jTextField1.getText());
        jTextField1.setText(text);
        nodeData.setIgnoring(text);
    }

    void jButton2_actionPerformed(ActionEvent e) {
    }

    void jButton3_actionPerformed(ActionEvent e) {
    }

    void jButton4_actionPerformed(ActionEvent e) {
        nodeData.getThreads();
    }

    void jButton7_actionPerformed(ActionEvent e) {
    }



    void println(String str) {
        jTextArea1.append(str);
        jTextArea1.append("\n");
    }

    void print(String str) {
        jTextArea1.append(str);
    }

    void clear() {
        jTextArea1.setText("");
    }

    void jButton8_actionPerformed(ActionEvent e) {
        nodeData.getStats();
    }







    /**
     * create a close window action listener
     */
    public void closeEvent() {
        nodeData.closeWindow();
    }

    public String viewMembersToString(View view) {
        String str = "";
        for(int i = 0; i < view.size(); i++)
            if( view.contains(i) )
                str += i + " ";
        return str;
    }

    public void update(View partition, View view, int leader, View ignoring,
                       long interval, long timeout, StatsMsg stats, ThreadsMsg threads) {
        clear();
        if( partition.isStable() )
            println("PARTITION -- stable -- timestamp: " + partition.getTimeStamp() + ", leader: " + leader);
        else
            println("PARTITION -- UNstable -- timestamp: " + partition.getTimeStamp() + ", leader: " + leader);
        println("    " + viewMembersToString(partition));
        println("");

        if( view.isStable() )
            println("VIEW -- stable -- timestamp: " + view.getTimeStamp());
        else
            println("VIEW -- UNstable -- timestamp: " + view.getTimeStamp());
        println("    " + viewMembersToString(view));
        println("");

        if( ignoring.isEmpty() )
            println("accepting all nodes");
        else
            println("IGNORING: \n" + viewMembersToString(ignoring));
        println("");
        println("heartbeat interval = " + interval + ", timeout = " + timeout);
        if( stats != null)
            println("Scheduling delay min: " + stats.schedulingOneMinute +
                    ", 10min: " + stats.schedulingTenMinute +
                    ", hour: " + stats.schedulingOneHour +
                    ", longest delay: " + stats.schedulingLongest);
        println("");
        if( threads != null ) {
            println(threads.threadsStatusString);
        }
        println("");

    }

    public String stripErrorPart(String inputStr) {
        String input = inputStr.trim();
        if( input.indexOf('[') == 0 && input.indexOf(']') != -1 )
            input = input.substring(input.indexOf(']') + 1);
        return input;
    }

    public void inputError(String errorStr) {
        jTextField1.setText("[" + errorStr + "]" + jTextField1.getText());
    }


}
