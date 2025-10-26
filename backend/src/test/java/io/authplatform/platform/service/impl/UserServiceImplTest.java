package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.UserCreateRequest;
import io.authplatform.platform.api.dto.UserListResponse;
import io.authplatform.platform.api.dto.UserResponse;
import io.authplatform.platform.api.dto.UserUpdateRequest;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.User;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.domain.repository.RoleRepository;
import io.authplatform.platform.domain.repository.UserRepository;
import io.authplatform.platform.domain.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private UserServiceImpl userService;

    private Organization testOrg;
    private User testUser;
    private UUID userId;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, organizationRepository);
        userService = new UserServiceImpl(userRepository, organizationRepository, roleRepository, userRoleRepository);

        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testOrg = Organization.builder()
                .id(orgId)
                .name("test-org")
                .displayName("Test Organization")
                .status(Organization.OrganizationStatus.ACTIVE)
                .build();

        testUser = User.builder()
                .id(userId)
                .organization(testOrg)
                .email("test@example.com")
                .username("testuser")
                .displayName("Test User")
                .status(User.UserStatus.ACTIVE)
                .attributes(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUserSuccessfully() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .organizationId(orgId)
                .email("newuser@example.com")
                .username("newuser")
                .displayName("New User")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(userRepository.existsByOrganizationIdAndEmailAndDeletedAtIsNull(orgId, request.getEmail()))
                .thenReturn(false);
        when(userRepository.existsByOrganizationIdAndUsernameAndDeletedAtIsNull(orgId, request.getUsername()))
                .thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        // When
        UserResponse response = userService.createUser(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo(request.getEmail());
        assertThat(response.getUsername()).isEqualTo(request.getUsername());
        assertThat(response.getDisplayName()).isEqualTo(request.getDisplayName());
        assertThat(response.getStatus()).isEqualTo("active");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw exception when organization not found")
    void shouldThrowExceptionWhenOrganizationNotFound() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .organizationId(orgId)
                .email("test@example.com")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Organization not found");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .organizationId(orgId)
                .email("existing@example.com")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(userRepository.existsByOrganizationIdAndEmailAndDeletedAtIsNull(orgId, request.getEmail()))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when username already exists")
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // Given
        UserCreateRequest request = UserCreateRequest.builder()
                .organizationId(orgId)
                .email("test@example.com")
                .username("existinguser")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(userRepository.existsByOrganizationIdAndEmailAndDeletedAtIsNull(orgId, request.getEmail()))
                .thenReturn(false);
        when(userRepository.existsByOrganizationIdAndUsernameAndDeletedAtIsNull(orgId, request.getUsername()))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("username already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get user by ID successfully")
    void shouldGetUserByIdSuccessfully() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When
        UserResponse response = userService.getUserById(userId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(userId);
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("Should update user successfully")
    void shouldUpdateUserSuccessfully() {
        // Given
        UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("Updated Name")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = userService.updateUser(userId, request);

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should deactivate user successfully")
    void shouldDeactivateUserSuccessfully() {
        // Given
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.deactivateUser(userId);

        // Then
        verify(userRepository).save(argThat(user ->
            user.isDeleted() && user.getStatus() == User.UserStatus.DELETED
        ));
    }

    @Test
    @DisplayName("Should throw exception when deactivating already deleted user")
    void shouldThrowExceptionWhenDeactivatingAlreadyDeletedUser() {
        // Given
        testUser.softDelete();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> userService.deactivateUser(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already deleted");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should activate user successfully")
    void shouldActivateUserSuccessfully() {
        // Given
        testUser.softDelete();
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userService.activateUser(userId);

        // Then
        verify(userRepository).save(argThat(user ->
            !user.isDeleted() && user.getStatus() == User.UserStatus.ACTIVE
        ));
    }
}
