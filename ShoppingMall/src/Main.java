import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=====================================");
        System.out.println("       Welcome to Meesho Shop        ");
        System.out.println("=====================================");
        System.out.println("1. Login");
        System.out.println("2. Signup");
        System.out.print("Enter choice: ");
        int choice = sc.nextInt();
        sc.nextLine();

        CustomerService cs = new CustomerService();

        if (choice == 1) {
            cs.login();
        } else if (choice == 2) {
            cs.signup();
        } else {
            System.out.println("Invalid choice");
        }

        sc.close();
    }
}