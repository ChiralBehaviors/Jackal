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
package org.smartfrog.services.anubis.partition.test.controller.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MainConsoleFrame extends JFrame {
    abstract class SliderListener implements ChangeListener {
        abstract public void newValue(int value);

        @Override
        public void stateChanged(ChangeEvent e) {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                newValue(source.getValue());
            }
        }
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private BorderLayout      borderLayout1    = new BorderLayout();

    private BorderLayout      borderLayout2    = new BorderLayout();
    private BorderLayout      borderLayout3    = new BorderLayout();
    private BorderLayout      borderLayout4    = new BorderLayout();
    private GraphicController controller;
    private FlowLayout        flowLayout1      = new FlowLayout();
    private JButton           jButton1         = new JButton();
    private JButton           jButton2         = new JButton();
    private JButton           jButton3         = new JButton();
    private JButton           jButton4         = new JButton();
    private JPanel            jPanel1          = new JPanel();
    private JPanel            jPanel2          = new JPanel();
    private JPanel            jPanel3          = new JPanel();
    private JPanel            jPanel4          = new JPanel();
    private JPanel            jPanel5          = new JPanel();
    private JSlider           jSlider1         = new JSlider();
    private JSlider           jSlider2         = new JSlider();
    private JTextField        jTextField2      = new JTextField();
    @SuppressWarnings("unused")
    private TitledBorder      titledBorder1;

    public MainConsoleFrame(GraphicController controller) {
        try {
            this.controller = controller;
            setVisible(false);
            jbInit();
            jSlider1.addChangeListener(new SliderListener() {
                @Override
                public void newValue(int value) {
                    setInterval(value);
                }
            });
            jSlider2.addChangeListener(new SliderListener() {
                @Override
                public void newValue(int value) {
                    setTimeout(value);
                }
            });
            this.setSize(640, 480);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addNode(GraphicNodeData nodeData) {
        jPanel2.add(nodeData.getButton());
        jPanel2.updateUI();
    }

    public void closeEvent() {
        controller.terminate();
    }

    public long getInterval() {
        return jSlider1.getValue();
    }

    public long getTimeout() {
        return jSlider2.getValue();
    }

    public void initialiseTiming(long interval, long timeout) {
        jSlider1.setValue((int) interval);
        jSlider2.setValue((int) timeout);
    }

    public void inputError(String errorStr) {
        jTextField2.setText("[" + errorStr + "]" + jTextField2.getText());
    }

    public void removeNode(GraphicNodeData nodeData) {
        jPanel2.remove(nodeData.getButton());
        jPanel2.updateUI();
    }

    public String stripErrorPart(String inputStr) {
        String input = inputStr.trim();
        if (input.indexOf('[') == 0 && input.indexOf(']') != -1) {
            input = input.substring(input.indexOf(']') + 1);
        }
        return input;
    }

    private void jbInit() throws Exception {
        titledBorder1 = new TitledBorder("");
        jPanel1.setBorder(BorderFactory.createLoweredBevelBorder());
        jPanel1.setMaximumSize(new Dimension(533, 139));
        jPanel1.setToolTipText("");
        jPanel1.setLayout(borderLayout4);
        jPanel2.setLayout(flowLayout1);
        getContentPane().setLayout(borderLayout1);
        jPanel2.setBorder(BorderFactory.createLoweredBevelBorder());
        jPanel3.setLayout(borderLayout2);
        jSlider1.setMajorTickSpacing(5000);
        jSlider1.setMaximum(10000);
        jSlider1.setMinorTickSpacing(1000);
        jSlider1.setPaintLabels(true);
        jSlider1.setPaintTicks(true);
        jSlider2.setPaintLabels(true);
        jSlider2.setPaintTicks(true);
        jSlider2.setMaximum(10);
        jSlider2.setMinimum(2);
        jSlider2.setMajorTickSpacing(1);
        jTextField2.setText("");
        jPanel4.setLayout(borderLayout3);
        jButton1.setText("Clear Partitions");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jButton1_actionPerformed(e);
            }
        });
        jButton2.setText("Sim Partition");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jButton2_actionPerformed(e);
            }
        });
        jButton3.setText("Asim Partition");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jButton3_actionPerformed(e);
            }
        });
        jButton4.setText("Asymetry Analysis");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jButton4_actionPerformed(e);
            }
        });
        getContentPane().add(jPanel2, BorderLayout.CENTER);
        getContentPane().add(jPanel1, BorderLayout.SOUTH);
        jPanel1.add(jPanel3, BorderLayout.NORTH);
        jPanel3.add(jSlider2, BorderLayout.EAST);
        jPanel3.add(jSlider1, BorderLayout.WEST);
        jPanel1.add(jPanel4, BorderLayout.CENTER);
        jPanel4.add(jTextField2, BorderLayout.CENTER);
        jPanel4.add(jPanel5, BorderLayout.SOUTH);
        jPanel5.add(jButton3, null);
        jPanel5.add(jButton2, null);
        jPanel5.add(jButton1, null);
        jPanel5.add(jButton4, null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeEvent();
            }
        });
    }

    private void setInterval(int interval) {
        controller.setTiming(interval, getTimeout());
    }

    private void setTimeout(int timeout) {
        controller.setTiming(getInterval(), timeout);
    }

    void jButton1_actionPerformed(ActionEvent e) {
        controller.clearPartitions();
    }

    void jButton2_actionPerformed(ActionEvent e) {
        String input = stripErrorPart(jTextField2.getText());
        jTextField2.setText(input);
        controller.symPartition(input);
    }

    void jButton3_actionPerformed(ActionEvent e) {
        String input = stripErrorPart(jTextField2.getText());
        jTextField2.setText(input);
        controller.asymPartition(input);
    }

    void jButton4_actionPerformed(ActionEvent e) {
        controller.showAsymetryReport();
    }
}
