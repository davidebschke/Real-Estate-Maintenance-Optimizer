package com.remo.realestatemaintainceoptimizer.repository;

import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persists AI optimization proposals in the {@code optimization_proposals} table via Spring Data JPA, scoped to their account.
 */
public interface OptimizationProposalRepository extends JpaRepository<OptimizationProposal, String> {

    /**
     * Returns every proposal of the given account in the given state, sorted by its proposed start.
     */
    List<OptimizationProposal> findAllByOwnerIdAndStatusOrderByProposedStartAsc(
            String ownerId, OptimizationProposalStatus status);

    /**
     * Returns every proposal of the given account in the given state, sorted by the time it was decided.
     */
    List<OptimizationProposal> findAllByOwnerIdAndStatusOrderByDecidedAtAsc(
            String ownerId, OptimizationProposalStatus status);

    /**
     * Returns the proposal with the given id if it belongs to the given account.
     */
    Optional<OptimizationProposal> findByIdAndOwnerId(String id, String ownerId);
}
