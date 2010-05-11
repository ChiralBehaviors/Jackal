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
package org.smartfrog.services.anubis.locator.util;




import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

public class DebugFrame extends JFrame {
    private JPanel jPanel2 = new JPanel();
    private BorderLayout borderLayout1 = new BorderLayout();
    private BorderLayout borderLayout2 = new BorderLayout();
    private TitledBorder titledBorder1;

    // private Test test;
    private JScrollPane jScrollPane1 = new JScrollPane();
    private JTextArea jTextArea1 = new JTextArea();

    private boolean      onDisplay = false;
    private Object       displayObject = null;

    public DebugFrame(String title) {
        // test = t;
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
        this.getContentPane().setLayout(borderLayout1);
        jPanel2.setLayout(borderLayout2);
        jPanel2.setBorder(BorderFactory.createRaisedBevelBorder());
        jTextArea1.setBorder(BorderFactory.createLoweredBevelBorder());
        this.getContentPane().add(jPanel2, BorderLayout.CENTER);
        jPanel2.add(jScrollPane1, BorderLayout.CENTER);
        jScrollPane1.getViewport().add(jTextArea1, null);

        this.setSize(450, 200);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { remove(); }
        });
    }


    void println(String str) {
        jTextArea1.append(str);
        jTextArea1.append("\n");
    }

    void print(String str) {
        jTextArea1.append(str);
    }

    public void setText(String text) {
        jTextArea1.setText(text);
    }

    public void update() {
        if( onDisplay )
            setText(displayObject.toString());
    }

    public void makeVisible(Object obj) {
        onDisplay = true;
        displayObject = obj;
        setVisible(true);
        update();
    }
    public void remove() {
        onDisplay = false;
        displayObject = null;
        setVisible(false);
    }
}
