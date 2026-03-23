import java.sql.*;

public class DeliveryService {

    // ======= GET RANDOM DELIVERY BOY =======
    public static String getDeliveryBoy() {
        try (Connection con = DBConnection.getConnection()) {

            ResultSet rs = con.createStatement().executeQuery(
                "SELECT delivery_boy FROM emp_table ORDER BY RAND() LIMIT 1"
            );

            if (rs.next()) {
                return rs.getString("delivery_boy");
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
        return null;
    }

    // ======= UPDATE DELIVERY STATUS =======
    public static void updateDeliveryStatus(int productId, String status) {
        try (Connection con = DBConnection.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                "UPDATE customer_table SET Delivery_status=? WHERE product_id=?"
            );
            ps.setString(1, status);
            ps.setInt(2, productId);
            ps.executeUpdate();

            System.out.println("Delivery status updated to: " + status + " ✅");

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
    }

    // ======= VIEW ALL DELIVERIES =======
    public static void viewAllDeliveries() {
        try (Connection con = DBConnection.getConnection()) {

            ResultSet rs = con.createStatement().executeQuery(
                "SELECT * FROM customer_table"
            );

            System.out.println("\nOrder ID | Customer | Product | Delivery Boy | Status | Cost");
            System.out.println("-".repeat(80));

            while (rs.next()) {
                System.out.printf("%d | %s | %s | %s | %s | %.2f%n",
                    rs.getInt("product_id"),
                    rs.getString("customer_name"),
                    rs.getString("product_name"),
                    rs.getString("Delivery_boy"),
                    rs.getString("Delivery_status"),
                    rs.getDouble("product_cost")
                );
            }

            System.out.println("-".repeat(80));

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
    }
}