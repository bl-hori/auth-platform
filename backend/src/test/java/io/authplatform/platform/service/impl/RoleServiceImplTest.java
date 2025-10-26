package io.authplatform.platform.service.impl;

import io.authplatform.platform.api.dto.RoleCreateRequest;
import io.authplatform.platform.api.dto.RoleResponse;
import io.authplatform.platform.api.dto.RoleUpdateRequest;
import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Role;
import io.authplatform.platform.domain.repository.OrganizationRepository;
import io.authplatform.platform.domain.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RoleServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService Tests")
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    private RoleServiceImpl roleService;

    private Organization testOrg;
    private Role parentRole;
    private Role childRole;
    private UUID roleId;
    private UUID parentRoleId;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(roleRepository, organizationRepository);

        orgId = UUID.randomUUID();
        roleId = UUID.randomUUID();
        parentRoleId = UUID.randomUUID();

        testOrg = Organization.builder()
                .id(orgId)
                .name("test-org")
                .displayName("Test Organization")
                .status(Organization.OrganizationStatus.ACTIVE)
                .build();

        parentRole = Role.builder()
                .id(parentRoleId)
                .organization(testOrg)
                .name("parent")
                .displayName("Parent Role")
                .level(0)
                .isSystem(false)
                .metadata(new HashMap<>())
                .build();

        childRole = Role.builder()
                .id(roleId)
                .organization(testOrg)
                .name("child")
                .displayName("Child Role")
                .parentRole(parentRole)
                .level(1)
                .isSystem(false)
                .metadata(new HashMap<>())
                .build();
    }

    @Test
    @DisplayName("Should create role successfully")
    void shouldCreateRoleSuccessfully() {
        // Given
        RoleCreateRequest request = RoleCreateRequest.builder()
                .organizationId(orgId)
                .name("newrole")
                .displayName("New Role")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(orgId, request.getName()))
                .thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(UUID.randomUUID());
            return role;
        });

        // When
        RoleResponse response = roleService.createRole(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(request.getName());
        assertThat(response.getDisplayName()).isEqualTo(request.getDisplayName());
        assertThat(response.getLevel()).isEqualTo(0);

        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("Should create role with parent successfully")
    void shouldCreateRoleWithParentSuccessfully() {
        // Given
        RoleCreateRequest request = RoleCreateRequest.builder()
                .organizationId(orgId)
                .name("child")
                .displayName("Child Role")
                .parentRoleId(parentRoleId)
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(orgId, request.getName()))
                .thenReturn(false);
        when(roleRepository.findByIdAndNotDeleted(parentRoleId)).thenReturn(Optional.of(parentRole));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        RoleResponse response = roleService.createRole(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getLevel()).isEqualTo(1);
        assertThat(response.getParentRoleId()).isEqualTo(parentRoleId);
    }

    @Test
    @DisplayName("Should throw exception when role name already exists")
    void shouldThrowExceptionWhenRoleNameAlreadyExists() {
        // Given
        RoleCreateRequest request = RoleCreateRequest.builder()
                .organizationId(orgId)
                .name("existing")
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(orgId, request.getName()))
                .thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when parent role not found")
    void shouldThrowExceptionWhenParentRoleNotFound() {
        // Given
        UUID nonExistentParentId = UUID.randomUUID();
        RoleCreateRequest request = RoleCreateRequest.builder()
                .organizationId(orgId)
                .name("child")
                .parentRoleId(nonExistentParentId)
                .build();

        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(testOrg));
        when(roleRepository.existsByOrganizationIdAndNameAndDeletedAtIsNull(orgId, request.getName()))
                .thenReturn(false);
        when(roleRepository.findByIdAndNotDeleted(nonExistentParentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent role not found");
    }

    @Test
    @DisplayName("Should get role by ID successfully")
    void shouldGetRoleByIdSuccessfully() {
        // Given
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(childRole));

        // When
        RoleResponse response = roleService.getRoleById(roleId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(roleId);
        assertThat(response.getName()).isEqualTo(childRole.getName());
    }

    @Test
    @DisplayName("Should get role hierarchy successfully")
    void shouldGetRoleHierarchySuccessfully() {
        // Given
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(childRole));

        // When
        List<String> hierarchy = roleService.getRoleHierarchy(roleId);

        // Then
        assertThat(hierarchy).containsExactly("parent", "child");
    }

    @Test
    @DisplayName("Should update role successfully")
    void shouldUpdateRoleSuccessfully() {
        // Given
        RoleUpdateRequest request = RoleUpdateRequest.builder()
                .displayName("Updated Display Name")
                .description("Updated description")
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(childRole));
        when(roleRepository.save(any(Role.class))).thenReturn(childRole);

        // When
        RoleResponse response = roleService.updateRole(roleId, request);

        // Then
        assertThat(response).isNotNull();
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    @DisplayName("Should throw exception when updating system role")
    void shouldThrowExceptionWhenUpdatingSystemRole() {
        // Given
        childRole.setIsSystem(true);
        RoleUpdateRequest request = RoleUpdateRequest.builder()
                .displayName("Updated")
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(childRole));

        // When/Then
        assertThatThrownBy(() -> roleService.updateRole(roleId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system role");

        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete role successfully")
    void shouldDeleteRoleSuccessfully() {
        // Given
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(childRole));
        when(roleRepository.findByParentRoleIdAndDeletedAtIsNull(roleId)).thenReturn(Collections.emptyList());
        when(roleRepository.save(any(Role.class))).thenReturn(childRole);

        // When
        roleService.deleteRole(roleId);

        // Then
        verify(roleRepository).save(argThat(Role::isDeleted));
    }

    @Test
    @DisplayName("Should throw exception when deleting system role")
    void shouldThrowExceptionWhenDeletingSystemRole() {
        // Given
        childRole.setIsSystem(true);
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(childRole));

        // When/Then
        assertThatThrownBy(() -> roleService.deleteRole(roleId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("system role");

        verify(roleRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when deleting role with children")
    void shouldThrowExceptionWhenDeletingRoleWithChildren() {
        // Given
        Role anotherChild = Role.builder()
                .id(UUID.randomUUID())
                .organization(testOrg)
                .name("another-child")
                .parentRole(childRole)
                .build();

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(childRole));
        when(roleRepository.findByParentRoleIdAndDeletedAtIsNull(roleId))
                .thenReturn(List.of(anotherChild));

        // When/Then
        assertThatThrownBy(() -> roleService.deleteRole(roleId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("child roles");

        verify(roleRepository, never()).save(any());
    }
}
