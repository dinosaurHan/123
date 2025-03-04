package com.betbrain.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 投注业务逻辑<br>
 * <p>
 * 1、关于top20数据的更新，这里可能需要结合读写比例来权衡，如果写多读少，则比较适合设计成懒加载模式；反之，如果读多写少，则适合及时更新。下面的实现是按照读多写少的情况来实现的；<br>
 * 2、当前存储投注记录的结构是采用按照betId分片，每个betId下存储每个客户的投注列表（可能重复），这里可能可以设计成只保留每个每户在每个betId下的最大投注记录，减少存储以及后续遍历、计算的成本，
 * 暂不确定后续是否会有查询该客户的所有投注记录的诉求，所以此处暂时保留了list结构；
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class StakeService {

    private static final Logger log = Logger.getLogger("StakeService");
    // 分片数量,按照betId,暂不清楚betId的量级，后续可以配置成参数动态调整；
    private static final int SHARD_COUNT = 256;
    //投注记录按照betId分片存储
    private static final ConcurrentHashMap<Integer, Shard> shards = new ConcurrentHashMap<>();
    // 按照betId缓存 Top 20 结果
    private static final ConcurrentHashMap<Integer, List<Map.Entry<Integer, Integer>>> top20Cache = new ConcurrentHashMap<>();

    static {
        // 初始化分片
        for (int i = 0; i < SHARD_COUNT; i++) {
            shards.put(i, new Shard());
        }
    }

    // 分片类
    private static class Shard {

        // 投注 ID -> (客户 ID -> 投注金额列表)
        private final ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, CustomerData>> stakes = new ConcurrentHashMap<>();

        // 将投注记录存储到对应的分片
        public void addStake(int betId, int customerId, int stakeAmount) {
            stakes.compute(betId, (k, existingStakes) -> {
                if (existingStakes == null) {
                    existingStakes = new ConcurrentHashMap<>();
                }
                // 保留所有投注记录
                CustomerData customer = existingStakes.computeIfAbsent(customerId,
                        cid -> new CustomerData());

                // 判断该次投注是否需要更新缓存
                boolean maxUpdated = customer.updateMax(stakeAmount);

                // 需要更新时，才更新Top 20 缓存
                if (maxUpdated) {
                    // 更新该betId的top 20缓存
                    updateTop20CacheConditional(betId, existingStakes, customer.getMaxStake());
                }

                return existingStakes;
            });
        }

        /**
         * 客户投注数据（封装最大值跟踪）
         */
        private static class CustomerData {
            private int maxStake = 0;
            private final List<Integer> stakes = new ArrayList<>(); // 保留原始记录

            /**
             * 返回值表示是否更新了最大值
             */
            public boolean updateMax(int stake) {
                stakes.add(stake);
                if (stake > maxStake) {
                    maxStake = stake;
                    return true;
                }
                return false;
            }

            public int getMaxStake() {
                return maxStake;
            }

            public List<Integer> getStakes() {
                return Collections.unmodifiableList(stakes);
            }
        }

        /**
         * 带条件判断的缓存更新
         */
        private void updateTop20CacheConditional(int betId,
                                                 ConcurrentHashMap<Integer, CustomerData> betStakes,
                                                 int updatedMax) {
            List<Map.Entry<Integer, Integer>> currentTop = top20Cache.get(betId);

            // 计算当前阈值和条目数
            int currentThreshold = Integer.MIN_VALUE;
            int currentSize = 0;
            if (currentTop != null && !currentTop.isEmpty()) {
                currentSize = currentTop.size();
                currentThreshold = currentTop.get(currentSize - 1).getValue();
            }

            // 精确判断条件
            boolean needUpdate = false;
            if (currentSize >= 20) {
                // 当前Top20已满：仅当新值超过最后一名时更新
                needUpdate = updatedMax > currentThreshold;
            } else {
                // 当前Top20未满：仅当新值>0时更新（有效投注）
                needUpdate = updatedMax > 0;
            }

            // 只有当新值可能进入Top20时才更新（核心判断逻辑）
            if (needUpdate) {
                // 新建最小堆
                PriorityQueue<Map.Entry<Integer, Integer>> minHeap = new PriorityQueue<>(
                        Comparator.comparingInt(Map.Entry::getValue)
                );

                // 遍历所有客户数据
                betStakes.forEach((cid, customer) -> {
                    int max = customer.getMaxStake();
                    if (max > 0) {
                        minHeap.offer(new AbstractMap.SimpleEntry<>(cid, max));
                        if (minHeap.size() > 20) {
                            minHeap.poll();
                        }
                    }
                });

                // 生成新Top20列表
                List<Map.Entry<Integer, Integer>> newTop = new ArrayList<>(minHeap);
                newTop.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
                top20Cache.put(betId, Collections.unmodifiableList(newTop));
            }
        }

        public ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, CustomerData>> getStakes() {
            return stakes;
        }
    }

    // 根据投注 ID 计算分片
    private static int getShardIndex(int betId) {
        return betId % SHARD_COUNT;
    }

    // 添加投注
    public void addStake(int betId, int customerId, int stakeAmount) {
        //输入检查
        if (betId < 0 || customerId < 0 || stakeAmount <= 0) {
            log.warning(() -> String.format("Invalid stake: betId=%d, customer=%d, amount=%d",
                    betId, customerId, stakeAmount));

            throw new IllegalArgumentException("Invalid input parameters");
        }

        int shardIndex = getShardIndex(betId);
        Shard shard = shards.get(shardIndex);
        shard.addStake(betId, customerId, stakeAmount);
    }

    // 查询 Top 20 高额投注
    public String getTop20Stakes(int betId) {
        // 从缓存中获取 Top 20
        List<Map.Entry<Integer, Integer>> top20 = top20Cache.get(betId);
        if (top20 == null) {
            return "No stakes found for bet ID: " + betId;
        }

        // 格式化输出
        return formatResult(top20);
    }

    // 格式化输出
    private String formatResult(List<Map.Entry<Integer, Integer>> result) {
        return result.stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    public static String logAllStakes() {
        StringBuilder sb = new StringBuilder();
        sb.append("BetID Statistics:\n");

        // 创建按betId聚合的视图
        Map<Integer, Map<Integer, Integer>> allBets = new TreeMap<>();

        // 遍历所有分片收集数据
        shards.forEach((shardId, shard) -> {
            shard.getStakes().forEach((betId, customers) -> {
                Map<Integer, Integer> betStats = allBets.computeIfAbsent(betId, k -> new HashMap<>());
                customers.forEach((customerId, customerData) -> {
                    // 记录每个客户的投注次数
                    betStats.merge(customerId, customerData.getStakes().size(), Integer::sum);
                });
            });
        });

        // 格式化输出
        allBets.forEach((betId, customers) -> {
            sb.append("┌── BetID: ").append(betId).append("\n");
            customers.forEach((customerId, count) -> {
                sb.append(String.format("│   Customer %-6d → %d stakes\n", customerId, count));
            });
            sb.append("└──────────────────────\n");
        });

        return sb.toString();
    }

    public static String logTop20Cache() {
        StringBuilder sb = new StringBuilder();
        sb.append("Top20 Details:\n");

        top20Cache.forEach((betId, entries) -> {
            sb.append("┌── BetID: ").append(betId).append("\n");
            if (entries.isEmpty()) {
                sb.append("│   (Empty)\n");
            } else {
                entries.forEach(entry -> {
                    sb.append(String.format("│   %-6d → %,9d\n",
                            entry.getKey(), entry.getValue()));
                });
            }
            sb.append("└──────────────────────\n");
        });

        return sb.toString();
    }

}