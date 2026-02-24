package com.openclassrooms.etudiant.controller;

import com.openclassrooms.etudiant.dto.AuthResponseDTO;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;
import com.openclassrooms.etudiant.service.UserService;
import com.openclassrooms.etudiant.service.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserDtoMapper userDtoMapper;
    private final JwtService jwtService;

    @PostMapping("/api/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        String jwtToken = userService.login(loginRequestDTO.getLogin(), loginRequestDTO.getPassword());
        long expiresIn = jwtService.getExpiration();
        AuthResponseDTO respBody = new AuthResponseDTO(jwtToken, expiresIn);

        return ResponseEntity.ok(respBody);
    }

    //= CREATE
    @PostMapping("/api/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterDTO registerDTO) {
        userService.register(userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    //= READ - get all students
    @GetMapping("/api/students")
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        List<StudentDTO> students = userService.getAllStudents()
                .stream()
                .map(userDtoMapper::toStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }
    //= READ - get student by id
    @GetMapping("/api/students/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        return userService.getStudentById(id)
                .map(userDtoMapper::toStudentDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    //= UPDATE - update student by id
    @PutMapping("/api/students/{id}")
    public ResponseEntity<?> updateStudentById(@PathVariable Long id, @Valid @RequestBody RegisterDTO registerDTO) {
        userService.updateStudentById(id, userDtoMapper.toEntity(registerDTO));
        return new ResponseEntity<>(HttpStatus.OK);
    }
    //= DELETE - delete student by id
    @DeleteMapping("/api/students/{id}")
    public ResponseEntity<?> deleteStudentById(@PathVariable Long id) {
        userService.deleteStudentById(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
