package com.smartinstitute.erp.user.service;

import com.smartinstitute.erp.exception.UserNotFoundException;
import com.smartinstitute.erp.exception.UserNotLinkedException;
import com.smartinstitute.erp.user.dto.UserRequestDto;
import com.smartinstitute.erp.user.dto.UserResponseDto;
import com.smartinstitute.erp.user.entity.User;
import com.smartinstitute.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserResponseDto createUser(UserRequestDto request) {

        User user = new User();
        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw  new RuntimeException("Email already exits ");
        }
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());

        userRepository.save(user);

        return mapToDto(user);
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToDto(user);
    }

    public UserResponseDto updateUser(long id, UserRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setStatus(request.getStatus());

        userRepository.save(user);

        return mapToDto(user);
    }

    public void deleteUser(long id) {
        userRepository.deleteById(id);
    }

    private UserResponseDto mapToDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        return dto;
    }}
