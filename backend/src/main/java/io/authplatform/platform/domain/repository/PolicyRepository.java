package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.Policy;
import io.authplatform.platform.domain.entity.Policy.PolicyStatus;
import io.authplatform.platform.domain.entity.Policy.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Policy} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for policy management.
 * Queries are scoped to organizations for multi-tenancy support.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private PolicyRepository policyRepository;
 *
 * // Find policy by name in organization
 * Optional<Policy> policy = policyRepository.findByOrganizationIdAndName(
 *     orgId, "document-access-policy");
 *
 * // Get all active policies
 * List<Policy> activePolicies = policyRepository.findActiveByOrganizationId(orgId);
 *
 * // Find Rego policies
 * List<Policy> regoPolicies = policyRepository.findByOrganizationIdAndType(
 *     orgId, PolicyType.REGO);
 * }</pre>
 *
 * @see Policy
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    /**
     * Find policy by name within an organization.
     *
     * @param organizationId the organization ID
     * @param name the policy name
     * @return Optional containing the policy if found
     */
    Optional<Policy> findByOrganizationIdAndName(UUID organizationId, String name);

    /**
     * Find all policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of all policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId AND p.deletedAt IS NULL")
    List<Policy> findAllByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all policies including soft-deleted ones in an organization.
     *
     * @param organizationId the organization ID
     * @return list of all policies including deleted ones
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId")
    List<Policy> findAllByOrganizationIdIncludingDeleted(@Param("organizationId") UUID organizationId);

    /**
     * Find policies by status within an organization.
     *
     * @param organizationId the organization ID
     * @param status the policy status
     * @return list of policies with the specified status
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.status = :status AND p.deletedAt IS NULL")
    List<Policy> findByOrganizationIdAndStatus(
            @Param("organizationId") UUID organizationId,
            @Param("status") PolicyStatus status);

    /**
     * Find all active policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of active policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.status = io.authplatform.platform.domain.entity.Policy$PolicyStatus.ACTIVE " +
           "AND p.deletedAt IS NULL")
    List<Policy> findActiveByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all draft policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of draft policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.status = io.authplatform.platform.domain.entity.Policy$PolicyStatus.DRAFT " +
           "AND p.deletedAt IS NULL")
    List<Policy> findDraftByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all archived policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of archived policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.status = io.authplatform.platform.domain.entity.Policy$PolicyStatus.ARCHIVED " +
           "AND p.deletedAt IS NULL")
    List<Policy> findArchivedByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find policies by type within an organization.
     *
     * @param organizationId the organization ID
     * @param policyType the policy type (REGO or CEDAR)
     * @return list of policies of the specified type
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.policyType = :policyType AND p.deletedAt IS NULL")
    List<Policy> findByOrganizationIdAndType(
            @Param("organizationId") UUID organizationId,
            @Param("policyType") PolicyType policyType);

    /**
     * Find all Rego policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of Rego policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.policyType = io.authplatform.platform.domain.entity.Policy$PolicyType.REGO " +
           "AND p.deletedAt IS NULL")
    List<Policy> findRegoByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all Cedar policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of Cedar policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.policyType = io.authplatform.platform.domain.entity.Policy$PolicyType.CEDAR " +
           "AND p.deletedAt IS NULL")
    List<Policy> findCedarByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all soft-deleted policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of deleted policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId AND p.deletedAt IS NOT NULL")
    List<Policy> findDeletedByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find policies created by a specific user.
     *
     * @param createdById the user ID who created the policies
     * @return list of policies created by the user
     */
    @Query("SELECT p FROM Policy p WHERE p.createdBy.id = :createdById AND p.deletedAt IS NULL")
    List<Policy> findByCreatedById(@Param("createdById") UUID createdById);

    /**
     * Find policies by name pattern (case-insensitive) within an organization.
     *
     * @param organizationId the organization ID
     * @param namePattern the name pattern to search for (use % for wildcards)
     * @return list of matching policies
     */
    @Query("SELECT p FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND LOWER(p.name) LIKE LOWER(:namePattern) AND p.deletedAt IS NULL")
    List<Policy> findByNameContainingIgnoreCase(
            @Param("organizationId") UUID organizationId,
            @Param("namePattern") String namePattern);

    /**
     * Check if a policy with the given name exists in an organization.
     *
     * @param organizationId the organization ID
     * @param name the policy name
     * @return true if a policy with the name exists
     */
    boolean existsByOrganizationIdAndName(UUID organizationId, String name);

    /**
     * Count all policies in an organization.
     *
     * @param organizationId the organization ID
     * @return number of policies (excluding deleted)
     */
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.organization.id = :organizationId AND p.deletedAt IS NULL")
    long countByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Count active policies in an organization.
     *
     * @param organizationId the organization ID
     * @return number of active policies
     */
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.status = io.authplatform.platform.domain.entity.Policy$PolicyStatus.ACTIVE " +
           "AND p.deletedAt IS NULL")
    long countActiveByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Count draft policies in an organization.
     *
     * @param organizationId the organization ID
     * @return number of draft policies
     */
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.status = io.authplatform.platform.domain.entity.Policy$PolicyStatus.DRAFT " +
           "AND p.deletedAt IS NULL")
    long countDraftByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Count policies by type in an organization.
     *
     * @param organizationId the organization ID
     * @param policyType the policy type
     * @return number of policies of the specified type
     */
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.organization.id = :organizationId " +
           "AND p.policyType = :policyType AND p.deletedAt IS NULL")
    long countByOrganizationIdAndType(
            @Param("organizationId") UUID organizationId,
            @Param("policyType") PolicyType policyType);
}
