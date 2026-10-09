package com.remo.realestatemaintainceoptimizer.service;

import java.time.LocalDate;

/**
 * Everything the optimization planner needs besides the schedule itself: the travel matrix, the account's buffer between appointments and the day the planning window is measured from.
 */
public record PlanningContext(TravelMatrix matrix, int bufferMinutes, LocalDate today) {
}
