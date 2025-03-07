package com.betbrain.service;

import com.betbrain.model.BetEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Manages betting records and provides top stake rankings
 */
public class StakeService {

    private static final Logger logger = Logger.getLogger("StakeService");

    /**
     * All betting event records storage:
     * Key - Betting event ID (Integer)
     * Value - Corresponding betting data
     */
    private final ConcurrentHashMap<Integer, BetEvent> betEventData = new ConcurrentHashMap<>();


    private static final StakeService stakeService = new StakeService();
    private StakeService() {
    }

    public static StakeService getInstance() {
        return stakeService;
    }

    /**
     * Records a new stake for a betting event
     * @param eventId ID of the betting event
     * @param customerId ID of the customer
     * @param amount Stake amount (must be positive)
     */
    public void recordStake(int eventId, int customerId, int amount) {
        if (eventId < 0 || customerId < 0 || amount <= 0) {
            logger.warning("Invalid stake: event=" + eventId
                    + " customer=" + customerId
                    + " amount=" + amount);
            throw new IllegalArgumentException();
        }

        betEventData.compute(eventId, (id, event) -> {
            if (event == null) event = new BetEvent();
            event.updateStake(customerId, amount);
            return event;
        });
    }

    /**
     * Gets formatted top 20 stakes for a betting event
     * @param eventId Target event ID
     * @return Comma-separated "customerId=amount" pairs,
     *         or message if no stakes exist
     */
    public String getTop20Stakes(int eventId) {
        BetEvent event = betEventData.get(eventId);
        if (event == null) return "No stakes for event: " + eventId;

        List<Map.Entry<Integer, Integer>> entries = event.getTop20();
        return formatEntries(entries);
    }

    /**
     * Formats stake entries to string
     * @param entries List of customer-amount pairs
     * @return Comma-separated "customerId=amount" values
     */
    private String formatEntries(List<Map.Entry<Integer, Integer>> entries) {
        return entries.stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }
}