package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.config.OptimizationProperties;
import com.remo.realestatemaintainceoptimizer.dto.SavingsGranularity;
import com.remo.realestatemaintainceoptimizer.dto.SavingsPeriodResponse;
import com.remo.realestatemaintainceoptimizer.dto.SavingsStatisticsResponse;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposal;
import com.remo.realestatemaintainceoptimizer.entity.OptimizationProposalStatus;
import com.remo.realestatemaintainceoptimizer.repository.OptimizationProposalRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sums up the road distance and driving time an account saved through accepted AI optimization proposals, in total and per week or month.
 */
@Service
@Transactional(readOnly = true)
@EnableConfigurationProperties(OptimizationProperties.class)
public class OptimizationSavingsService {

    private final OptimizationProposalRepository proposalRepository;
    private final OptimizationProperties properties;

    public OptimizationSavingsService(OptimizationProposalRepository proposalRepository, OptimizationProperties properties) {
        this.proposalRepository = proposalRepository;
        this.properties = properties;
    }

    /**
     * Returns the given account's savings, grouped by the period in which each proposal was accepted, with an entry for every period from the first acceptance up to the current one so a line chart shows gaps as zero.
     */
    public SavingsStatisticsResponse statistics(String ownerId, SavingsGranularity granularity) {
        List<OptimizationProposal> accepted = proposalRepository.findAllByOwnerIdAndStatusOrderByDecidedAtAsc(
                ownerId, OptimizationProposalStatus.ACCEPTED);
        SortedMap<LocalDate, SavingsPeriodResponse> periodsByStart = new TreeMap<>();
        double totalDistance = 0;
        double totalDuration = 0;
        for (OptimizationProposal proposal : accepted) {
            LocalDate periodStart = granularity.periodStart(proposal.decidedAt().atZone(properties.zone()).toLocalDate());
            periodsByStart.merge(
                    periodStart,
                    new SavingsPeriodResponse(periodStart, proposal.savedDistanceMeters(), proposal.savedDurationSeconds(), 1),
                    OptimizationSavingsService::add);
            totalDistance += proposal.savedDistanceMeters();
            totalDuration += proposal.savedDurationSeconds();
        }
        return new SavingsStatisticsResponse(
                granularity, totalDistance, totalDuration, accepted.size(), withoutGaps(periodsByStart, granularity));
    }

    private List<SavingsPeriodResponse> withoutGaps(
            SortedMap<LocalDate, SavingsPeriodResponse> periodsByStart, SavingsGranularity granularity) {
        List<SavingsPeriodResponse> periods = new ArrayList<>();
        if (periodsByStart.isEmpty()) {
            return periods;
        }
        LocalDate currentPeriod = granularity.periodStart(LocalDate.now(properties.zone()));
        LocalDate lastPeriod = periodsByStart.lastKey();
        LocalDate endPeriod = lastPeriod.isAfter(currentPeriod) ? lastPeriod : currentPeriod;
        for (LocalDate period = periodsByStart.firstKey();
                !period.isAfter(endPeriod);
                period = granularity.nextPeriodStart(period)) {
            periods.add(periodsByStart.getOrDefault(period, new SavingsPeriodResponse(period, 0, 0, 0)));
        }
        return periods;
    }

    private static SavingsPeriodResponse add(SavingsPeriodResponse first, SavingsPeriodResponse second) {
        return new SavingsPeriodResponse(
                first.periodStart(),
                first.savedDistanceMeters() + second.savedDistanceMeters(),
                first.savedDurationSeconds() + second.savedDurationSeconds(),
                first.acceptedProposalCount() + second.acceptedProposalCount());
    }
}
