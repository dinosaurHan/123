package com.betbrain.service;

// StakeServiceTest.java (修改版)
import com.betbrain.model.BetEvent;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class StakeServiceTest {
    private StakeService stakeService;

    @Before
    public void setUp() throws Exception {
        stakeService = StakeService.getInstance();
        resetBetEventData(); // 每次测试前重置数据
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRecordStakeThrowsForInvalidAmount() {
        stakeService.recordStake(1, 1, -100);
    }

    @Test
    public void testRecordStakeCreatesNewBetEventWhenMissing() throws Exception {
        stakeService.recordStake(1, 1, 100);
        assertNotNull(getBetEventData().get(1));
    }

    @Test
    public void testGetTop20StakesReturnsCorrectFormat() {
        stakeService.recordStake(1, 1001, 500);
        stakeService.recordStake(1, 1002, 400);

        String result = stakeService.getTop20Stakes(1);
        assertTrue(result.matches("1001=500,1002=400|1002=400,1001=500"));
    }

    @Test
    public void testGetTop20StakesReturnsEmptyMessageForMissingBetId() {
        String result = stakeService.getTop20Stakes(999);
        assertEquals("No stakes for bet ID: 999", result);
    }

    // 通过反射重置单例状态
    private void resetBetEventData() throws Exception {
        Field field = StakeService.class.getDeclaredField("betEventData");
        field.setAccessible(true);
        ((ConcurrentHashMap) field.get(stakeService)).clear();
    }

    // 辅助方法获取内部状态（仅用于断言）
    private ConcurrentHashMap<Integer, BetEvent> getBetEventData() throws Exception {
        Field field = StakeService.class.getDeclaredField("betEventData");
        field.setAccessible(true);
        return (ConcurrentHashMap<Integer, BetEvent>) field.get(stakeService);
    }
}