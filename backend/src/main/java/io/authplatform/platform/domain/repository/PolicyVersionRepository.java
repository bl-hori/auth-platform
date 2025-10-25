package io.authplatform.platform.domain.repository;

import io.authplatform.platform.domain.entity.PolicyVersion;
import io.authplatform.platform.domain.entity.PolicyVersion.ValidationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link PolicyVersion} entity operations.
 *
 * <p>Provides CRUD operations and custom queries for policy version management.
 * Supports version history tracking, validation status queries, and publication tracking.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Autowired
 * private PolicyVersionRepository policyVersionRepository;
 *
 * // Get all versions of a policy
 * List<PolicyVersion> versions = policyVersionRepository.findByPolicyId(policyId);
 *
 * // Find a specific version
 * Optional<PolicyVersion> version = policyVersionRepository.findByPolicyIdAndVersion(
 *     policyId, 2);
 *
 * // Get the latest version
 * Optional<PolicyVersion> latest = policyVersionRepository.findLatestByPolicyId(policyId);
 *
 * // Find published versions
 * List<PolicyVersion> published = policyVersionRepository.findPublishedByPolicyId(policyId);
 * }</pre>
 *
 * @see PolicyVersion
 */
@Repository
public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, UUID> {

    /**
     * Find all versions of a policy, ordered by version number descending.
     *
     * @param policyId the policy ID
     * @return list of policy versions
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId ORDER BY pv.version DESC")
    List<PolicyVersion> findByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Find a specific version of a policy.
     *
     * @param policyId the policy ID
     * @param version the version number
     * @return Optional containing the version if found
     */
    Optional<PolicyVersion> findByPolicyIdAndVersion(UUID policyId, Integer version);

    /**
     * Find the latest version of a policy.
     *
     * @param policyId the policy ID
     * @return Optional containing the latest version if found
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "ORDER BY pv.version DESC LIMIT 1")
    Optional<PolicyVersion> findLatestByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Find all published versions of a policy.
     *
     * @param policyId the policy ID
     * @return list of published versions
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.publishedAt IS NOT NULL ORDER BY pv.version DESC")
    List<PolicyVersion> findPublishedByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Find all unpublished (draft) versions of a policy.
     *
     * @param policyId the policy ID
     * @return list of unpublished versions
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.publishedAt IS NULL ORDER BY pv.version DESC")
    List<PolicyVersion> findUnpublishedByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Find versions by validation status.
     *
     * @param policyId the policy ID
     * @param status the validation status
     * @return list of versions with the specified status
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.validationStatus = :status ORDER BY pv.version DESC")
    List<PolicyVersion> findByPolicyIdAndValidationStatus(
            @Param("policyId") UUID policyId,
            @Param("status") ValidationStatus status);

    /**
     * Find all valid versions of a policy.
     *
     * @param policyId the policy ID
     * @return list of valid versions
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.validationStatus = io.authplatform.platform.domain.entity.PolicyVersion$ValidationStatus.VALID " +
           "ORDER BY pv.version DESC")
    List<PolicyVersion> findValidByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Find all invalid versions of a policy.
     *
     * @param policyId the policy ID
     * @return list of invalid versions
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.validationStatus = io.authplatform.platform.domain.entity.PolicyVersion$ValidationStatus.INVALID " +
           "ORDER BY pv.version DESC")
    List<PolicyVersion> findInvalidByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Find versions with pending validation.
     *
     * @param policyId the policy ID
     * @return list of pending versions
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.validationStatus = io.authplatform.platform.domain.entity.PolicyVersion$ValidationStatus.PENDING " +
           "ORDER BY pv.version DESC")
    List<PolicyVersion> findPendingByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Find versions by checksum (for deduplication).
     *
     * @param policyId the policy ID
     * @param checksum the content checksum
     * @return list of versions with the same checksum
     */
    List<PolicyVersion> findByPolicyIdAndChecksum(UUID policyId, String checksum);

    /**
     * Find versions created by a specific user.
     *
     * @param createdById the user ID who created the versions
     * @return list of versions created by the user
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.createdBy.id = :createdById " +
           "ORDER BY pv.createdAt DESC")
    List<PolicyVersion> findByCreatedById(@Param("createdById") UUID createdById);

    /**
     * Find versions published by a specific user.
     *
     * @param publishedById the user ID who published the versions
     * @return list of versions published by the user
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.publishedBy.id = :publishedById " +
           "ORDER BY pv.publishedAt DESC")
    List<PolicyVersion> findByPublishedById(@Param("publishedById") UUID publishedById);

    /**
     * Find versions published within a date range.
     *
     * @param policyId the policy ID
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of versions published in the date range
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.publishedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY pv.publishedAt DESC")
    List<PolicyVersion> findPublishedBetween(
            @Param("policyId") UUID policyId,
            @Param("startDate") OffsetDateTime startDate,
            @Param("endDate") OffsetDateTime endDate);

    /**
     * Find all versions across all policies in an organization.
     *
     * @param organizationId the organization ID
     * @return list of all policy versions
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.organization.id = :organizationId " +
           "ORDER BY pv.createdAt DESC")
    List<PolicyVersion> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find versions with validation errors.
     *
     * @param policyId the policy ID
     * @return list of versions with validation errors
     */
    @Query("SELECT pv FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.validationErrors IS NOT NULL ORDER BY pv.version DESC")
    List<PolicyVersion> findWithValidationErrors(@Param("policyId") UUID policyId);

    /**
     * Check if a version exists for a policy.
     *
     * @param policyId the policy ID
     * @param version the version number
     * @return true if the version exists
     */
    boolean existsByPolicyIdAndVersion(UUID policyId, Integer version);

    /**
     * Check if a checksum already exists for a policy (for deduplication).
     *
     * @param policyId the policy ID
     * @param checksum the content checksum
     * @return true if a version with the checksum exists
     */
    boolean existsByPolicyIdAndChecksum(UUID policyId, String checksum);

    /**
     * Count versions of a policy.
     *
     * @param policyId the policy ID
     * @return number of versions
     */
    long countByPolicyId(UUID policyId);

    /**
     * Count published versions of a policy.
     *
     * @param policyId the policy ID
     * @return number of published versions
     */
    @Query("SELECT COUNT(pv) FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.publishedAt IS NOT NULL")
    long countPublishedByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Count valid versions of a policy.
     *
     * @param policyId the policy ID
     * @return number of valid versions
     */
    @Query("SELECT COUNT(pv) FROM PolicyVersion pv WHERE pv.policy.id = :policyId " +
           "AND pv.validationStatus = io.authplatform.platform.domain.entity.PolicyVersion$ValidationStatus.VALID")
    long countValidByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Get the maximum version number for a policy.
     *
     * @param policyId the policy ID
     * @return Optional containing the max version number, or empty if no versions exist
     */
    @Query("SELECT MAX(pv.version) FROM PolicyVersion pv WHERE pv.policy.id = :policyId")
    Optional<Integer> findMaxVersionByPolicyId(@Param("policyId") UUID policyId);

    /**
     * Delete all versions of a policy.
     *
     * @param policyId the policy ID
     */
    void deleteByPolicyId(UUID policyId);
}
