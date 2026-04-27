package lk.kishan.carelink.security;

import lk.kishan.carelink.model.Customer;
import lk.kishan.carelink.model.Pharmacy;
import lk.kishan.carelink.repository.CustomerRepository;
import lk.kishan.carelink.repository.PharmacyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PharmacyRepository pharmacyRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer customer = customerRepository.findByEmail(email);

        if (customer != null) {
            return new User(
                    customer.getEmail(),
                    customer.getPassword(),
                    new ArrayList<>()
            );
        }

        Pharmacy pharmacy = pharmacyRepository.findByEmail(email);
        if (pharmacy != null) {
            return new org.springframework.security.core.userdetails.User(
                    pharmacy.getEmail(),
                    pharmacy.getPassword(),
                    List.of(new SimpleGrantedAuthority("ROLE_" + pharmacy.getRole()))
            );
        }

        throw new UsernameNotFoundException("User not found with email: " + email);
    }
}