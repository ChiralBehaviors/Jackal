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

import java.util.Vector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


public class SnugTable extends JTable
{
    public SnugTable()
    {
        super();
    }

    public SnugTable(Vector v1, Vector v2) {
        super(v1, v2);
        resizeColumns();
    }

    public void setModel(TableModel dataModel)
    {
        super.setModel( dataModel );
        resizeColumns();
    }

    public void setSize (Dimension d)
    {
        super.setSize( d );
        resizeColumns();
    }

    private void resizeColumns()
    {
        try
        {
            byte num_cols = (byte)getColumnCount();
            Object[] longValues = new Object[num_cols];

            //Trace.log( this, "resizeColumns", "#cols = " + num_cols );

            for (int c = 0; c < num_cols; c++)
            {
                longValues[c] = "";
            }

            int num_rows = getRowCount();

            FontMetrics fm = null;
            Font font = getFont();

            if ( font == null )
            {
                fm = getFontMetrics( new Font( "Dialog", 0, 12 ) );
            }
            else
            {
                fm = getFontMetrics( font );
            }

            Object obj = null;
            String s = null;

            for (int r = 0; r < num_rows; r++)
            {
                for (byte c = 0; c < num_cols; c++)
                {
                    obj = getValueAt(r, c);

                    //Trace.log( this, "resizeColumns", "" + r + ", " + c );

                    if ( obj == null )
                    {
                        continue;
                    }

                    if ( obj instanceof JLabel )
                    {
                        s = ((JLabel)obj).getText();
                    }
                    else
                    {
                        s = obj.toString();
                    }

                    if ( s == null )
                    {
                        continue;
                    }

                    //Trace.log( this, "resizeColumns", "" + r + ", " + c + " = " + s );

                    if (fm.stringWidth(s) > fm.stringWidth(longValues[c].toString()))
                    {
                        //
                        // We should really store obj here but since we are not using
                        // the table cell renderer (see comment below), this is OK.
                        //

                        longValues[c] = s;
                    }
                }
            }

            //Trace.log( this, "resizeColumns", "Finding biggest" );

            TableColumnModel columnModel = getColumnModel();
            TableColumn column = null;
            TableCellRenderer headerRenderer, cellRenderer;
            Component comp;
            Insets ins;
            int headerWidth = 0, cellWidth = 0, maxWidth = 0;

            for (int i = 0; i < num_cols; i++)
            {
                column = columnModel.getColumn(i);
                headerRenderer = column.getHeaderRenderer();

                if (headerRenderer == null)
                {
                    headerRenderer = getTableHeader().getDefaultRenderer();
                }

                comp = headerRenderer.getTableCellRendererComponent(this, column.getHeaderValue(), false, false, -1, i);
                headerWidth = comp.getPreferredSize().width;
                cellRenderer = column.getCellRenderer();

                //Trace.log( this, "resizeColumns", "col: " + i + " -- header width = " + headerWidth );

                if (cellRenderer == null)
                {
                    cellRenderer = getDefaultRenderer(getColumnClass(i));
                }

                //
                // We should really use the getPreferredSize method to determine cell width
                // but it is returning the same value for all columns so we will base our
                // cell width on the size of the text in each cell (this could cause problems
                // with cells containing JLabels with icons).
                //
                // comp = cellRenderer.getTableCellRendererComponent(this, longValues, false, false, 0, i);
                // cellWidth = comp.getPreferredSize().width;
                //

                if ( longValues[i] == null )
                {
                    cellWidth = 0;
                }
                else
                {
                    cellWidth = fm.stringWidth(longValues[i].toString());
                }

                //Trace.log( this, "resizeColumns", "col: " + i + " -- cell width = " + cellWidth );

                if (cellWidth > headerWidth)
                {
                    maxWidth = cellWidth;
                    ins = ((JComponent)cellRenderer).getInsets();
                }
                else
                {
                    maxWidth = headerWidth;
                    ins = ((JComponent)headerRenderer).getInsets();
                }

                //Trace.log( this, "resizeColumns", "col: " + i + " -- ins = " + ins.left + ", " + ins.right );
                //Trace.log( this, "resizeColumns", "col: " + i + " -- pref. size = " + (ins.left + maxWidth + ins.right) );

                column.setPreferredWidth(ins.left + maxWidth + ins.right);
            }
        }
        catch ( Throwable t )
        {
            //Trace.log( this, "resizeColumns", t.getMessage() );
            //Trace.exception( this, "resizeColumns", t );
        }
    }
}

