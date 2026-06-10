package com.smartinstitute.erp;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class test {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder= new BCryptPasswordEncoder();
        String hash= encoder.encode("123456");
        System.out.println(hash);
    }
}
