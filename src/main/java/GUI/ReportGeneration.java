/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import CODE.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.swing.JOptionPane;
/**
 *
 * @author tharu
 */
public class ReportGeneration extends javax.swing.JFrame {

    /**
     * Creates new form ReportGeneration
     */
    public ReportGeneration() {
        initComponents();

        // One report per team member
        jButton1.addActionListener(evt -> showCustomerReport());          // Customer Report
        jButton2.addActionListener(evt -> showInventoryReport());         // Inventory Report
        jButton3.addActionListener(evt -> showOrderReport());             // Order Report
        jButton4.addActionListener(evt -> showUserReport());              // User Report
        jButton5.addActionListener(evt -> showSupplierReport());          // Supplier Report
        jButton6.addActionListener(evt -> showTierReport());              // Tier Report
        btnPaymentReport.addActionListener(evt -> showPaymentReport());   // Payment Report (Step 1)
    }

    // ===================== REPORT GENERATION =====================
    // Each team member's report is its own method below.

    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(ReportGeneration.class.getName());

    // Time periods for the Order and Payment reports (fixed text, so it is safe to put in the SQL)
    private static final String[] PERIOD_NAMES = {"Today", "Last 7 Days", "This Month", "All Time"};
    private static final String[] PERIOD_SQL = {
        "DATE(o.order_date) = CURDATE()",
        "o.order_date >= CURDATE() - INTERVAL 6 DAY",
        "YEAR(o.order_date) = YEAR(CURDATE()) AND MONTH(o.order_date) = MONTH(CURDATE())",
        "1 = 1"
    };

    // Asks which period to report on. Returns -1 if the box is closed.
    private int askPeriod() {
        return JOptionPane.showOptionDialog(jPanel1, "Which period should the report cover?",
                "Report Period", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, PERIOD_NAMES, PERIOD_NAMES[0]);
    }

    // 1. CUSTOMER REPORT - M.W.T.D. Fernando (Customer Management)
    private void showCustomerReport() {
        String sql = "SELECT c.customer_id AS `ID`, "
                   + "COALESCE(NULLIF(c.full_name, ''), '-') AS `Name`, "
                   + "c.phone_number AS `Phone`, "
                   + "COALESCE(c.loyalty_tier, '-') AS `Tier`, "
                   + "c.loyalty_points AS `Points`, "
                   + "COALESCE(s.order_count, 0) AS `Paid Orders`, "
                   + "FORMAT(COALESCE(s.total_spent, 0), 2) AS `Total Spent (Rs.)` "
                   + "FROM customers c "
                   + "LEFT JOIN (SELECT o.customer_id, COUNT(*) AS order_count, SUM(o.total_amount) AS total_spent "
                   + "           FROM orders o JOIN payments p ON p.order_id = o.order_id "
                   + "           WHERE p.payment_status = 'Success' GROUP BY o.customer_id) s "
                   + "  ON s.customer_id = c.customer_id "
                   + "ORDER BY COALESCE(s.total_spent, 0) DESC";

        String summary = "SELECT COUNT(*) AS `Total Customers`, "
                       + "COALESCE(SUM(loyalty_points), 0) AS `Total Loyalty Points` "
                       + "FROM customers";

        showReport("Customer Report", sql, summary);
    }

    // 2. USER (STAFF) REPORT - M.W.T.D. Fernando (User Login)
    private void showUserReport() {
        String sql = "SELECT u.user_id AS `Staff ID`, u.full_name AS `Name`, u.username AS `Username`, "
                   + "u.role AS `Role`, u.status AS `Status`, "
                   + "DATE_FORMAT(u.created_at, '%Y-%m-%d') AS `Created`, "
                   + "COALESCE(s.order_count, 0) AS `Orders Handled`, "
                   + "FORMAT(COALESCE(s.total_sales, 0), 2) AS `Sales Handled (Rs.)` "
                   + "FROM users u "
                   + "LEFT JOIN (SELECT o.user_id, COUNT(*) AS order_count, SUM(o.total_amount) AS total_sales "
                   + "           FROM orders o JOIN payments p ON p.order_id = o.order_id "
                   + "           WHERE p.payment_status = 'Success' GROUP BY o.user_id) s "
                   + "  ON s.user_id = u.user_id "
                   + "ORDER BY u.user_id";

        String summary = "SELECT COUNT(*) AS `Total Staff`, "
                       + "COALESCE(SUM(role = 'Admin'), 0) AS `Admins`, "
                       + "COALESCE(SUM(status = 'Active'), 0) AS `Active Accounts` "
                       + "FROM users";

        showReport("Staff (User) Report", sql, summary);
    }

