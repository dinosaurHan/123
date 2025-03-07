package com.betbrain.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Contains all stake records for a single betting event
 */
public class BetEvent {

    /**
     * Maximum stake amounts per customer:
     * Key - Customer ID (Integer)
     * Value - Highest stake amount (Integer)
     */
    private final ConcurrentHashMap<Integer, Integer> customerMaxAmounts = new ConcurrentHashMap<>();

    /**
     * Stake amounts sorted descendingly:
     * Key - Stake amount (Integer)
     * Value - Customer IDs with this amount (Set<Integer>)
     */
    private final ConcurrentSkipListMap<Integer, Set<Integer>> amountToCustomers =
            new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    /**
     * Updates customer's maximum stake amount
     * @param customerId ID of the customer
     * @param newAmount New stake amount to record
     */
    public void updateStake(int customerId, int newAmount) {
        customerMaxAmounts.compute(customerId, (id, currentMax) -> {
            if (currentMax != null && newAmount <= currentMax) {
                return currentMax;
            }

            if (currentMax != null) {
                amountToCustomers.compute(currentMax, (amount, customers) -> {
                    if (customers != null) {
                        customers.remove(id);
                        return customers.isEmpty() ? null : customers;
                    }
                    return null;
                });
            }

            amountToCustomers.compute(newAmount, (amount, customers) -> {
                if (customers == null) {
                    customers = ConcurrentHashMap.newKeySet();
                }
                customers.add(id);
                return customers;
            });

            return newAmount;
        });
    }

    /**
     * Gets top 20 highest stake entries
     * @return List of entries containing:
     *         Key - Customer ID
     *     Value - Stake amount
     */
    public List<Map.Entry<Integer, Integer>> getTop20() {
        List<Map.Entry<Integer, Integer>> results = new ArrayList<>(20);
        for (Map.Entry<Integer, Set<Integer>> entry : amountToCustomers.entrySet()) {
            int amount = entry.getKey();
            for (int customerId : entry.getValue()) {
                results.add(new AbstractMap.SimpleEntry<>(customerId, amount));
                if (results.size() >= 20) {
                    return results;
                }
            }
        }
        return results;
    }
}