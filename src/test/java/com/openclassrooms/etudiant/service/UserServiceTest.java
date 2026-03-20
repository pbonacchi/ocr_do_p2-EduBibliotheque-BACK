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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    // 0.1.a - test de la méthode register avec un utilisateur valide
    /* Vérifie que register enregistre un utilisateur valide.
     * Entrants : user valide + findByLogin -> Optional.empty 
     * Sortants : le user enregistré dans la base de données est le même que celui passé en paramètre.
     */
    @Test
    public void test_create_user() {
        // GIVEN
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .withPassword(PASSWORD)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME)
            .build();
        //when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD);
        when(userRepository.findByLogin(any())).thenReturn(Optional.empty());

        // WHEN
        userService.register(user);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo(user);
    }

    // 0.1.b - test de la méthode register avec un utilisateur null
    /* Vérifie que register rejette une entrée null.
     * Entrants : user = null 
     * Sortants : une exception IllegalArgumentException ("User must not be null").
     */
    @Test
    public void test_create_null_user_throws_IllegalArgumentException() {
        // GIVEN

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(null));
    }

    // 0.1.c - test de la méthode register avec un utilisateur déjà existant
    /* Vérifie que register rejette un login déjà existant.
     * Entrants : user valide + findByLogin -> Optional.of(user) 
     * Sortants : une exception IllegalArgumentException.
     */
    @Test
    public void test_create_already_exist_user_throws_IllegalArgumentException() {
        // GIVEN
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .withPassword(PASSWORD)
            .withFirstName(FIRST_NAME)
            .withLastName(LAST_NAME)
            .build();
        when(userRepository.findByLogin(any())).thenReturn(Optional.of(user));

        // THEN
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> userService.register(user));
    }

    // 1.1.a - test de la méthode login avec un login et password valides
    /* Vérifie que login retourne un token valide.
     * Entrants : login et password valides
     *            findByLogin -> Optional.of(user)
     *            passwordEncoder.matches -> true avec le password envoyé et le password de l'utilisateur
     *            jwtService.generateToken -> "expectedToken" avec un UserDetails valide
     * Sortants : le token retourné est correct
     *            passwordEncoder.matches a été appelé avec le password envoyé et le password de l'utilisateur
     *            jwtService.generateToken a été appelé avec un UserDetails valide
     */
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

    // 1.1.b - test de la méthode login avec un login null
    /* Vérifie que login rejette un login null.
     * Entrants : login = null, password valide
     * Sortants : une exception IllegalArgumentException ("Login must not be null").
     */
    @Test
    public void test_login_with_null_login_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> userService.login(null, PASSWORD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Login must not be null");
    }

    // 1.1.c - test de la méthode login avec un password null
    /* Vérifie que login rejette un password null.
     * Entrants : login valide, password = null
     * Sortants : une exception IllegalArgumentException ("Password must not be null").
     */
    @Test
    public void test_login_with_null_password_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> userService.login(LOGIN, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Password must not be null");
    }

    // 1.1.d - test de la méthode login avec un login invalide
    /* Vérifie que login rejette un login invalide.
     * Entrants : findByLogin -> Optional.empty
     * Sortants : une exception IllegalArgumentException ("Invalid credentials").
     */
    @Test
    public void test_login_with_invalid_login_throws_IllegalArgumentException() {
        // GIVEN
        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());
        // THEN
        assertThatThrownBy(() -> userService.login(LOGIN, PASSWORD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid credentials");
    }

    // 1.1.e - test de la méthode login avec un password invalide
    /* Vérifie que login rejette un password invalide.
     * Entrants : findByLogin -> Optional.of(user)
     *            passwordEncoder.matches -> false avec le password envoyé et le password de l'utilisateur
     * Sortants : une exception IllegalArgumentException ("Invalid credentials").
     */
    @Test
    public void test_login_with_invalid_password_throws_IllegalArgumentException() {
        // GIVEN
        User user = UserTestBuilder.aUser()
            .withLogin(LOGIN)
            .withPassword(PASSWORD)
            .build();
        String invalidPassword = "invalid password";
        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(invalidPassword, user.getPassword())).thenReturn(false);

        // THEN
        assertThatThrownBy(() -> userService.login(LOGIN, invalidPassword))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid credentials");
    }

    // 1.2.a - test de la méthode createStudent avec un utilisateur valide
    /* Vérifie que createStudent enregistre un utilisateur valide.
     * Entrants : user valide + findByLogin -> Optional.empty
     *            passwordEncoder.encode -> "encoded-password" avec le password par défaut
     * Sortants : le user enregistré dans la base de données est le même que celui passé en paramètre
     *            passwordEncoder.encode a été appelé avec le password par défaut
     *            userRepository.save a été appelé avec le user
     */
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

    // 1.2.b - test de la méthode createStudent avec un utilisateur null
    /* Vérifie que createStudent rejette un utilisateur null.
     * Entrants : user = null
     * Sortants : une exception IllegalArgumentException ("User must not be null").
     */
    @Test
    public void test_create_user_by_admin_with_null_user_throws_IllegalArgumentException() {
        assertThatThrownBy(() -> userService.createStudent(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User must not be null");
    }

    // 1.2.c - test de la méthode createStudent avec un utilisateur déjà existant
    /* Vérifie que createStudent rejette un login déjà existant.
     * Entrants : user valide + findByLogin -> Optional.of(user)
     * Sortants : une exception IllegalArgumentException ("User with login XXX already exists").
     */
    @Test
    public void test_create_user_by_admin_with_already_existing_user_throws_IllegalArgumentException() {
        // GIVEN
        User user = UserTestBuilder.aUser().withLogin(LOGIN).build();
        when(userRepository.findByLogin(LOGIN)).thenReturn(Optional.of(user));
        // THEN
        assertThatThrownBy(() -> userService.createStudent(user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User with login " + user.getLogin() + " already exists");
    }

    // 1.3.a - test de la méthode getAllStudents
    /* Vérifie que getAllStudents retourne tous les utilisateurs.
     * Entrants : findAll -> List<User> non vide
     * Sortants : la liste des utilisateurs retournée est correcte
     *            findAll a été appelé
     */
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

    // 1.4.a - test de la méthode getStudentById avec un utilisateur existant
    /* Vérifie que getStudentById retourne un utilisateur existant.
     * Entrants : findById -> Optional.of(user)
     * Sortants : l'utilisateur retourné est le même que celui passé en paramètre
     *            findById a été appelé avec l'id de l'utilisateur
     */
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

    // 1.4.b - test de la méthode getStudentById avec un utilisateur non existant
    /* Vérifie que getStudentById rejette un utilisateur non existant.
     * Entrants : findById -> Optional.empty
     * Sortants : une exception IllegalArgumentException ("User with id XXX does not exist").
     */
    @Test
    public void test_get_student_by_id_with_non_existing_user_throws_IllegalArgumentException() {
        // GIVEN
        Long id = 1L;
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        // THEN
        assertThatThrownBy(() -> userService.getStudentById(id))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User with id " + id + " does not exist");
    }

    // 1.5.a - test de la méthode updateStudentById avec un utilisateur existant
    /* Vérifie que updateStudentById met à jour un utilisateur existant.
     * Entrants : findById -> Optional.of(user)
     *            updateFromDto -> user avec les nouvelles données
     * Sortants : le user enregistré dans la base de données est le même que celui passé en paramètre
     *            findById a été appelé avec l'id de l'utilisateur
     *            updateFromDto a été appelé avec le DTO et le user
     *            userRepository.save a été appelé avec le user
     */
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

    // 1.5.b - test de la méthode updateStudentById avec un DTO null
    /* Vérifie que updateStudentById rejette un DTO null.
     * Entrants : id, dto = null
     * Sortants : une exception IllegalArgumentException ("Update data must not be null").
     */
    @Test
    public void test_update_student_by_id_with_null_dto_throws_IllegalArgumentException() {
        // GIVEN
        Long id = 1L;
        UpdateStudentDTO dto = null;

        // THEN
        assertThatThrownBy(() -> userService.updateStudentById(id, dto, mapper))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Update data must not be null");
    }

    // 1.5.c - test de la méthode updateStudentById avec un utilisateur non existant
    /* Vérifie que updateStudentById rejette un utilisateur non existant.
     * Entrants : findById -> Optional.empty
     * Sortants : une exception IllegalArgumentException ("User with id XXX does not exist").
     */
    @Test
    public void test_update_student_by_id_with_non_existing_user_throws_IllegalArgumentException() {
        // GIVEN
        Long id = 1L;
        UpdateStudentDTO dto = new UpdateStudentDTO();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // THEN
        assertThatThrownBy(() -> userService.updateStudentById(id, dto, mapper))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User with id " + id + " does not exist");
    }

    // 1.6.a - test de la méthode deleteStudentById avec un utilisateur valide
    /* Vérifie que deleteStudentById supprime un utilisateur existant.
     * Entrants : existsById -> true
     * Sortants : existsById a été appelé avec l'id de l'utilisateur
     *            deleteById a été appelé avec l'id de l'utilisateur
     */
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

    // 1.6.b - test de la méthode deleteStudentById avec un utilisateur non existant
    /* Vérifie que deleteStudentById rejette un utilisateur non existant.
     * Entrants : existsById -> false
     * Sortants : une exception IllegalArgumentException ("User with id XXX does not exist").
     */
    @Test
    public void test_delete_student_by_id_with_non_existing_user_throws_IllegalArgumentException() {
        // GIVEN
        Long id = 1L;
        when(userRepository.existsById(id)).thenReturn(false);

        // THEN
        assertThatThrownBy(() -> userService.deleteStudentById(id))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User with id " + id + " does not exist");
    }
}
