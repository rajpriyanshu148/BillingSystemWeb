package com.billing.controller;

import com.billing.model.BusinessProfile;
import com.billing.model.User;
import com.billing.repository.BusinessProfileRepository;
import com.billing.repository.CustomerRepository;
import com.billing.repository.UserRepository;
import com.billing.model.Customer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
public class RegistrationController {

    private final BusinessProfileRepository businessRepo;
    private final UserRepository userRepo;
    private final CustomerRepository customerRepo;
    private final PasswordEncoder encoder;

    public RegistrationController(BusinessProfileRepository businessRepo,
                                  UserRepository userRepo,
                                  CustomerRepository customerRepo,
                                  PasswordEncoder encoder) {
        this.businessRepo = businessRepo;
        this.userRepo = userRepo;
        this.customerRepo = customerRepo;
        this.encoder = encoder;
    }

    /** Show registration page — only if no business registered yet */
    @GetMapping("/register")
    public String registerPage() {
        if (businessRepo.existsByIdNotNull()) {
            return "redirect:/login";   // already registered
        }
        return "forward:/register.html";
    }

    /** Handle registration form submission */
    @PostMapping("/register")
    public String doRegister(
            // Business fields
            @RequestParam String businessName,
            @RequestParam String businessType,
            @RequestParam(required = false) String ownerName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String addressLine1,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String pincode,
            @RequestParam(required = false) String gstin,
            @RequestParam(defaultValue = "INV") String invoicePrefix,
            // Admin account fields
            @RequestParam String adminUsername,
            @RequestParam String adminPassword,
            @RequestParam String adminFullName,
            RedirectAttributes ra) {

        // Guard: already registered
        if (businessRepo.existsByIdNotNull()) {
            return "redirect:/login";
        }

        // Validate
        if (businessName == null || businessName.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Business name is required.");
            return "redirect:/register";
        }
        if (adminUsername == null || adminUsername.trim().length() < 3) {
            ra.addFlashAttribute("error", "Admin username must be at least 3 characters.");
            return "redirect:/register";
        }
        if (adminPassword == null || adminPassword.length() < 6) {
            ra.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/register";
        }
        if (userRepo.existsByUsername(adminUsername.trim())) {
            ra.addFlashAttribute("error", "Username already taken.");
            return "redirect:/register";
        }

        // 1. Save business profile
        BusinessProfile biz = new BusinessProfile();
        biz.setBusinessName(businessName.trim());
        biz.setBusinessType(businessType);
        biz.setOwnerName(ownerName);
        biz.setPhone(phone);
        biz.setEmail(email);
        biz.setWebsite(website);
        biz.setAddressLine1(addressLine1);
        biz.setCity(city);
        biz.setState(state);
        biz.setPincode(pincode);
        biz.setGstin(gstin);
        biz.setInvoicePrefix(invoicePrefix != null && !invoicePrefix.isBlank() ? invoicePrefix : "INV");
        biz.setRegisteredAt(LocalDateTime.now());
        businessRepo.save(biz);

        // 2. Create admin user
        User admin = new User();
        admin.setUsername(adminUsername.trim());
        admin.setPassword(encoder.encode(adminPassword));
        admin.setFullName(adminFullName != null && !adminFullName.isBlank() ? adminFullName : adminUsername);
        admin.setRole(User.Role.ADMIN);
        admin.setActive(true);
        userRepo.save(admin);

        // 3. Create Walk-in Customer
        Customer walkIn = new Customer();
        walkIn.setName("Walk-in Customer");
        walkIn.setPhone("0000000000");
        customerRepo.save(walkIn);

        ra.addFlashAttribute("registered", true);
        return "redirect:/login?registered=true";
    }
}
