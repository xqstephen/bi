package com.xy.bi.manager;

import com.xy.bi.common.ErrorCode;
import com.xy.bi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * redis 限流
 *
 * @author 25133
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流
     */
    public void doRateLimit(String key) {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRateAsync(RateType.OVERALL,2,1, RateIntervalUnit.SECONDS);
        //每当一个用户调用接口时，请求一个令牌
        boolean b = rateLimiter.tryAcquire(1);
        if (!b) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST_ERROR);
        }
    }
}