    // 3. SUPPLIER REPORT - Rusandi Minethma (Supplier Management)
    private void showSupplierReport() {
        String sql = "SELECT s.supplier_name AS `Supplier`, s.contact_person AS `Contact Person`, "
                   + "s.phone_number AS `Phone`, s.status AS `Status`, "
                   + "COUNT(p.product_id) AS `Active Products`, "
                   + "COALESCE(SUM(i.quantity_on_hand), 0) AS `Units In Stock`, "
                   + "COALESCE(SUM(i.quantity_on_hand <= i.reorder_level), 0) AS `Low Stock Items` "
                   + "FROM suppliers s "
                   + "LEFT JOIN products p ON p.supplier_id = s.supplier_id AND p.status = 'Active' "
                   + "LEFT JOIN inventory i ON i.product_id = p.product_id "
                   + "GROUP BY s.supplier_id, s.supplier_name, s.contact_person, s.phone_number, s.status "
                   + "ORDER BY s.supplier_name";

        String summary = "SELECT COUNT(*) AS `Total Suppliers`, "
                       + "COALESCE(SUM(status = 'Active'), 0) AS `Active Suppliers` "
                       + "FROM suppliers";

        showReport("Supplier Report", sql, summary);
    }

    // 4. ORDER REPORT - R.A.S.R. Ranasinghe (Order Management)
    private void showOrderReport() {
        int period = askPeriod();
        if (period < 0) {
            return; // box was closed
        }
        String where = " WHERE " + PERIOD_SQL[period] + " ";

        String sql = "SELECT o.order_id AS `Order No`, "
                   + "DATE_FORMAT(o.order_date, '%Y-%m-%d %H:%i') AS `Date`, "
                   + "COALESCE(NULLIF(c.full_name, ''), c.phone_number) AS `Customer`, "
                   + "COALESCE(u.full_name, '-') AS `Cashier`, "
                   + "(SELECT COALESCE(SUM(oi.quantity), 0) FROM order_items oi WHERE oi.order_id = o.order_id) AS `Items`, "
                   + "FORMAT(o.total_amount, 2) AS `Total (Rs.)`, "
                   + "o.order_status AS `Status` "
                   + "FROM orders o "
                   + "JOIN customers c ON c.customer_id = o.customer_id "
                   + "LEFT JOIN users u ON u.user_id = o.user_id "
                   + where
                   + "ORDER BY o.order_id DESC";

        String summary = "SELECT COUNT(*) AS `Orders`, "
                       + "COALESCE(SUM(o.order_status = 'Refunded'), 0) AS `Refunded`, "
                       + "FORMAT(COALESCE(SUM(CASE WHEN o.order_status <> 'Refunded' THEN o.total_amount END), 0), 2) AS `Net Sales (Rs.)` "
                       + "FROM orders o" + where;

        showReport("Order Report - " + PERIOD_NAMES[period], sql, summary);
    }

