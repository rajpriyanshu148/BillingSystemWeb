package com.billing.config;

import com.billing.model.Customer;
import com.billing.model.Product;
import com.billing.model.User;
import com.billing.repository.CustomerRepository;
import com.billing.repository.ProductRepository;
import com.billing.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedData(UserRepository users,
                                     ProductRepository products,
                                     CustomerRepository customers,
                                     PasswordEncoder encoder) {
        return args -> {
            // ── Users ───────────────────────────────────────────
            if (users.count() == 0) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(encoder.encode("admin123"));
                admin.setRole(User.Role.ADMIN);
                admin.setFullName("System Administrator");
                admin.setActive(true);
                users.save(admin);

                User staff = new User();
                staff.setUsername("staff");
                staff.setPassword(encoder.encode("staff123"));
                staff.setRole(User.Role.STAFF);
                staff.setFullName("Staff User");
                staff.setActive(true);
                users.save(staff);

                log.info("✅ Default users created (admin / staff)");
            }

            // ── Walk-in Customer ────────────────────────────────
            if (customers.count() == 0) {
                Customer walkIn = new Customer();
                walkIn.setName("Walk-in Customer");
                walkIn.setPhone("0000000000");
                walkIn.setEmail("");
                walkIn.setAddress("");
                customers.save(walkIn);
                log.info("✅ Walk-in customer created");
            }

            // ── Sample Products ─────────────────────────────────
            if (products.count() == 0) {
                String[][] data = {
                    {"Laptop Dell Inspiron",  "Intel i5, 8GB RAM, 512GB SSD", "55000", "25", "18", "PCS"},
                    {"Wireless Mouse",         "Logitech M185, USB Nano",        "650",  "100", "18", "PCS"},
                    {"USB Keyboard",           "Mechanical, Backlit",           "1200",   "75", "18", "PCS"},
                    {"HDMI Cable 2m",          "Gold Plated, 4K Support",        "350",  "200", "18", "PCS"},
                    {"Printer Paper A4",       "75 GSM, 500 Sheets/Ream",        "280",  "500", "12", "REAM"},
                    {"Pen Blue (Box)",          "Ball Point, 10 pens",             "45",  "300",  "5", "BOX"},
                    {"Stapler",                "Full Strip, 26/6",               "120",  "150", "12", "PCS"},
                    {"File Folder",            "Polypropylene, A4",               "25",  "400",  "5", "PCS"},
                };
                for (String[] row : data) {
                    Product p = new Product();
                    p.setName(row[0]);
                    p.setDescription(row[1]);
                    p.setPrice(new BigDecimal(row[2]));
                    p.setQuantity(Integer.parseInt(row[3]));
                    p.setGstPercent(new BigDecimal(row[4]));
                    p.setUnit(row[5]);
                    p.setActive(true);
                    products.save(p);
                }
                log.info("✅ {} sample products created", data.length);
            }
        };
    }
}
