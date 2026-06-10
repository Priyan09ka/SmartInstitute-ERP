package com.smartinstitute.erp.user.controller;

import com.smartinstitute.erp.user.dto.UserRequestDto;
import com.smartinstitute.erp.user.dto.UserResponseDto;
import com.smartinstitute.erp.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserResponseDto createUser(@RequestBody UserRequestDto request){
        return userService.createUser(request);
    }

    @GetMapping
    public List<UserResponseDto> getAllUsers(){
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDto getUserById(@PathVariable long id){
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public UserResponseDto updateUser(@PathVariable long id,
                                      @RequestBody UserRequestDto request){
        return userService.updateUser(id,request);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable long id){
        userService.deleteUser(id);
        return "User deleted successfully!!";
    }
}
