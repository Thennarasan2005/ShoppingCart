import java.sql.*;
import java.util.Scanner;

public class CustomerService {

    Scanner sc = new Scanner(System.in);

    public void signup() {
        System.out.print("Set username: ");
        String username = sc.nextLine();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        System.out.print("Re-enter password: ");
        String rePassword = sc.nextLine();

        System.out.print("Enter phone: ");
        String phone = sc.nextLine();

        if (!password.equals(rePassword)) {
            System.out.println("Password mismatch ❌");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement check = con.prepareStatement(
                "SELECT * FROM customer_info WHERE user_name=?"
            );
            check.setString(1, username);
            ResultSet rs = check.executeQuery();

            if (rs.next()) {
                System.out.println("User already exists ❌");
            } else {
                PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO customer_info VALUES (?,?,?)"
                );
                insert.setString(1, username);
                insert.setString(2, password);
                insert.setString(3, phone);
                insert.executeUpdate();
                System.out.println("Account created successfully ✅");
                login();
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
    }

    public void login() {
        System.out.print("Enter username: ");
        String username = sc.nextLine();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM customer_info WHERE user_name=? AND password=?"
            );
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Welcome " + username + " ✅");
                ProductService productService = new ProductService();
                productService.showProducts(username);
            } else {
                System.out.println("Invalid credentials ❌");
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
    }
}