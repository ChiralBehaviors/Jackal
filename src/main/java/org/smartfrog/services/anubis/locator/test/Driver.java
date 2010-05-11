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
package org.smartfrog.services.anubis.locator.test;



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


public class Driver extends JFrame {
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

    private Test test;
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JTextArea jTextArea1 = new JTextArea();
    private JButton jButton5 = new JButton();
    private JPanel jPanel3 = new JPanel();
    private JButton jButton6 = new JButton();
    private JButton jButton7 = new JButton();

    public Driver(Test t, String title) {
        test = t;
        try {
            jbInit();
            this.setTitle(title);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }
    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder("");
        jButton2.setText("- Prov");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton2_actionPerformed(e);
            }
        });
        jButton3.setActionCommand("Rapido");
        jButton3.setText("Rapido");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton3_actionPerformed(e);
            }
        });
        jButton4.setText("- Lsnr");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton4_actionPerformed(e);
            }
        });
        jButton1.setText("+ Prov");
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
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton5_actionPerformed(e);
            }
        });
        jButton5.setText("Local");
        jButton5.setMinimumSize(new Dimension(60, 27));
        jButton5.setActionCommand("Local");
        jButton5.setMaximumSize(new Dimension(60, 27));
        jPanel3.setBorder(BorderFactory.createRaisedBevelBorder());
        jButton6.setMaximumSize(new Dimension(60, 27));
        jButton6.setMinimumSize(new Dimension(60, 27));
        jButton6.setActionCommand("Global");
        jButton6.setText("Global");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton6_actionPerformed(e);
            }
        });
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jButton7_actionPerformed(e);
            }
        });
        jButton7.setActionCommand("+ Listener");
        jButton7.setText("+ Lsnr");
        jPanel1.add(jButton1, null);
        jPanel1.add(jButton2, null);
        jPanel1.add(jButton3, null);
        jPanel1.add(jButton7, null);
        jPanel1.add(jButton4, null);
        this.getContentPane().add(jPanel3, BorderLayout.NORTH);
        jPanel3.add(jButton6, null);
        jPanel3.add(jButton5, null);
        this.getContentPane().add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(jTextField1,  BorderLayout.SOUTH);
        jPanel2.add(jScrollPane1, BorderLayout.CENTER);
        this.getContentPane().add(jPanel1, BorderLayout.SOUTH);
        jScrollPane1.getViewport().add(jTextArea1, null);

        this.setSize(new Dimension(499, 300));

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { test.terminate(); }
        });
    }

    void jButton1_actionPerformed(ActionEvent e) {
        test.addProvider(jTextField1.getText());
        jTextField1.setText("");
    }

    void jButton2_actionPerformed(ActionEvent e) {
        test.removeProvider(jTextField1.getText());
        jTextField1.setText("");
    }

    void jButton3_actionPerformed(ActionEvent e) {
        test.rapidStates(jTextField1.getText());
        jTextField1.setText("");
    }

    void jButton4_actionPerformed(ActionEvent e) {
        test.removeListener(jTextField1.getText());
        jTextField1.setText("");
    }

    void jButton7_actionPerformed(ActionEvent e) {
        test.nonBlockAddListener(jTextField1.getText());
        jTextField1.setText("");
    }

    void jButton5_actionPerformed(ActionEvent e) {
        test.showLocal();
    }

    void jButton6_actionPerformed(ActionEvent e) {
        test.showGlobal();
    }

    void println(String str) {
        jTextArea1.append(str);
        jTextArea1.append("\n");
    }

    void print(String str) {
        jTextArea1.append(str);
    }


}
