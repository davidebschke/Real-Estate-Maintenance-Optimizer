package com.remo.realestatemaintainceoptimizer.controller;

import com.remo.realestatemaintainceoptimizer.dto.OptimizationProposalResponse;
import com.remo.realestatemaintainceoptimizer.dto.OptimizationRunResponse;
import com.remo.realestatemaintainceoptimizer.dto.SavingsGranularity;
import com.remo.realestatemaintainceoptimizer.dto.SavingsStatisticsResponse;
import com.remo.realestatemaintainceoptimizer.security.AuthenticatedUser;
import com.remo.realestatemaintainceoptimizer.service.OptimizationProposalService;
import com.remo.realestatemaintainceoptimizer.service.OptimizationSavingsService;
import com.remo.realestatemaintainceoptimizer.service.OptimizationService;
import java.util.List;
import java.util.Locale;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the logged-in account's AI appointment optimization: starting a run, deciding on its proposals and the resulting savings statistics.
 */
@RestController
@RequestMapping("/api/optimizations")
public class OptimizationController {

    private final OptimizationService optimizationService;
    private final OptimizationProposalService proposalService;
    private final OptimizationSavingsService savingsService;

    public OptimizationController(
            OptimizationService optimizationService,
            OptimizationProposalService proposalService,
            OptimizationSavingsService savingsService) {
        this.optimizationService = optimizationService;
        this.proposalService = proposalService;
        this.savingsService = savingsService;
    }

    /**
     * Starts an optimization run whose proposal reasons are written in the request's language.
     */
    @PostMapping
    public OptimizationRunResponse startRun(@AuthenticationPrincipal AuthenticatedUser user, Locale locale) {
        return optimizationService.run(user.id(), locale);
    }

    /**
     * Returns every proposal still awaiting a decision.
     */
    @GetMapping("/proposals")
    public List<OptimizationProposalResponse> listPendingProposals(@AuthenticationPrincipal AuthenticatedUser user) {
        return proposalService.listPending(user.id());
    }

    /**
     * Accepts a proposal, moving its appointment to the proposed slot.
     */
    @PostMapping("/proposals/{id}/accept")
    public OptimizationProposalResponse acceptProposal(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return proposalService.accept(user.id(), id);
    }

    /**
     * Rejects a proposal, leaving its appointment untouched.
     */
    @PostMapping("/proposals/{id}/reject")
    public OptimizationProposalResponse rejectProposal(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String id) {
        return proposalService.reject(user.id(), id);
    }

    /**
     * Returns the saved road distance and driving time per week or month.
     */
    @GetMapping("/savings")
    public SavingsStatisticsResponse savings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(defaultValue = "WEEK") SavingsGranularity granularity) {
        return savingsService.statistics(user.id(), granularity);
    }
}
