/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package GUI;

import CODE.Session;
import CODE.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
/**
 *
 * @author tharu
 */
public class Dashboard extends javax.swing.JFrame {

    /**
     * Creates new form Logging
     */
    public Dashboard() {
        initComponents();
        jPanel1.setPreferredSize(null);
        jPanel2.setPreferredSize(null);
        setBounds(java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
        setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);

        btnManageStaff.setVisible(Session.isAdmin());
        btnManageStaff.addActionListener(evt -> loadContent(new ManageStaff()));

        jButton8.addActionListener(evt -> signOut());

            jButton2.addActionListener(evt -> loadContent(new Customer_management()));
            jButton3.addActionListener(evt -> loadContent(new Supplierlist()));
            jButton4.addActionListener(evt -> loadContent(new POS_Billing()));
            jButton6.addActionListener(evt -> loadContent(new Discount_and_Delivery()));
            jButton7.addActionListener(evt -> loadContent(new Inventory_and_product_management()));
            jButton5.addActionListener(evt -> loadContent(new ReportGeneration())); // Reports button            
            jButton1.addActionListener(evt -> showDashboardHome()); // Dashboard button reloads the stats
            showDashboardHome();    
    }
    
    private void loadContent(javax.swing.JFrame moduleFrame) {
        jPanel2.removeAll();
        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel2.add(CODE.NavBar.create("← Back to Dashboard", this::showDashboardHome), java.awt.BorderLayout.NORTH);        
        javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(moduleFrame.getContentPane());
        scroll.setBorder(null);
        jPanel2.add(scroll, java.awt.BorderLayout.CENTER);
        jPanel2.revalidate();
        jPanel2.repaint();
    }

    private void signOut() {
        Session.logout();
        new Logging().setVisible(true);
        this.dispose();
    }

    // ===================== DASHBOARD STATISTICS =====================

    // Builds the statistics page and shows it in the right-side area (jPanel2)
    private void showDashboardHome() {
        javax.swing.JPanel home = new javax.swing.JPanel(new java.awt.BorderLayout(0, 20));
        home.setBackground(new java.awt.Color(246, 245, 242));
        home.setBorder(javax.swing.BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // Page title
        javax.swing.JLabel title = new javax.swing.JLabel("DASHBOARD OVERVIEW");
        title.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
        home.add(title, java.awt.BorderLayout.NORTH);

        // 6 cards: 2 rows x 3 columns
        javax.swing.JPanel cards = new javax.swing.JPanel(new java.awt.GridLayout(2, 3, 20, 20));
        cards.setOpaque(false);

        cards.add(createStatCard("Today's Sales",
                getStat("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE DATE(order_date) = CURDATE()", true),
                new java.awt.Color(11, 107, 109)));

        cards.add(createStatCard("Today's Orders",
                getStat("SELECT COUNT(*) FROM orders WHERE DATE(order_date) = CURDATE()", false),
                new java.awt.Color(52, 152, 219)));

        cards.add(createStatCard("Total Customers",
                getStat("SELECT COUNT(*) FROM customers", false),
                new java.awt.Color(46, 139, 87)));

        cards.add(createStatCard("Active Products",
                getStat("SELECT COUNT(*) FROM products WHERE status = 'Active'", false),
                new java.awt.Color(142, 68, 173)));

        cards.add(createStatCard("Low Stock Items",
                getStat("SELECT COUNT(*) FROM products p JOIN inventory i ON p.product_id = i.product_id "
                      + "WHERE p.status = 'Active' AND i.quantity_on_hand <= i.reorder_level", false),
                new java.awt.Color(231, 76, 60)));

        cards.add(createStatCard("Active Suppliers",
                getStat("SELECT COUNT(*) FROM suppliers WHERE status = 'Active'", false),
                new java.awt.Color(255, 153, 102)));

        // Cards on top, low-stock table below
        javax.swing.JPanel center = new javax.swing.JPanel(new java.awt.BorderLayout(0, 25));
        center.setOpaque(false);
        center.add(cards, java.awt.BorderLayout.NORTH);
        center.add(createLowStockSection(), java.awt.BorderLayout.CENTER);
        home.add(center, java.awt.BorderLayout.CENTER);

        // Put the page into the content area (same idea as loadContent)
        jPanel2.removeAll();
        jPanel2.setLayout(new java.awt.BorderLayout());
        jPanel2.add(home, java.awt.BorderLayout.CENTER);
        jPanel2.revalidate();
        jPanel2.repaint();
    }

    // Makes one white card: small grey title + big number + coloured strip on the left
    private javax.swing.JPanel createStatCard(String title, String value, java.awt.Color accent) {
        javax.swing.JPanel card = new javax.swing.JPanel(new java.awt.BorderLayout(0, 10));
        card.setBackground(java.awt.Color.WHITE);
        card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                javax.swing.BorderFactory.createMatteBorder(0, 6, 0, 0, accent),
                javax.swing.BorderFactory.createEmptyBorder(18, 20, 18, 20)));

        javax.swing.JLabel lblTitle = new javax.swing.JLabel(title);
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        lblTitle.setForeground(java.awt.Color.GRAY);

