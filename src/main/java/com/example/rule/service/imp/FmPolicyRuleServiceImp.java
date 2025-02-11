package com.example.rule.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.rule.mapper.FmPolicyRuleMapper;
import com.example.rule.model.FmPolicyRules;
import com.example.rule.service.FmPolicyRuleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FmPolicyRuleServiceImp extends ServiceImpl<FmPolicyRuleMapper, FmPolicyRules> implements FmPolicyRuleService {
    @Resource
    private FmPolicyRuleMapper fmPolicyRuleMapper;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RedissonClient redissonClient;
    /**
     * Redis 中规则组的 Key 前缀
     */
    private static final String RULE_GROUP_KEY_PREFIX = "rules:";
    /**
     * 规则数据的过期时间（单位：分钟）
     */
    private static final long BASE_EXPIRE_TIME = 30;

    /**
     * 过期时间的随机偏移量范围（单位：分钟）
     */
    private static final long EXPIRE_TIME_OFFSET = 10;

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    @Override
    public void addRule(FmPolicyRules rule) {
        // 插入规则
        fmPolicyRuleMapper.insert(rule);
        String secondaryEventId = rule.getSecondaryEventId();
        if (StringUtils.isNotBlank(secondaryEventId)) {
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            try {
                String ruleJson = objectMapper.writeValueAsString(rule);
                String[] secondaryEventIds = secondaryEventId.split(",");
                for (String secondaryEventId1 : secondaryEventIds) {
                    // 清理和验证分割后的字符串
                    String trimmedEventId = secondaryEventId1.trim();
                    if (trimmedEventId.isEmpty()) {
                        continue;
                    }
                    // 将规则信息存储到 Redis Set 中
                    String ruleGroupKey = RULE_GROUP_KEY_PREFIX + trimmedEventId;
                    redisTemplate.execute(new SessionCallback<Object>() {
                        @Override
                        public Object execute(RedisOperations operations) throws DataAccessException {
                            operations.multi();
                            setOps.add(ruleGroupKey, ruleJson);
                            long expireTime = BASE_EXPIRE_TIME + random.nextInt((int) EXPIRE_TIME_OFFSET);
                            operations.expire(ruleGroupKey, expireTime, TimeUnit.MINUTES);
                            return operations.exec();
                        }
                    });
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize rule: {}", e.getMessage());
            }
        }
        log.info("Rule added and cached in Redis: {}", rule.getId());
    }

    @Override
    public List<FmPolicyRules> getRulesByEventId(String eventId) {
        // 从 Redis Set 中获取规则信息
        String ruleGroupKey = RULE_GROUP_KEY_PREFIX + eventId;
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        Set<String> ruleJsons = setOps.members(ruleGroupKey);
        if (CollectionUtils.isEmpty(ruleJsons)) {
            // 使用分布式锁确保同一时间只有一个线程加载数据到 Redis 中
            RLock lock = redissonClient.getLock("lock:" + ruleGroupKey);
            try {
                if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                    // 再次检查 Redis 中是否有数据，防止多线程竞争
                    ruleJsons = setOps.members(ruleGroupKey);
                    if(CollectionUtils.isEmpty(ruleJsons)){
                        // 如果 Redis 中没有数据，从数据库加载
                        LambdaQueryWrapper<FmPolicyRules> queryWrapper = Wrappers.lambdaQuery(FmPolicyRules.class).
                                like(FmPolicyRules::getSecondaryEventId, eventId);
                        List<FmPolicyRules> rules = fmPolicyRuleMapper.selectList(queryWrapper);
                        if (!rules.isEmpty()) {
                            // 将规则信息存储到 Redis Set 中
                            for (FmPolicyRules rule : rules) {
                                try {
                                    String ruleJson = objectMapper.writeValueAsString(rule);
                                    setOps.add(ruleGroupKey, ruleJson);
                                } catch (JsonProcessingException e) {
                                    log.error("Failed to serialize rule: {}", e.getMessage());
                                }
                            }
                            redisTemplate.expire(ruleGroupKey, 1, TimeUnit.HOURS); // 设置过期时间为1小时
                        }
                        return rules;
                    }
                }
            } catch (InterruptedException e) {
                log.error("Failed to acquire lock for event {}: {}", eventId, e.getMessage());
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        // 将 Redis 中的数据转换为 LiteFlowRule 对象
        return ruleJsons.stream()
                .map(ruleJson -> {
                    try {
                        return objectMapper.readValue(ruleJson, FmPolicyRules.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize rule: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<FmPolicyRules> getRulesByTitle(String title) {
        // 从 Redis Set 中获取规则信息
        String ruleGroupKey = RULE_GROUP_KEY_PREFIX + title;
        SetOperations<String, String> setOps = redisTemplate.opsForSet();
        Set<String> ruleJsons = setOps.members(ruleGroupKey);
        if (CollectionUtils.isNotEmpty(ruleJsons)) {
            // 使用分布式锁确保同一时间只有一个线程加载数据到 Redis 中
            RLock lock = redissonClient.getLock("lock:" + ruleGroupKey);
            try {
                if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
                    // 再次检查 Redis 中是否有数据，防止多线程竞争
                    ruleJsons = setOps.members(ruleGroupKey);
                    // 如果 Redis 中没有数据，从数据库加载
                    List<FmPolicyRules> rules = fmPolicyRuleMapper.selectList(Wrappers.lambdaQuery(FmPolicyRules.class).
                            like(FmPolicyRules::getPrimaryTitle, title));
                    if (!rules.isEmpty()) {
                        // 将规则信息存储到 Redis Set 中
                        for (FmPolicyRules rule : rules) {
                            try {
                                String ruleJson = objectMapper.writeValueAsString(rule);
                                setOps.add(ruleGroupKey, ruleJson);
                            } catch (JsonProcessingException e) {
                                log.error("Failed to serialize rule: {}", e.getMessage());
                            }
                        }
                        redisTemplate.expire(ruleGroupKey, 1, TimeUnit.HOURS); // 设置过期时间为1小时
                    }
                    return rules;
                }
            } catch (InterruptedException e) {
                log.error("Failed to acquire lock for event {}: {}", title, e.getMessage());
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }

        // 将 Redis 中的数据转换为 LiteFlowRule 对象
        return ruleJsons.stream()
                .map(ruleJson -> {
                    try {
                        return objectMapper.readValue(ruleJson, FmPolicyRules.class);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize rule: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        // 删除数据库中的规则
        FmPolicyRules rule = fmPolicyRuleMapper.selectById(id);
        if (rule != null) {
            fmPolicyRuleMapper.deleteById(id);
            String secondaryEventId = rule.getSecondaryEventId();
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            if (StringUtils.isNotBlank(secondaryEventId)) {
                String[] secondaryEventIds = secondaryEventId.split(",");
                for (String secondaryEventId1 : secondaryEventIds) {
                    // 从 Redis Set 中删除规则信息
                    String ruleGroupKey = RULE_GROUP_KEY_PREFIX + secondaryEventId1;
                    try {
                        String ruleJson = objectMapper.writeValueAsString(rule);
                        setOps.remove(ruleGroupKey, ruleJson);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize rule: {}", e.getMessage());
                    }
                }
            }
            log.info("Rule deleted and cache updated: {}", id);
        }
    }
}
