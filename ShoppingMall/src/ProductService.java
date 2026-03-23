import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Scanner;

public class ProductService {

    Scanner sc = new Scanner(System.in);

    public void showProducts(String username) {
        try (Connection con = DBConnection.getConnection()) {

            boolean keepShopping = true;

            while (keepShopping) {

                PreparedStatement ps = con.prepareStatement(
                    "SELECT product_name, Quantity, cost_price, offer_percentage, offer_time " +
                    "FROM product_info WHERE Quantity > 0"
                );
                ResultSet rs = ps.executeQuery();

                System.out.println("\nProduct | Stock | Price | Discount% | Deadline | Selling Price");
                System.out.println("-".repeat(80));

                LocalDate today = LocalDate.now();
                boolean anyProduct = false;

                while (rs.next()) {
                    LocalDate deadline = LocalDate.parse(rs.getString("offer_time"));
                    if (deadline.isAfter(today)) {
                        anyProduct = true;
                        double cost         = rs.getDouble("cost_price");
                        double offer        = rs.getDouble("offer_percentage");
                        double sellingPrice = cost - (cost * offer / 100);

                        System.out.printf("%s | %d | %.2f | %.2f%% | %s | %.2f%n",
                            rs.getString("product_name"),
                            rs.getInt("Quantity"),
                            cost,
                            offer,
                            rs.getString("offer_time"),
                            sellingPrice);
                    }
                }

                if (!anyProduct) {
                    System.out.println("No products available right now.");
                    return;
                }

                System.out.println("-".repeat(80));
                System.out.println("1. Buy");
                System.out.println("2. Add to Cart");
                System.out.println("3. Delte item from the cart");

                String choice = "";
                while (true) {
                    System.out.print("Enter choice (1 or 2 or 3): ");
                    choice = sc.nextLine().trim();
                    if (choice.equals("1") || choice.equals("2") || choice.equals("3")) {
                        break;
                    }
                    System.out.println("Invalid choice! Please enter 1 to Buy or 2 to Add to Cart.");
                }

                boolean tryAnotherProduct = false;

                if (choice.equals("1")) {
                    tryAnotherProduct = buyProduct(username, con);
                }
                else if(choice.equals("2")){
                    addToCart(username, con);
                }
                else {
                	removeFromCart(username,con);
                }

                // If buyProduct() returned true, it means user wants to buy another product
                // so we loop again. Otherwise, ask if they want to continue shopping.
                if (!tryAnotherProduct) {
                    System.out.print("\nDo you want to continue shopping? (yes/no): ");
                    String cont = sc.nextLine().trim().toLowerCase();
                    if (!cont.equals("yes")) {
                        keepShopping = false;
                        System.out.println("Thank you for shopping! Goodbye.");
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
    }

    // Returns true  → user wants to buy another product (loop again)
    // Returns false → normal exit or order placed
    private boolean buyProduct(String username, Connection con) throws SQLException {

        System.out.print("Enter product name to buy: ");
        String product = sc.nextLine().trim();

        // ---- Check if user already ordered this product ----
        PreparedStatement orderCheck = con.prepareStatement(
            "SELECT * FROM customer_table WHERE customer_name = ? AND product_name = ?"
        );
        orderCheck.setString(1, username);
        orderCheck.setString(2, product);
        ResultSet orderRs = orderCheck.executeQuery();

        if (orderRs.next()) {
            // Product already purchased — show clear message
            System.out.println("\n=======================================");
            System.out.println("  You have already purchased \"" + product + "\" before!");
            System.out.println("=======================================");
            System.out.println("1. Buy it again");
            System.out.println("2. Choose a different product");
            System.out.println("3. Cancel and go back");

            String option = "";
            while (true) {
                System.out.print("Enter your choice (1/2/3): ");
                option = sc.nextLine().trim();
                if (option.equals("1") || option.equals("2") || option.equals("3")) {
                    break;
                }
                System.out.println("Invalid input! Please enter 1, 2, or 3.");
            }

            if (option.equals("1")) {
                // Proceed to place the order again (fall through below)
                System.out.println("Proceeding to order \"" + product + "\" again...");
            } else if (option.equals("2")) {
                // Signal the caller to show the product list again
                System.out.println("Redirecting you to the product list...");
                return true;
            } else {
                System.out.println("Order cancelled. Returning to menu.");
                return false;
            }
        }

        // ---- Check product exists and has stock ----
        PreparedStatement ps = con.prepareStatement(
            "SELECT * FROM product_info WHERE Quantity > 0 AND product_name = ?"
        );
        ps.setString(1, product);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            double cost       = rs.getDouble("cost_price");
            double offer      = rs.getDouble("offer_percentage");
            double finalPrice = cost - (cost * offer / 100);
            int currentQty    = rs.getInt("Quantity");

            // ---- Get random delivery boy ----
            ResultSet empRs = con.createStatement()
                .executeQuery("SELECT name AS emp_name FROM emp_names ORDER BY RAND() LIMIT 1");

            if (empRs.next()) {
                String deliveryBoy = empRs.getString("emp_name");

                con.setAutoCommit(false);

                try {
                    // ---- Insert order into customer_table ----
                    PreparedStatement insertOrder = con.prepareStatement(
                        "INSERT INTO customer_table " +
                        "(customer_name, product_name, Delivery_boy, Delivery_status, product_cost) " +
                        "VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                    );
                    insertOrder.setString(1, username);
                    insertOrder.setString(2, product);
                    insertOrder.setString(3, deliveryBoy);
                    insertOrder.setString(4, "progress");
                    insertOrder.setDouble(5, finalPrice);
                    insertOrder.executeUpdate();

                    // ---- Retrieve the auto-generated product_id ----
                    int pid = 0;
                    ResultSet generatedKeys = insertOrder.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        pid = generatedKeys.getInt(1);
                    }

                    // ---- Insert details into emp_table ----
                    PreparedStatement insertOrderToEmployee = con.prepareStatement(
                        "INSERT INTO emp_table " +
                        "(customer_name, delivery_boy, product_name, product_id, selling_price, completed) " +
                        "VALUES (?, ?, ?, ?, ?, ?)"
                    );
                    insertOrderToEmployee.setString(1, username);
                    insertOrderToEmployee.setString(2, deliveryBoy);
                    insertOrderToEmployee.setString(3, product);
                    insertOrderToEmployee.setInt(4, pid);
                    insertOrderToEmployee.setDouble(5, finalPrice);
                    insertOrderToEmployee.setString(6, "progress");
                    insertOrderToEmployee.executeUpdate();

                    // ---- Reduce quantity by 1 ----
                    PreparedStatement updateQty = con.prepareStatement(
                        "UPDATE product_info SET Quantity = Quantity - 1 WHERE product_name = ?"
                    );
                    updateQty.setString(1, product);
                    updateQty.executeUpdate();

                    con.commit();

                    System.out.println("\n=======================================");
                    System.out.println("  Order placed successfully!");
                    System.out.println("  Product  : " + product);
                    System.out.println("  Price    : Rs." + String.format("%.2f", finalPrice));
                    System.out.println("  Delivery : " + deliveryBoy);
                    System.out.println("  Status   : In Progress");
                    System.out.println("  Remaining Stock: " + (currentQty - 1));
                    System.out.println("=======================================\n");

                    // ---- Send SMS confirmation ----
                    PreparedStatement phonePs = con.prepareStatement(
                        "SELECT phone FROM customer_info WHERE user_name = ?"
                    );
                    phonePs.setString(1, username);
                    ResultSet phoneRs = phonePs.executeQuery();

                    if (phoneRs.next()) {
                        String phone = phoneRs.getString("phone");
                        String smsBody = "Dear " + username +
                                         ", your order for " + product +
                                         " is placed! Price: Rs." + String.format("%.2f", finalPrice) +
                                         ". Delivery by: " + deliveryBoy + ". Thank you!";
                        NotificationService.sendSMS(phone, smsBody);
                    }

                } catch (SQLException e) {
                    con.rollback();
                    System.out.println("Order failed, changes rolled back: " + e.getMessage());
                } finally {
                    con.setAutoCommit(true);
                }

            } else {
                System.out.println("No delivery boys available right now.");
            }

        } else {
            System.out.println("Product not found or out of stock!");
        }

        return false;
    }

    private void addToCart(String username, Connection con) throws SQLException {

        System.out.print("Enter product name to add to cart: ");
        String product = sc.nextLine().trim();

        // ---- Check product exists and has stock ----
        PreparedStatement ps = con.prepareStatement(
            "SELECT product_name FROM product_info WHERE Quantity > 0 AND product_name = ?"
        );
        ps.setString(1, product);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {

            // ---- Check if already in cart ----
            PreparedStatement cartCheck = con.prepareStatement(
                "SELECT * FROM cart_table WHERE user_name = ? AND product_name = ?"
            );
            cartCheck.setString(1, username);
            cartCheck.setString(2, product);
            ResultSet cartRs = cartCheck.executeQuery();

            if (cartRs.next()) {
                System.out.println("Product already in your cart!");
            } else {
                PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO cart_table (user_name, product_name) VALUES (?, ?)"
                );
                insert.setString(1, username);
                insert.setString(2, product);
                insert.executeUpdate();

                System.out.println("\n=======================================");
                System.out.println("  Added to cart successfully!");
                System.out.println("  Product: " + product);
                System.out.println("=======================================\n");

                // ---- Send SMS confirmation ----
                PreparedStatement phonePs = con.prepareStatement(
                    "SELECT phone FROM customer_info WHERE user_name = ?"
                );
                phonePs.setString(1, username);
                ResultSet phoneRs = phonePs.executeQuery();

                if (phoneRs.next()) {
                    String phone = phoneRs.getString("phone");
                    String smsBody = "Dear " + username +
                                     ", your product " + product +
                                     " has been added to cart successfully!";
                    NotificationService.sendSMS(phone, smsBody);
                }
            }

        } else {
            System.out.println("Product not available or out of stock!");
        }
    }
    
    private void removeFromCart(String username, Connection con) throws SQLException{
    	
    	System.out.println("Do you want to delete a product from a cart (Yes/No)");
    	String play=sc.nextLine().trim();
    	while (!play.equals("No")) {
    		System.out.println("enter product here to remove from the cart");
        	String item=sc.nextLine().trim();
        	
        	// getting the item data from the cart table
        	PreparedStatement ps=con.prepareStatement("SELECT product_name FROM cart_table WHERE product_name=? AND user_name=?");
        	ps.setString(1, item);
        	ps.setString(2, username);
        	
        	ResultSet Res=ps.executeQuery();
        	
        	if (Res.next()) {
        		//create a prepared statement for selecting a data from the database
            	PreparedStatement Ps=con.prepareStatement("DELETE FROM cart_table WHERE user_name=? AND product_name=?" );
            	
            	Ps.setString(1, username);
            	Ps.setString(2, item);
            	
            	int product_deleted=Ps.executeUpdate();
            	
            	if (product_deleted>0) {
            		System.out.println("product removed from the cart successfully");
            		
            		PreparedStatement phonePs=con.prepareStatement("SELECT phone FROM customer_info WHERE user_name=?");
            		phonePs.setString(1,username);
            		
            		ResultSet phone_no=phonePs.executeQuery();
            		
            		if (phone_no.next()) {
            			String p_no=phone_no.getString("phone");
            		
            			String Body_of_content="Dear "+username+ ", your "+item+" has been successfully removed from your cart ";
            		
            			NotificationService.sendSMS(p_no,Body_of_content);
            			
            			System.out.println("message sent successfully to your phone no");
            		}
            		
            		
            	}
            	System.out.println("Do you want to delete a product from the cart again (Yes/No)");
            	play=sc.nextLine().trim();
        	}
        	
        	else {
        		System.out.println("there is no product you entered");
        	}
    	}
    	    	
    }
  }
