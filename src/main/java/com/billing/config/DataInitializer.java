package com.billing.config;

import com.billing.model.*;
import com.billing.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private int invoiceCounter = 1;

    @Bean
    public CommandLineRunner seedData(BusinessProfileRepository businessRepo,
                                      UserRepository users,
                                      ProductRepository products,
                                      CustomerRepository customers,
                                      OrderRepository orders,
                                      PasswordEncoder encoder) {
        return args -> {

            // ── Already registered via /register page — skip seeding ─
            if (businessRepo.existsByIdNotNull()) {
                log.info("✅ Business registered — skipping demo seed");
                return;
            }

            log.info("🍽️  Seeding Spice Garden Restaurant demo data...");

            // ════════════════════════════════════════════════════════
            // 1. BUSINESS PROFILE
            // ════════════════════════════════════════════════════════
            BusinessProfile biz = new BusinessProfile();
            biz.setBusinessName("Spice Garden Restaurant");
            biz.setBusinessType("RESTAURANT");
            biz.setOwnerName("Rajesh Kumar");
            biz.setPhone("9876543210");
            biz.setEmail("spicegarden@email.com");
            biz.setWebsite("www.spicegarden.in");
            biz.setAddressLine1("15, MG Road, Near City Mall");
            biz.setCity("Bangalore");
            biz.setState("Karnataka");
            biz.setPincode("560001");
            biz.setGstin("29AABCS1429B1Z1");
            biz.setInvoicePrefix("SG");
            biz.setRegisteredAt(LocalDateTime.now());
            businessRepo.save(biz);

            // ════════════════════════════════════════════════════════
            // 2. USERS
            // ════════════════════════════════════════════════════════
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("admin123"));
            admin.setRole(User.Role.ADMIN);
            admin.setFullName("Rajesh Kumar");
            admin.setActive(true);
            User savedAdmin = users.save(admin);

            User cashier = new User();
            cashier.setUsername("cashier");
            cashier.setPassword(encoder.encode("cashier123"));
            cashier.setRole(User.Role.STAFF);
            cashier.setFullName("Priya Sharma");
            cashier.setActive(true);
            users.save(cashier);

            User staff = new User();
            staff.setUsername("staff");
            staff.setPassword(encoder.encode("staff123"));
            staff.setRole(User.Role.STAFF);
            staff.setFullName("Arun Nair");
            staff.setActive(true);
            users.save(staff);
            log.info("✅ Users: admin / cashier / staff");

            // ════════════════════════════════════════════════════════
            // 3. MENU ITEMS  {name, description, price, qty, gst%, unit}
            // ════════════════════════════════════════════════════════
            String[][] menu = {
                // STARTERS
                {"Paneer Tikka",          "Grilled cottage cheese, mint chutney",    "280","50","5","PLATE"},
                {"Chicken Tikka",         "Tandoori chicken, 6 pcs",                 "320","50","5","PLATE"},
                {"Veg Spring Roll",       "Crispy rolls, veg filling, 4 pcs",        "160","60","5","PLATE"},
                {"Masala Papad",          "Roasted papad with toppings, 2 pcs",       "60","80","5","PLATE"},
                {"Samosa",               "Fried pastry, spiced potato, 2 pcs",        "80","80","5","PLATE"},
                {"Hara Bhara Kebab",      "Spinach & pea patty, 4 pcs",             "220","40","5","PLATE"},
                {"Chicken Lollipop",      "Spicy drumettes, Schezwan dip, 6 pcs",   "340","40","5","PLATE"},
                {"Mushroom 65",          "Crispy tossed mushrooms, 65-style",        "200","40","5","PLATE"},
                // MAIN COURSE — VEG
                {"Dal Makhani",          "Slow-cooked black lentils, cream",         "220","60","5","BOWL"},
                {"Paneer Butter Masala", "Rich tomato-cashew gravy",                 "280","60","5","BOWL"},
                {"Palak Paneer",         "Cottage cheese in spinach gravy",          "260","50","5","BOWL"},
                {"Kadai Paneer",         "Wok-tossed paneer with peppers",           "280","50","5","BOWL"},
                {"Mix Veg Curry",        "Seasonal vegetables, home-style gravy",    "200","60","5","BOWL"},
                {"Chole Masala",         "Spiced chickpeas, Punjabi style",          "180","60","5","BOWL"},
                {"Aloo Gobi",            "Potato & cauliflower, dry masala",         "180","50","5","BOWL"},
                {"Paneer Do Pyaza",      "Paneer with fried onions, rich gravy",     "280","40","5","BOWL"},
                // MAIN COURSE — NON-VEG
                {"Butter Chicken",       "Tender chicken, buttery tomato gravy",     "320","60","5","BOWL"},
                {"Chicken Biryani",      "Fragrant basmati, whole spices, raita",   "350","60","5","PLATE"},
                {"Chicken Biryani Half", "Half portion biryani with raita",          "220","60","5","PLATE"},
                {"Mutton Rogan Josh",    "Kashmiri slow-cooked mutton curry",        "420","40","5","BOWL"},
                {"Mutton Biryani",       "Slow-cooked mutton biryani, raita",        "420","40","5","PLATE"},
                {"Fish Curry",           "Coastal fish in coconut gravy",            "360","40","5","BOWL"},
                {"Prawn Masala",         "Juicy prawns, spiced onion gravy",         "400","30","5","BOWL"},
                {"Egg Curry",            "Hard-boiled eggs, tangy masala",           "200","50","5","BOWL"},
                // BREADS
                {"Butter Naan",          "Leavened bread, butter-glazed",             "45","100","5","PIECE"},
                {"Garlic Naan",          "Naan with garlic & coriander",              "55","100","5","PIECE"},
                {"Tandoori Roti",        "Whole wheat bread from tandoor",            "30","100","5","PIECE"},
                {"Lachha Paratha",       "Layered flaky wheat bread",                 "60","100","5","PIECE"},
                {"Puri",                "Deep-fried fluffy bread, 4 pcs",             "50","100","5","PLATE"},
                // RICE
                {"Steamed Rice",         "Plain basmati rice",                        "80","100","5","PLATE"},
                {"Jeera Rice",           "Basmati with cumin tempering",             "120","100","5","PLATE"},
                {"Veg Fried Rice",       "Indo-Chinese stir-fried rice",             "160","60","5","PLATE"},
                {"Egg Fried Rice",       "Wok-tossed rice with egg & veggies",       "180","60","5","PLATE"},
                // BEVERAGES (12% GST — packaged)
                {"Masala Chai",          "Spiced milk tea, ginger & cardamom",        "40","150","12","CUP"},
                {"Cold Coffee",          "Blended iced coffee with milk",            "120","100","12","GLASS"},
                {"Fresh Lime Soda",      "Sparkling lime, sweet or salt",             "80","100","12","GLASS"},
                {"Mango Lassi",          "Sweet chilled mango yogurt drink",         "100","100","12","GLASS"},
                {"Sweet Lassi",          "Chilled beaten yogurt drink",               "90","100","12","GLASS"},
                {"Mineral Water",        "Packaged drinking water, 1L",               "30","200","12","BOTTLE"},
                {"Fresh Juice",          "Seasonal fruit juice",                     "120","80","12","GLASS"},
                // DESSERTS
                {"Gulab Jamun",          "Soft milk-solid dumplings in syrup, 2 pcs","80","80","5","PLATE"},
                {"Ice Cream",            "1 scoop, choice of flavour",              "100","80","5","SCOOP"},
                {"Rasgulla",             "Soft spongy cottage cheese balls, 2 pcs",  "80","80","5","PLATE"},
                {"Phirni",              "Chilled rice pudding with saffron",         "120","60","5","BOWL"},
                {"Kheer",               "Creamy rice pudding, dry fruits",           "110","60","5","BOWL"},
            };

            List<Product> saved = new ArrayList<>();
            for (String[] row : menu) {
                Product p = new Product();
                p.setName(row[0]); p.setDescription(row[1]);
                p.setPrice(new BigDecimal(row[2]));
                p.setQuantity(Integer.parseInt(row[3]));
                p.setGstPercent(new BigDecimal(row[4]));
                p.setUnit(row[5]); p.setActive(true);
                saved.add(products.save(p));
            }
            log.info("✅ Menu: {} items (Starters, Mains Veg/Non-Veg, Breads, Rice, Beverages, Desserts)", saved.size());

            // ════════════════════════════════════════════════════════
            // 4. CUSTOMERS
            // ════════════════════════════════════════════════════════
            List<Customer> custs = new ArrayList<>();
            for (Object[] cd : new Object[][]{
                {"Walk-in Customer",   "0000000000", null,                     null},
                {"Priya Singh",        "9876501234", "priya.singh@email.com",  null},
                {"Rahul Mehta",        "9876502345", "rahul.mehta@email.com",  null},
                {"Anita Desai",        "9876503456", "anita.desai@email.com",  null},
                {"TechCorp Cafeteria", "9876504567", "catering@techcorp.com",  "27AADCS1234B1Z5"},
                {"Suresh Iyer",        "9876505678", null,                     null},
                {"Meera Patel",        "9876506789", "meera.patel@email.com",  null},
                {"Arjun Kapoor",       "9876507890", null,                     null},
            }) {
                Customer c = new Customer();
                c.setName((String) cd[0]); c.setPhone((String) cd[1]);
                c.setEmail((String) cd[2]); c.setGstin((String) cd[3]);
                custs.add(customers.save(c));
            }
            log.info("✅ Customers: 8 records");

            // ════════════════════════════════════════════════════════
            // 5. SAMPLE ORDERS
            //    {daysAgo, custIdx, paymentMethod, note, int[][]{{prodIdx,qty},...}}
            // ════════════════════════════════════════════════════════
            Object[][] orderDefs = {
                {0,  0, "CASH", "Table 3",                 new int[][]{{1,1},{24,2},{16,1},{30,1},{40,1},{33,2}}},
                {0,  1, "UPI",  "Zomato #Z1001",           new int[][]{{0,1},{9,1},{25,2},{36,1},{41,1}}},
                {0,  2, "CASH", "Table 7",                 new int[][]{{17,2},{38,2},{40,2}}},
                {0,  0, "CARD", "Table 1",                 new int[][]{{13,1},{8,1},{24,3},{29,1},{33,1}}},
                {1,  4, "UPI",  "Corporate Lunch",         new int[][]{{17,5},{18,3},{16,4},{29,5},{38,8}}},
                {1,  3, "CASH", "Table 5",                 new int[][]{{10,1},{27,2},{37,1},{43,1}}},
                {1,  0, "UPI",  "Swiggy #SW202",           new int[][]{{6,1},{18,1},{35,2}}},
                {2,  5, "CASH", "Table 2",                 new int[][]{{19,1},{25,3},{30,1},{41,2},{34,1}}},
                {2,  0, "UPI",  "Table 6",                 new int[][]{{7,1},{2,1},{11,1},{25,2},{30,1},{40,1}}},
                {2,  6, "CARD", "Anniversary dinner",      new int[][]{{0,1},{9,1},{20,2},{25,2},{36,2},{43,2}}},
                {3,  7, "CASH", "Table 4",                 new int[][]{{16,1},{8,1},{24,2},{29,1},{33,2}}},
                {3,  0, "UPI",  "Zomato #Z1045",           new int[][]{{18,1},{35,1},{40,1}}},
                {4,  4, "UPI",  "Team Lunch - 12 pax",     new int[][]{{17,6},{16,4},{0,2},{29,6},{38,12},{41,6}}},
                {5,  1, "CASH", "Table 8",                 new int[][]{{5,1},{10,1},{26,2},{29,1},{42,1}}},
                {5,  0, "CARD", "Table 3",                 new int[][]{{4,1},{13,1},{24,2},{38,2}}},
                {7,  2, "UPI",  "Birthday party",          new int[][]{{1,2},{6,1},{16,2},{17,1},{25,4},{36,3},{41,3}}},
                {7,  0, "CASH", "Table 5",                 new int[][]{{23,2},{26,2},{29,2},{33,2}}},
                {10, 3, "UPI",  "Table 2",                 new int[][]{{0,1},{9,1},{24,2},{43,1},{34,1}}},
                {10, 4, "CARD", "Weekly catering",         new int[][]{{17,8},{8,4},{16,4},{29,8},{38,16}}},
                {14, 5, "CASH", "Table 7",                 new int[][]{{19,1},{20,1},{25,3},{36,1},{44,2}}},
                {14, 0, "UPI",  "Swiggy #SW310",           new int[][]{{17,1},{35,1}}},
                {15, 6, "CARD", "Table 4",                 new int[][]{{11,1},{14,1},{27,2},{37,1},{42,1}}},
                {20, 4, "UPI",  "Corporate Lunch - 20 pax",new int[][]{{17,10},{18,5},{16,8},{29,10},{38,20},{40,10}}},
                {20, 7, "CASH", "Table 6",                 new int[][]{{16,1},{9,1},{24,2},{29,1},{33,1}}},
                {21, 1, "UPI",  "Table 3",                 new int[][]{{0,1},{12,1},{25,2},{36,1},{41,1}}},
                {25, 0, "CASH", "Table 1",                 new int[][]{{22,1},{21,1},{29,2},{26,2},{33,2}}},
                {28, 4, "UPI",  "Monthly catering",        new int[][]{{17,15},{8,8},{16,10},{9,5},{29,15},{38,25}}},
                {30, 2, "CARD", "Table 5",                 new int[][]{{19,1},{25,3},{30,1},{44,1},{34,1}}},
            };

            String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            for (Object[] def : orderDefs) {
                int daysAgo = (int) def[0];
                Customer cust = custs.get((int) def[1]);
                Order.PaymentMethod pm = Order.PaymentMethod.valueOf((String) def[2]);
                String note = (String) def[3];
                int[][] items = (int[][]) def[4];

                Order order = new Order();
                order.setCustomer(cust);
                order.setUser(savedAdmin);
                order.setPaymentMethod(pm);
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setNotes(note);
                order.setInvoiceNumber("SG-" + datePrefix + "-" + String.format("%04d", invoiceCounter++));

                List<OrderItem> itemList = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;
                BigDecimal totalGst = BigDecimal.ZERO;

                for (int[] item : items) {
                    Product prod = saved.get(item[0]);
                    int qty = item[1];
                    OrderItem oi = new OrderItem(
                            prod.getId(),
                            prod.getName(),
                            qty,
                            prod.getPrice(),
                            prod.getGstPercent()
                    );
                    oi.setOrder(order);
                    subtotal = subtotal.add(prod.getPrice().multiply(BigDecimal.valueOf(qty)));
                    totalGst = totalGst.add(oi.getGstAmount());
                    itemList.add(oi);
                }

                order.setItems(itemList);
                order.setSubtotal(subtotal);
                order.setTotalGst(totalGst);
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setTotalAmount(subtotal.add(totalGst));
                orders.save(order);
            }
            log.info("✅ Orders: {} sample bills created (last 30 days)", orderDefs.length);
            log.info("🎉 Spice Garden Restaurant ready!  Login → admin / admin123");
        };
    }
}
