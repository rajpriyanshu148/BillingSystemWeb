package com.billing.repository;

import com.billing.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByNameAsc();

    List<Customer> findByNameContainingIgnoreCase(String keyword);

    @Query("SELECT COUNT(c) FROM Customer c")
    long countAll();
}
