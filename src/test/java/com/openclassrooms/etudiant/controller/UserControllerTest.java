package com.openclassrooms.etudiant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openclassrooms.etudiant.dto.LoginRequestDTO;
import com.openclassrooms.etudiant.dto.RegisterDTO;
import com.openclassrooms.etudiant.dto.StudentDTO;
import com.openclassrooms.etudiant.dto.UpdateStudentDTO;
import com.openclassrooms.etudiant.entities.User;
import com.openclassrooms.etudiant.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Import(TestMapperConfig.class)
public class UserControllerTest {

    private static final String REGISTER_URL = "/api/register";
    private static final String LOGIN_URL = "/api/login";
    private static final String STUDENTS_URL = "/api/students";
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";
    private static final String LOGIN = "login";
    private static final String PASSWORD = "password";
    private static final String TEST_JWT_SECRET_BASE64 = "VGhpc0lzQVRlc3RTZWNyZXRLZXlGb3JIVDI1NlRoaXNJc0xvbmdFbm91Z2hUb0JlVmFsaWQ=";


    @Container
    public static MySQLContainer mySQLContainer = new MySQLContainer("mysql:latest");

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mySQLContainer.getJdbcUrl());
        registry.add("spring.datasource.username", () -> mySQLContainer.getUsername());
        registry.add("spring.datasource.password", () -> mySQLContainer.getPassword());
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("jwt.secret", () -> TEST_JWT_SECRET_BASE64);

    }

    @AfterEach
    public void afterEach() {
        userRepository.deleteAll();
    }

    @Test
    public void registerUserWithoutRequiredData() throws Exception {
        // Vérifie que POST /api/register rejette une inscription invalide.
        // Entrants : JSON vide (champs obligatoires manquants).
        // Sortants : HTTP 400 Bad Request + aucun enregistrement en base.
        // GIVEN
        RegisterDTO registerDTO = new RegisterDTO();

        // WHEN
        mockMvc.perform(post(REGISTER_URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    public void registerAlreadyExistUser() throws Exception {
        // Vérifie que POST /api/register rejette un login déjà existant.
        // Entrants : un utilisateur existant en base + même login dans la requête.
        // Sortants : HTTP 400 Bad Request + un seul utilisateur conservé en base.
        // GIVEN
        userRepository.save(buildPersistedUser(FIRST_NAME, LAST_NAME, LOGIN, PASSWORD));

        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setFirstName(FIRST_NAME);
        registerDTO.setLastName(LAST_NAME);
        registerDTO.setLogin(LOGIN);
        registerDTO.setPassword(PASSWORD);

        // WHEN
        mockMvc.perform(post(REGISTER_URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isEqualTo(1L);
    }

    @Test
    public void registerUserSuccessful() throws Exception {
        // Vérifie que POST /api/register crée un nouvel étudiant.
        // Entrants : RegisterDTO valide avec login inédit.
        // Sortants : HTTP 201 Created + utilisateur persistant en base.
        // GIVEN
        RegisterDTO registerDTO = new RegisterDTO();
        registerDTO.setFirstName(FIRST_NAME);
        registerDTO.setLastName(LAST_NAME);
        registerDTO.setLogin(LOGIN);
        registerDTO.setPassword(PASSWORD);

        // WHEN
        mockMvc.perform(post(REGISTER_URL)
                        .content(objectMapper.writeValueAsString(registerDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated());

        User createdUser = userRepository.findByLogin(LOGIN).orElseThrow();
        assertThat(createdUser.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(createdUser.getLastName()).isEqualTo(LAST_NAME);
        assertThat(passwordEncoder.matches(PASSWORD, createdUser.getPassword())).isTrue();
    }

    @Test
    public void loginSuccessful() throws Exception {
        // Vérifie que POST /api/login authentifie un utilisateur existant.
        // Entrants : login/password valides d'un utilisateur déjà présent en base.
        // Sortants : HTTP 200 OK + réponse contenant idToken et expiresIn.
        // GIVEN
        userRepository.save(buildPersistedUser(FIRST_NAME, LAST_NAME, LOGIN, PASSWORD));
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setLogin(LOGIN);
        loginRequestDTO.setPassword(PASSWORD);

        // WHEN / THEN
        mockMvc.perform(post(LOGIN_URL)
                        .content(objectMapper.writeValueAsString(loginRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idToken").isString())
                .andExpect(jsonPath("$.idToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    public void loginWithInvalidPassword() throws Exception {
        // Vérifie que POST /api/login rejette un mot de passe invalide.
        // Entrants : login valide + password incorrect.
        // Sortants : HTTP 400 Bad Request.
        // GIVEN
        userRepository.save(buildPersistedUser(FIRST_NAME, LAST_NAME, LOGIN, PASSWORD));
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();
        loginRequestDTO.setLogin(LOGIN);
        loginRequestDTO.setPassword("wrong-password");

        // WHEN / THEN
        mockMvc.perform(post(LOGIN_URL)
                        .content(objectMapper.writeValueAsString(loginRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void loginWithMissingRequiredData() throws Exception {
        // Vérifie que POST /api/login rejette une requête invalide.
        // Entrants : LoginRequestDTO incomplet (login/password manquants).
        // Sortants : HTTP 400 Bad Request.
        // GIVEN
        LoginRequestDTO loginRequestDTO = new LoginRequestDTO();

        // WHEN / THEN
        mockMvc.perform(post(LOGIN_URL)
                        .content(objectMapper.writeValueAsString(loginRequestDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin")
    public void createStudentSuccessful() throws Exception {
        // Vérifie que POST /api/students crée un étudiant via un endpoint protégé.
        // Entrants : utilisateur authentifié + StudentDTO valide.
        // Sortants : HTTP 201 Created + étudiant retourné et persistant en base.
        // GIVEN
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setFirstName("Alice");
        studentDTO.setLastName("Martin");
        studentDTO.setLogin("alice.martin");

        // WHEN / THEN
        mockMvc.perform(post(STUDENTS_URL)
                        .content(objectMapper.writeValueAsString(studentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Alice"))
                .andExpect(jsonPath("$.lastName").value("Martin"))
                .andExpect(jsonPath("$.login").value("alice.martin"));

        User createdUser = userRepository.findByLogin("alice.martin").orElseThrow();
        assertThat(createdUser.getFirstName()).isEqualTo("Alice");
        assertThat(createdUser.getLastName()).isEqualTo("Martin");
    }

    @Test
    public void createStudentWithoutAuthentication() throws Exception {
        // Vérifie que POST /api/students est inaccessible sans authentification.
        // Entrants : StudentDTO valide mais requête non authentifiée.
        // Sortants : HTTP 401 Unauthorized + aucun étudiant créé.
        // GIVEN
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setFirstName("No");
        studentDTO.setLastName("Auth");
        studentDTO.setLogin("no.auth");

        // WHEN / THEN
        mockMvc.perform(post(STUDENTS_URL)
                        .content(objectMapper.writeValueAsString(studentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        assertThat(userRepository.findByLogin("no.auth")).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin")
    public void createStudentWithAlreadyExistingLogin() throws Exception {
        // Vérifie que POST /api/students rejette un login déjà existant.
        // Entrants : utilisateur authentifié + StudentDTO avec login déjà en base.
        // Sortants : HTTP 400 Bad Request + un seul enregistrement conservé.
        // GIVEN
        userRepository.save(buildPersistedUser("Existing", "User", "existing.login", PASSWORD));
        StudentDTO studentDTO = new StudentDTO();
        studentDTO.setFirstName("New");
        studentDTO.setLastName("User");
        studentDTO.setLogin("existing.login");

        // WHEN / THEN
        mockMvc.perform(post(STUDENTS_URL)
                        .content(objectMapper.writeValueAsString(studentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isEqualTo(1L);
    }

    @Test
    @WithMockUser(username = "admin")
    public void getAllStudentsSuccessful() throws Exception {
        // Vérifie que GET /api/students retourne la liste des étudiants.
        // Entrants : 2 étudiants déjà présents en base + utilisateur authentifié.
        // Sortants : HTTP 200 OK + tableau JSON de taille 2.
        // GIVEN
        userRepository.saveAll(List.of(
                buildPersistedUser("Ada", "Lovelace", "ada", PASSWORD),
                buildPersistedUser("Alan", "Turing", "alan", PASSWORD)
        ));

        // WHEN / THEN
        mockMvc.perform(get(STUDENTS_URL).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    public void getAllStudentsWithoutAuthentication() throws Exception {
        // Vérifie que GET /api/students est inaccessible sans authentification.
        // Entrants : requête non authentifiée.
        // Sortants : HTTP 401 Unauthorized.
        // WHEN / THEN
        mockMvc.perform(get(STUDENTS_URL).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin")
    public void getStudentByIdSuccessful() throws Exception {
        // Vérifie que GET /api/students/{id} retourne un étudiant existant.
        // Entrants : id d'un étudiant présent en base + utilisateur authentifié.
        // Sortants : HTTP 200 OK + JSON de l'étudiant correspondant.
        // GIVEN
        User user = userRepository.save(buildPersistedUser("Grace", "Hopper", "grace", PASSWORD));

        // WHEN / THEN
        mockMvc.perform(get(STUDENTS_URL + "/" + user.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.firstName").value("Grace"))
                .andExpect(jsonPath("$.lastName").value("Hopper"))
                .andExpect(jsonPath("$.login").value("grace"));
    }

    @Test
    @WithMockUser(username = "admin")
    public void getStudentByIdWithNonExistingId() throws Exception {
        // Vérifie que GET /api/students/{id} rejette un id inexistant.
        // Entrants : id absent en base + utilisateur authentifié.
        // Sortants : HTTP 400 Bad Request.
        // WHEN / THEN
        mockMvc.perform(get(STUDENTS_URL + "/999999").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void getStudentByIdWithoutAuthentication() throws Exception {
        // Vérifie que GET /api/students/{id} est inaccessible sans authentification.
        // Entrants : id quelconque mais requête non authentifiée.
        // Sortants : HTTP 401 Unauthorized.
        // WHEN / THEN
        mockMvc.perform(get(STUDENTS_URL + "/1").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin")
    public void updateStudentByIdSuccessful() throws Exception {
        // Vérifie que PUT /api/students/{id} met à jour un étudiant existant.
        // Entrants : id existant + UpdateStudentDTO valide + utilisateur authentifié.
        // Sortants : HTTP 200 OK + données mises à jour en base.
        // GIVEN
        User user = userRepository.save(buildPersistedUser("Marie", "Curie", "marie", PASSWORD));
        UpdateStudentDTO updateStudentDTO = new UpdateStudentDTO("Marie-Updated", "Curie-Updated", "marie.updated");

        // WHEN / THEN
        mockMvc.perform(put(STUDENTS_URL + "/" + user.getId())
                        .content(objectMapper.writeValueAsString(updateStudentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getFirstName()).isEqualTo("Marie-Updated");
        assertThat(updatedUser.getLastName()).isEqualTo("Curie-Updated");
        assertThat(updatedUser.getLogin()).isEqualTo("marie.updated");
    }

    @Test
    @WithMockUser(username = "admin")
    public void updateStudentByIdWithInvalidPayload() throws Exception {
        // Vérifie que PUT /api/students/{id} rejette un DTO invalide.
        // Entrants : id existant + UpdateStudentDTO sans champs obligatoires.
        // Sortants : HTTP 400 Bad Request + données en base inchangées.
        // GIVEN
        User user = userRepository.save(buildPersistedUser("Initial", "User", "initial.login", PASSWORD));
        UpdateStudentDTO invalidUpdateStudentDTO = new UpdateStudentDTO();

        // WHEN / THEN
        mockMvc.perform(put(STUDENTS_URL + "/" + user.getId())
                        .content(objectMapper.writeValueAsString(invalidUpdateStudentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());

        User unchangedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unchangedUser.getFirstName()).isEqualTo("Initial");
        assertThat(unchangedUser.getLastName()).isEqualTo("User");
        assertThat(unchangedUser.getLogin()).isEqualTo("initial.login");
    }

    @Test
    @WithMockUser(username = "admin")
    public void updateStudentByIdWithNonExistingId() throws Exception {
        // Vérifie que PUT /api/students/{id} rejette un id inexistant.
        // Entrants : id absent en base + UpdateStudentDTO valide.
        // Sortants : HTTP 400 Bad Request.
        // GIVEN
        UpdateStudentDTO updateStudentDTO = new UpdateStudentDTO("Name", "Surname", "name.surname");

        // WHEN / THEN
        mockMvc.perform(put(STUDENTS_URL + "/999999")
                        .content(objectMapper.writeValueAsString(updateStudentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateStudentByIdWithoutAuthentication() throws Exception {
        // Vérifie que PUT /api/students/{id} est inaccessible sans authentification.
        // Entrants : id + UpdateStudentDTO valide mais requête non authentifiée.
        // Sortants : HTTP 401 Unauthorized.
        // GIVEN
        UpdateStudentDTO updateStudentDTO = new UpdateStudentDTO("Name", "Surname", "name.surname");

        // WHEN / THEN
        mockMvc.perform(put(STUDENTS_URL + "/1")
                        .content(objectMapper.writeValueAsString(updateStudentDTO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin")
    public void deleteStudentByIdSuccessful() throws Exception {
        // Vérifie que DELETE /api/students/{id} supprime un étudiant existant.
        // Entrants : id existant + utilisateur authentifié.
        // Sortants : HTTP 204 No Content + étudiant supprimé de la base.
        // GIVEN
        User user = userRepository.save(buildPersistedUser("Linus", "Torvalds", "linus", PASSWORD));

        // WHEN / THEN
        mockMvc.perform(delete(STUDENTS_URL + "/" + user.getId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(user.getId())).isEmpty();
    }

    @Test
    @WithMockUser(username = "admin")
    public void deleteStudentByIdWithNonExistingId() throws Exception {
        // Vérifie que DELETE /api/students/{id} rejette un id inexistant.
        // Entrants : id absent en base + utilisateur authentifié.
        // Sortants : HTTP 400 Bad Request.
        // WHEN / THEN
        mockMvc.perform(delete(STUDENTS_URL + "/999999"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void deleteStudentByIdWithoutAuthentication() throws Exception {
        // Vérifie que DELETE /api/students/{id} est inaccessible sans authentification.
        // Entrants : id quelconque mais requête non authentifiée.
        // Sortants : HTTP 401 Unauthorized.
        // WHEN / THEN
        mockMvc.perform(delete(STUDENTS_URL + "/1"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    private User buildPersistedUser(String firstName, String lastName, String login, String rawPassword) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setLogin(login);
        user.setPassword(passwordEncoder.encode(rawPassword));
        return user;
    }
}
