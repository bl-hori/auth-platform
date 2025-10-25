package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Organization;
import io.authplatform.platform.domain.entity.Organization.OrganizationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Organization} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for organization management.
 * All queries automatically exclude soft-deleted organizations unless explicitly
 * included.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private OrganizationRepository organizationRepository;
 *
 * // Find active organization by name
 * Optional<Organization> org = organizationRepository.findByNameAndDeletedAtIsNull("acme-corp");
 *
 * // Get all active organizations
 * List<Organization> activeOrgs = organizationRepository.findAllActive();
 * }</pre>
 *
 * @see Organization
 */
@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    /**
     * Find organization by name, excluding soft-deleted organizations.
     *
     * @param name the organization name
     * @return Optional containing the organization if found and not deleted
     */
    Optional<Organization> findByNameAndDeletedAtIsNull(String name);

    /**
     * Find organization by name, including soft-deleted organizations.
     *
     * @param name the organization name
     * @return Optional containing the organization if found
     */
    Optional<Organization> findByName(String name);

    /**
     * Find organization by ID, excluding soft-deleted organizations.
     *
     * @param id the organization ID
     * @return Optional containing the organization if found and not deleted
     */
    @Query("SELECT o FROM Organization o WHERE o.id = :id AND o.deletedAt IS NULL")
    Optional<Organization> findByIdAndNotDeleted(@Param("id") UUID id);

    /**
     * Find all organizations with a specific status, excluding soft-deleted.
     *
     * @param status the organization status
     * @return list of organizations with the given status
     */
    List<Organization> findByStatusAndDeletedAtIsNull(OrganizationStatus status);

    /**
     * Find all active organizations (status = ACTIVE and not deleted).
     *
     * @return list of active organizations
     */
    @Query("SELECT o FROM Organization o WHERE o.status = io.authplatform.platform.domain.entity.Organization$OrganizationStatus.ACTIVE AND o.deletedAt IS NULL")
    List<Organization> findAllActive();

    /**
     * Find all non-deleted organizations.
     *
     * @return list of organizations that are not soft-deleted
     */
    @Query("SELECT o FROM Organization o WHERE o.deletedAt IS NULL")
    List<Organization> findAllNotDeleted();

    /**
     * Check if an organization with the given name exists (excluding deleted).
     *
     * @param name the organization name
     * @return true if an organization with the name exists and is not deleted
     */
    boolean existsByNameAndDeletedAtIsNull(String name);

    /**
     * Count active organizations.
     *
     * @return number of active organizations
     */
    @Query("SELECT COUNT(o) FROM Organization o WHERE o.status = io.authplatform.platform.domain.entity.Organization$OrganizationStatus.ACTIVE AND o.deletedAt IS NULL")
    long countActive();

    /**
     * Count all non-deleted organizations.
     *
     * @return number of non-deleted organizations
     */
    @Query("SELECT COUNT(o) FROM Organization o WHERE o.deletedAt IS NULL")
    long countNotDeleted();

    /**
     * Find organizations by name pattern (case-insensitive), excluding deleted.
     *
     * @param namePattern the name pattern to search for (use % for wildcards)
     * @return list of matching organizations
     */
    @Query("SELECT o FROM Organization o WHERE LOWER(o.name) LIKE LOWER(:namePattern) AND o.deletedAt IS NULL")
    List<Organization> findByNameContainingIgnoreCaseAndNotDeleted(@Param("namePattern") String namePattern);

    /**
     * Find organizations by display name pattern (case-insensitive), excluding deleted.
     *
     * @param displayNamePattern the display name pattern to search for
     * @return list of matching organizations
     */
    @Query("SELECT o FROM Organization o WHERE LOWER(o.displayName) LIKE LOWER(:displayNamePattern) AND o.deletedAt IS NULL")
    List<Organization> findByDisplayNameContainingIgnoreCaseAndNotDeleted(@Param("displayNamePattern") String displayNamePattern);
}
