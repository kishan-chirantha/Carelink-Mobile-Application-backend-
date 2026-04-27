package lk.kishan.carelink.service;

import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String registerCustomer(Customer customer) {
        customer.setPassword(passwordEncoder.encode(customer.getPassword()));

        customerRepository.save(customer);
        return "Customer Registered Successfully!";
    }

    public Customer getCustomerByEmail(String email) {
        return customerRepository.findByEmail(email);
    }


}