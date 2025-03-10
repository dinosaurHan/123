package com.betbrain.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BetEventTest {
    private BetEvent betEvent;

    @Before
    public void setUp() {
        betEvent = new BetEvent();
    }

    @Test
    public void testUpdateStakeNewCustomerStoresMaxAmount() {
        betEvent.updateStake(1, 100);
        assertEquals(100, (int) getCustomerMaxAmounts().get(1));
    }

    @Test
    public void testUpdateStakeHigherAmountUpdatesExistingCustomer() {
        betEvent.updateStake(1, 100);
        betEvent.updateStake(1, 200);
        assertEquals(200, (int) getCustomerMaxAmounts().get(1));
    }

    @Test
    public void testUpdateStakeLowerAmountPreservesExistingValue() {
        betEvent.updateStake(1, 200);
        betEvent.updateStake(1, 150);
        assertEquals(200, (int) getCustomerMaxAmounts().get(1));
    }

    @Test
    public void testGetTop20ReturnsEntriesInDescendingOrder() {
        betEvent.updateStake(1, 300);
        betEvent.updateStake(2, 400);
        betEvent.updateStake(3, 200);

        List<Map.Entry<Integer, Integer>> results = betEvent.getTop20();
        assertEquals(400, (int) results.get(0).getValue());
        assertEquals(300, (int) results.get(1).getValue());
        assertEquals(200, (int) results.get(2).getValue());
    }

    @Test
    public void testGetTop20LimitsTo20Entries() {
        for (int i = 1; i <= 25; i++) {
            betEvent.updateStake(i, i * 10);
        }

        List<Map.Entry<Integer, Integer>> results = betEvent.getTop20();
        assertEquals(20, results.size());
    }

    // 带异常处理的版本
    private ConcurrentHashMap<Integer, Integer> getCustomerMaxAmounts() {
        try {
            Field field = BetEvent.class.getDeclaredField("customerMaxAmounts");
            field.setAccessible(true);
            return (ConcurrentHashMap<Integer, Integer>) field.get(betEvent);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Reflection access failed", e);
        }
    }

}
