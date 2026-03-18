package com.openclassrooms.etudiant.service;

import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import com.openclassrooms.testutils.UserTestBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import com.openclassrooms.etudiant.dto.UpdateStudentDTO;
import com.openclassrooms.etudiant.mapper.UserDtoMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class UserServiceTest {
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "LOGIN";
    private static final String PASSWORD = "PASSWORD";
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserDtoMapper mapper;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private UserService userService;

    // 0.1.a - test de la méthode register avec un utilisateur null
    @Test
    public void test_create_null_user_throws_IllegalArgumentException() {
        // GIVEN

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(null));
    }

    // 0.1.b - test de la méthode register avec un utilisateur déjà existant
    @Test
    public void test_create_already_exist_user_throws_IllegalArgumentException() {
        // GIVEN
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .withPassword(PASSWORD)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME)
            .build();
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByLogin(any())).thenReturn(Optional.of(user));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(user));
    }

    // 0.1.c - test de la méthode register avec un utilisateur valide
    @Test
    public void test_create_user() {
        // GIVEN
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .withPassword(PASSWORD)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME)
            .build();
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByLogin(any())).thenReturn(Optional.empty());

        // WHEN
        userService.register(user);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo(user);
    }

    // 1.1.a - test de la méthode login avec un login et password valides
    @Test
    public void test_login_with_valid_login_and_password() {
        // GIVEN
        // Créer un utilisateur valide
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .withPassword(PASSWORD)
            .build();
        // Créer des credentials utilisateur valides (à la place d'un LoginRequestDTO)
        String loginAttempted = LOGIN;
        String passwordAttempted = PASSWORD;
        // Créer un token attendu
        String expectedToken = "expectedToken";
        // Configurer les mocks
        when(userRepository.findByLogin(loginAttempted)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(passwordAttempted, user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn(expectedToken);

        // WHEN
        String token = userService.login(loginAttempted, passwordAttempted);

        // THEN
        // vérifier que le token retourné est correct
        assertThat(token).isEqualTo(expectedToken);
        // vérifier que passwordEncoder.matches a été appelé avec le password envoyé et le password de l'utilisateur
        verify(passwordEncoder).matches(passwordAttempted, user.getPassword());
        // vérifier que jwtService.generateToken a été appelé avec un UserDetails valide
        verify(jwtService).generateToken(any(UserDetails.class));
    }

    // 1.2.a - test de la méthode createStudent avec un utilisateur valide
    @Test
    public void test_create_user_by_admin() {
        // GIVEN
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .withNoPassword()
            .build();
        String defaultPassword = "password123";
        String encodedPassword = "encoded-password";
        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(defaultPassword)).thenReturn(encodedPassword);

        // WHEN
        userService.createStudent(user);

        // THEN
        verify(userRepository).findByLogin(LOGIN);
        verify(passwordEncoder).encode(defaultPassword);
        assertThat(user.getPassword()).isEqualTo(encodedPassword);
        verify(userRepository).save(user);
    }

    // 1.3.a - test de la méthode getAllStudents avec un utilisateur valide
    @Test
    public void test_get_all_students() {
        // GIVEN
        User user1 = UserTestBuilder.aUser()
            .withLogin(LOGIN + "1")
            .withPassword(PASSWORD + "1")
            .withFirstName(FIRST_NAME + "1")
            .withLastName(LAST_NAME + "1")
            .build();
        User user2 = UserTestBuilder.aUser()
            .withLogin(LOGIN + "2")
            .withPassword(PASSWORD + "2")
            .withFirstName(FIRST_NAME + "2")
            .withLastName(LAST_NAME + "2")
            .build();
        List<User> users = List.of(user1, user2);
        when(userRepository.findAll()).thenReturn(users);

        // WHEN
        List<User> retrievedUsers = userService.getAllStudents();

        // THEN
        assertThat(retrievedUsers).isEqualTo(users);
        verify(userRepository).findAll();
    }

    // 1.4.a - test de la méthode getStudentById avec un utilisateur valide
    @Test
    public void test_get_student_by_id() {
        // GIVEN
        User user = UserTestBuilder.aUser()
            .withId(1L)
            .withLogin(LOGIN)
            .withPassword(PASSWORD)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME)
            .build();
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));

        // WHEN
        Optional<User> retrievedUser = userService.getStudentById(user.getId());

        // THEN
        assertThat(retrievedUser).contains(user);
        verify(userRepository).findById(user.getId());
    }

    // 1.5.a - test de la méthode updateStudentById avec un utilisateur valide
    @Test
    public void test_update_student_by_id() {
        // GIVEN
        Long id = 1L;
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .build();

        UpdateStudentDTO dto = new UpdateStudentDTO();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        // WHEN
        userService.updateStudentById(id, dto, mapper);

        // THEN
        verify(userRepository).findById(id);
        verify(mapper).updateFromDto(dto, user);
        verify(userRepository).save(user);
    }

    // 1.6.a - test de la méthode deleteStudentById avec un utilisateur valide
    @Test
    public void test_delete_student_by_id() {
        // GIVEN
        Long id = 1L;
        when(userRepository.existsById(id)).thenReturn(true);

        // WHEN
        userService.deleteStudentById(id);

        // THEN
        verify(userRepository).existsById(id);
        verify(userRepository).deleteById(id);
    }
}
