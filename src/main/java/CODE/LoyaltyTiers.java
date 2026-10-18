/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package CODE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * @author tharu
 */
public class LoyaltyTiers {
 
    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(LoyaltyTiers.class.getName());
 
    // Picks the right tier name for a customer's points.
    // If no tier matches (points below every MIN POINTS), the customer keeps their current tier.
    private static final String TIER_FOR_POINTS =
              "COALESCE((SELECT t.tier_name FROM loyalty_tiers t "
            + "          WHERE t.min_points <= c.loyalty_points "
            + "          ORDER BY t.min_points DESC LIMIT 1), c.loyalty_tier)";
 
    // Re-checks the customer who placed this order (after a sale or a refund).
    // Uses the caller's connection, so it can be part of the same transaction.
    public static void updateCustomerOfOrder(Connection conn, int orderId) throws SQLException {
        String sql = "UPDATE customers c "
                   + "JOIN orders o ON o.customer_id = c.customer_id "
                   + "SET c.loyalty_tier = " + TIER_FOR_POINTS + " "
                   + "WHERE o.order_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.executeUpdate();
        }
    }
 
    // Re-checks EVERY customer (when tier rules change, or when the customer list opens)
    public static void updateAllCustomers() {
        String sql = "UPDATE customers c SET c.loyalty_tier = " + TIER_FOR_POINTS;
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.log(java.util.logging.Level.WARNING, "Could not update customer loyalty tiers", e);
        }
    }
}