        javax.swing.JLabel lblValue = new javax.swing.JLabel(value);
        lblValue.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));

        card.add(lblTitle, java.awt.BorderLayout.NORTH);
        card.add(lblValue, java.awt.BorderLayout.CENTER);
        return card;
    }

    // Runs a query that returns ONE number (COUNT or SUM) and returns it as text.
    // If the query fails (e.g. a wrong column name), the card shows "-" instead of crashing.
    private String getStat(String sql, boolean isMoney) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                if (isMoney) {
                    return "Rs. " + String.format("%,.2f", rs.getDouble(1));
                }
                return String.valueOf(rs.getInt(1));
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(Dashboard.class.getName())
                    .log(java.util.logging.Level.WARNING, "Dashboard stat failed: " + sql, e);
        }
        return "-";
    }

    // Table of products at or below their reorder level
    private javax.swing.JPanel createLowStockSection() {
        javax.swing.JPanel section = new javax.swing.JPanel(new java.awt.BorderLayout(0, 10));
        section.setOpaque(false);

        javax.swing.JLabel heading = new javax.swing.JLabel("Low Stock Alerts");
        heading.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        section.add(heading, java.awt.BorderLayout.NORTH);

        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new String[]{"PRODUCT", "SKU", "STOCK", "REORDER LEVEL"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only
            }
        };

        String sql = "SELECT p.product_name, p.sku, i.quantity_on_hand, i.reorder_level "
                   + "FROM products p JOIN inventory i ON p.product_id = i.product_id "
                   + "WHERE p.status = 'Active' AND i.quantity_on_hand <= i.reorder_level "
                   + "ORDER BY i.quantity_on_hand ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("product_name"),
                    rs.getString("sku"),
                    rs.getInt("quantity_on_hand"),
                    rs.getInt("reorder_level")
                });
            }
        } catch (SQLException e) {
            java.util.logging.Logger.getLogger(Dashboard.class.getName())
                    .log(java.util.logging.Level.SEVERE, "Failed to load low stock list", e);
        }

        javax.swing.JTable table = new javax.swing.JTable(model);
        table.setRowHeight(30);
        section.add(new javax.swing.JScrollPane(table), java.awt.BorderLayout.CENTER);
        return section;
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
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        btnManageStaff = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(245, 243, 240));
        jPanel1.setPreferredSize(new java.awt.Dimension(1150, 700));

        jPanel4.setBackground(new java.awt.Color(11, 107, 109));
        jPanel4.setPreferredSize(new java.awt.Dimension(300, 700));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Samadhi Pooja Banda");

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton1.setText("Dashboard");

        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton2.setLabel("Customers");
        jButton2.setMaximumSize(new java.awt.Dimension(88, 23));
        jButton2.setMinimumSize(new java.awt.Dimension(88, 23));
        jButton2.setPreferredSize(new java.awt.Dimension(88, 23));

        jButton3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton3.setText("Supplier");
        jButton3.setMaximumSize(new java.awt.Dimension(88, 23));
        jButton3.setMinimumSize(new java.awt.Dimension(88, 23));
        jButton3.setPreferredSize(new java.awt.Dimension(88, 23));

        jButton4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton4.setText("Order");
        jButton4.setMaximumSize(new java.awt.Dimension(88, 23));
        jButton4.setMinimumSize(new java.awt.Dimension(88, 23));
        jButton4.setPreferredSize(new java.awt.Dimension(88, 23));

        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton5.setText("Reports");
        jButton5.setMaximumSize(new java.awt.Dimension(88, 23));
        jButton5.setMinimumSize(new java.awt.Dimension(88, 23));
        jButton5.setPreferredSize(new java.awt.Dimension(88, 23));

        jButton6.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton6.setText("Discount & Delivery");
        jButton6.setMaximumSize(new java.awt.Dimension(88, 23));
        jButton6.setMinimumSize(new java.awt.Dimension(88, 23));
        jButton6.setPreferredSize(new java.awt.Dimension(88, 23));

        jButton7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton7.setText("Inventory & Products");
        jButton7.setMaximumSize(new java.awt.Dimension(88, 23));
        jButton7.setMinimumSize(new java.awt.Dimension(88, 23));
        jButton7.setPreferredSize(new java.awt.Dimension(88, 23));

        jButton8.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jButton8.setText("Sign out");
        jButton8.setMaximumSize(new java.awt.Dimension(88, 23));
        jButton8.setMinimumSize(new java.awt.Dimension(88, 23));
        jButton8.setPreferredSize(new java.awt.Dimension(88, 23));

        btnManageStaff.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        btnManageStaff.setText("Manage Staff");
        btnManageStaff.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnManageStaffActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGap(29, 29, 29)
                                .addComponent(jLabel1))
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 21, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnManageStaff, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton7, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addComponent(jLabel1)
                .addGap(93, 93, 93)
                .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton3, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton7, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnManageStaff, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addComponent(jButton8, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16))
        );

        jPanel2.setBackground(new java.awt.Color(246, 245, 242));
        jPanel2.setPreferredSize(new java.awt.Dimension(1200, 700));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1188, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, 1188, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1500, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnManageStaffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnManageStaffActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnManageStaffActionPerformed

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
            java.util.logging.Logger.getLogger(Dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Dashboard.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Dashboard().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnManageStaff;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    // End of variables declaration//GEN-END:variables
}
