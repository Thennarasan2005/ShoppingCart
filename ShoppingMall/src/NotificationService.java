import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class NotificationService {

    static final String ACCOUNT_SID = "AC2ce598bc6a25f4d9bf2ff1dca0787ad2";
    static final String AUTH_TOKEN  = "3143be29c7973eaa2b3c15cff93a81f0";
    static final String TWILIO_NUM  = "+12545034207";

    public static void sendSMS(String toPhone, String body) {
        try {
            if (!toPhone.startsWith("+91")) {
                toPhone = "+91" + toPhone;
            }

            Twilio.init(ACCOUNT_SID, AUTH_TOKEN);

            Message msg = Message.creator(
                    new PhoneNumber(toPhone),
                    new PhoneNumber(TWILIO_NUM),
                    body
            ).create();

            System.out.println("SMS Sent SID: " + msg.getSid());

        } catch (Exception e) {
            System.out.println("SMS Error: " + e.getMessage());
        }
    }

    public static void sendCartNotifications() {
        try (Connection con = DBConnection.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                "SELECT user_name, product_name FROM cart_table"
            );
            ResultSet rs = ps.executeQuery();

            LocalDate today = LocalDate.now();

            while (rs.next()) {
                String userName = rs.getString("user_name");
                String product  = rs.getString("product_name");

                PreparedStatement offerPs = con.prepareStatement(
                    "SELECT offer_time, offer_percentage FROM product_info WHERE product_name=?"
                );
                offerPs.setString(1, product);
                ResultSet offerRs = offerPs.executeQuery();

                if (!offerRs.next()) continue;

                LocalDate deadline  = LocalDate.parse(offerRs.getString("offer_time"));
                double offerPercent = offerRs.getDouble("offer_percentage");
                long daysLeft       = ChronoUnit.DAYS.between(today, deadline);

                PreparedStatement phonePs = con.prepareStatement(
                    "SELECT phone FROM customer_info WHERE user_name=?"
                );
                phonePs.setString(1, userName);
                ResultSet phoneRs = phonePs.executeQuery();

                if (!phoneRs.next()) {
                    System.out.println("No phone found for: " + userName);
                    continue;
                }

                String phone = phoneRs.getString("phone");

                if (daysLeft > 0) {
                    String smsBody = "Dear " + userName +
                                     ", only " + daysLeft +
                                     " days left for " + product +
                                     " offer! Discount: " + offerPercent + "%.";
                    sendSMS(phone, smsBody);

                } else if (daysLeft == 0) {
                    System.out.println(userName + ": Today is last day for " + product);

                } else {
                    System.out.println(userName + ": Offer expired for " + product);
                }
            }

        } catch (SQLException e) {
            System.out.println("DB Error: " + e.getMessage());
        }
    }
}