    // 5. PAYMENT REPORT - K.K.G.M.P. Abeyrathna (Payment Handling)
    private void showPaymentReport() {
        int period = askPeriod();
        if (period < 0) {
            return; // box was closed
        }
        String where = " WHERE " + PERIOD_SQL[period] + " ";

        String sql = "SELECT o.order_id AS `Order No`, "
                   + "DATE_FORMAT(o.order_date, '%Y-%m-%d %H:%i') AS `Date`, "
                   + "p.payment_method AS `Method`, "
                   + "p.payment_status AS `Status`, "
                   + "FORMAT(o.total_amount, 2) AS `Amount Due (Rs.)`, "
                   + "FORMAT(p.amount_paid, 2) AS `Amount Paid (Rs.)`, "
                   + "FORMAT(p.amount_paid - o.total_amount, 2) AS `Change (Rs.)` "
                   + "FROM payments p "
                   + "JOIN orders o ON o.order_id = p.order_id "
                   + where
                   + "ORDER BY o.order_id DESC";

        String summary = "SELECT COUNT(*) AS `Payments`, "
                       + "FORMAT(COALESCE(SUM(CASE WHEN p.payment_status = 'Success' AND p.payment_method = 'Cash' THEN o.total_amount END), 0), 2) AS `Cash (Rs.)`, "
                       + "FORMAT(COALESCE(SUM(CASE WHEN p.payment_status = 'Success' AND p.payment_method = 'Card' THEN o.total_amount END), 0), 2) AS `Card (Rs.)`, "
                       + "FORMAT(COALESCE(SUM(CASE WHEN p.payment_status = 'Refunded' THEN o.total_amount END), 0), 2) AS `Refunded (Rs.)` "
                       + "FROM payments p JOIN orders o ON o.order_id = p.order_id" + where;

        showReport("Payment Report - " + PERIOD_NAMES[period], sql, summary);
    }

    // 6. TIER & DELIVERY REPORT - Senura Waidyarathna (Discount & Delivery)
    private void showTierReport() {
        String sql = "SELECT t.tier_name AS `Tier`, "
                   + "CASE WHEN t.max_points IS NULL THEN CONCAT(t.min_points, '+') "
                   + "     ELSE CONCAT(t.min_points, ' - ', t.max_points) END AS `Point Range`, "
                   + "CONCAT(t.discount_percentage, '%') AS `Discount`, "
                   + "COUNT(c.customer_id) AS `Customers In Tier` "
                   + "FROM loyalty_tiers t "
                   + "LEFT JOIN customers c ON c.loyalty_tier = t.tier_name "
                   + "GROUP BY t.tier_id, t.tier_name, t.min_points, t.max_points, t.discount_percentage "
                   + "ORDER BY t.min_points";

        String summary = "SELECT (SELECT COUNT(*) FROM loyalty_tiers) AS `Tier Rules`, "
                       + "(SELECT COUNT(*) FROM deliveries) AS `Total Deliveries`, "
                       + "(SELECT COUNT(*) FROM deliveries WHERE delivery_status = 'Pending') AS `Pending Deliveries`";

        showReport("Loyalty Tier & Delivery Report", sql, summary);
    }

    // 7. INVENTORY REPORT - Sanjana Ruwange (Inventory & Product Management)
    private void showInventoryReport() {
        String sql = "SELECT p.sku AS `SKU`, p.product_name AS `Product`, "
                   + "COALESCE(s.supplier_name, '-') AS `Supplier`, "
                   + "i.quantity_on_hand AS `In Stock`, "
                   + "i.reorder_level AS `Reorder Level`, "
                   + "CASE WHEN i.quantity_on_hand <= 0 THEN 'OUT OF STOCK' "
                   + "     WHEN i.quantity_on_hand <= i.reorder_level THEN 'LOW' "
                   + "     ELSE 'OK' END AS `Stock Status` "
                   + "FROM products p "
                   + "JOIN inventory i ON i.product_id = p.product_id "
                   + "LEFT JOIN suppliers s ON s.supplier_id = p.supplier_id "
                   + "WHERE p.status = 'Active' "
                   + "ORDER BY (i.quantity_on_hand <= i.reorder_level) DESC, p.product_name";

        String summary = "SELECT COUNT(*) AS `Active Products`, "
                       + "COALESCE(SUM(i.quantity_on_hand), 0) AS `Total Units`, "
                       + "COALESCE(SUM(i.quantity_on_hand <= i.reorder_level), 0) AS `Low / Out of Stock` "
                       + "FROM products p JOIN inventory i ON i.product_id = p.product_id "
                       + "WHERE p.status = 'Active'";

        showReport("Inventory Report", sql, summary);
    }

