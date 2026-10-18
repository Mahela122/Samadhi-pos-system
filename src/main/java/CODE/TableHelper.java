/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 * @author tharu
 */
public class TableHelper {
    // Called after a cell is edited. Return true if it was saved, false to undo the change.
    public interface CellSaver {
        boolean save(int modelRow, int column, Object newValue);
    }
 
    // ---------- 1. Double-click to edit, Enter to save ----------
    // Call this AFTER the table's final model is set (after any setModel(...)).
    public static void onCellEdited(JTable table, CellSaver saver, Runnable reloadTable) {
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE); // clicking away also finishes the edit
 
        table.getModel().addTableModelListener(e -> {
            // Only react when ONE cell was changed by the user (not when the table is loaded or cleared)
            if (e.getType() != TableModelEvent.UPDATE
                    || e.getColumn() == TableModelEvent.ALL_COLUMNS
                    || e.getFirstRow() != e.getLastRow()) {
                return;
            }
            int row = e.getFirstRow();
            int column = e.getColumn();
            Object newValue = table.getModel().getValueAt(row, column);
 
            // Run after the table has finished closing the edit box
            SwingUtilities.invokeLater(() -> {
                if (!saver.save(row, column, newValue)) {
                    reloadTable.run(); // not saved -> show the real value from the database again
                }
            });
        });
    }
 
    // ---------- 2. Red cross (x) delete column ----------
    // Clicking the cross calls onDelete with the MODEL row number.
    // In the table model, this column must NOT be editable (isCellEditable returns false for it).
    public static void installDeleteColumn(JTable table, int column, IntConsumer onDelete) {
        TableColumn col = table.getColumnModel().getColumn(column);
        col.setPreferredWidth(60);
        col.setMaxWidth(80);
        col.setCellRenderer(new DefaultTableCellRenderer() {
            private final Font crossFont = new Font("Segoe UI", Font.BOLD, 22);
            private final Color red = new Color(220, 38, 38);
 
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int c) {
                super.getTableCellRendererComponent(t, "\u00D7", isSelected, false, row, c); // the cross mark
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(crossFont);
                setForeground(red);
                setToolTipText("Delete");
                return this;
            }
        });
 
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0 && isOnCross(e) && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1) {
                    if (table.isEditing()) {
                        table.getCellEditor().cancelCellEditing();
                    }
                    onDelete.accept(table.convertRowIndexToModel(viewRow));
                }
            }
 
            @Override
            public void mouseMoved(MouseEvent e) {
                // hand cursor when the mouse is over the cross
                table.setCursor(isOnCross(e) ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                                             : Cursor.getDefaultCursor());
            }
 
            private boolean isOnCross(MouseEvent e) {
                int viewCol = table.columnAtPoint(e.getPoint());
                return viewCol >= 0 && table.convertColumnIndexToModel(viewCol) == column;
            }
        };
        table.addMouseListener(mouse);
        table.addMouseMotionListener(mouse);
    }
 
    // ---------- 3. Drop-down editor (e.g. Active / Inactive) ----------
    public static void useDropDown(JTable table, int column, String... choices) {
        DefaultCellEditor editor = new DefaultCellEditor(new JComboBox<>(choices));
        editor.setClickCountToStart(2); // double-click, same as the text cells
        table.getColumnModel().getColumn(column).setCellEditor(editor);
    }
 
    // ---------- 4. Right-click option (for things the table can't edit, e.g. password) ----------
    public static void addRightClickOption(JTable table, String label, IntConsumer action) {
        JPopupMenu menu = table.getComponentPopupMenu();
        if (menu == null) {
            menu = new JPopupMenu();
            table.setComponentPopupMenu(menu);
 
            // Select the row under the mouse when right-clicking
            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (SwingUtilities.isRightMouseButton(e) && row >= 0) {
                        table.setRowSelectionInterval(row, row);
                    }
                }
            });
        }
 
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(evt -> {
            int viewRow = table.getSelectedRow();
            if (viewRow >= 0) {
                if (table.isEditing()) {
                    table.getCellEditor().cancelCellEditing();
                }
                action.accept(table.convertRowIndexToModel(viewRow));
            }
        });
        menu.add(item);
    }
}
 