    // SHARED by every report: runs the SQL, fills a table and shows it in a pop-up window.
    // The table's column headings come from the AS `...` names in each query.
    private void showReport(String title, String sql, String summarySql) {
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only
            }
        };
        StringBuilder summary = new StringBuilder();

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Report rows
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columns = meta.getColumnCount();
                for (int c = 1; c <= columns; c++) {
                    model.addColumn(meta.getColumnLabel(c));
                }
                while (rs.next()) {
                    Object[] row = new Object[columns];
                    for (int c = 1; c <= columns; c++) {
                        row[c - 1] = rs.getObject(c);
                    }
                    model.addRow(row);
                }
            }

            // 2. Totals line shown under the table
            try (PreparedStatement ps = conn.prepareStatement(summarySql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    for (int c = 1; c <= meta.getColumnCount(); c++) {
                        if (c > 1) {
                            summary.append("   |   ");
                        }
                        summary.append(meta.getColumnLabel(c)).append(": ").append(rs.getString(c));
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.SEVERE, "Failed to generate " + title, e);
            JOptionPane.showMessageDialog(jPanel1, "Could not generate the report:\n" + e.getMessage(),
                    "Report Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String generatedOn = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new java.util.Date());

        // ---- Build the report window ----
        javax.swing.JDialog dialog = new javax.swing.JDialog(
                javax.swing.SwingUtilities.getWindowAncestor(jPanel1), title,
                java.awt.Dialog.ModalityType.APPLICATION_MODAL);

        javax.swing.JLabel lblTitle = new javax.swing.JLabel(title.toUpperCase());
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 22));
        javax.swing.JLabel lblInfo = new javax.swing.JLabel("Samadhi Pooja Banda   |   Generated: "
                + generatedOn + "   |   Records: " + model.getRowCount());
        lblInfo.setForeground(java.awt.Color.GRAY);

        javax.swing.JPanel top = new javax.swing.JPanel(new java.awt.GridLayout(2, 1));
        top.setOpaque(false);
        top.add(lblTitle);
        top.add(lblInfo);

        javax.swing.JTable table = new javax.swing.JTable(model);
        table.setRowHeight(28);

        javax.swing.JLabel lblSummary = new javax.swing.JLabel(summary.toString());
        lblSummary.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));

        javax.swing.JButton btnPrint = new javax.swing.JButton("Print / Save as PDF");
        btnPrint.addActionListener(evt -> {
            try {
                table.print(javax.swing.JTable.PrintMode.FIT_WIDTH,
                        new java.text.MessageFormat(title),
                        new java.text.MessageFormat("Generated " + generatedOn + "  -  Page {0}"));
            } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(dialog, "Could not print: " + ex.getMessage(),
                        "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        javax.swing.JButton btnClose = new javax.swing.JButton("Close");
        btnClose.addActionListener(evt -> dialog.dispose());

        javax.swing.JPanel buttons = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(btnPrint);
        buttons.add(btnClose);

        javax.swing.JPanel bottom = new javax.swing.JPanel(new java.awt.BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblSummary, java.awt.BorderLayout.CENTER);
        bottom.add(buttons, java.awt.BorderLayout.EAST);

        javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.BorderLayout(0, 12));
        content.setBackground(java.awt.Color.WHITE);
        content.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 20, 15, 20));
        content.add(top, java.awt.BorderLayout.NORTH);
        content.add(new javax.swing.JScrollPane(table), java.awt.BorderLayout.CENTER);
        content.add(bottom, java.awt.BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setSize(1100, 600);
        dialog.setLocationRelativeTo(jPanel1);
        dialog.setVisible(true);
    }    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jButton1 = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        btnPaymentReport = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1200, 700));

        jButton1.setText("Customer Report");

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Report Generation");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel1)
                .addContainerGap(966, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(48, Short.MAX_VALUE)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jButton2.setText("Inventory Report");

        jButton3.setText("Order Report");

        jButton4.setText("User Report");

        jButton5.setText("Supplier Report");

        jButton6.setText("Tier Report");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        btnPaymentReport.setText("Payment Report");
        btnPaymentReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPaymentReportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnPaymentReport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnPaymentReport, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(20, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton6ActionPerformed

    private void btnPaymentReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPaymentReportActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnPaymentReportActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ReportGeneration.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ReportGeneration.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ReportGeneration.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ReportGeneration.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ReportGeneration().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnPaymentReport;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    // End of variables declaration//GEN-END:variables
}
