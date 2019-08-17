package com.taotao.lock.redis;

import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.aopalliance.intercept.Joinpoint;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import com.taotao.common.redis.RedissonManager;
import com.taotao.util.common.PropertiesUtil;
import com.taotao.util.redis.RedisShardedPoolUtil;

@Component
@Aspect
@ConditionalOnClass(redis.clients.jedis.JedisPoolConfig.class)
public class RedisLock {

	private static final Logger log = LoggerFactory.getLogger(RedisLock.class);

	private static final String REDIS_LOCK = "REDIS_LOCK";//分布式锁
	
    @Autowired
    private RedissonManager redissonManager;

    @PreDestroy
    public void delLock(){
        RedisShardedPoolUtil.del(REDIS_LOCK);

    }
	
    /**
     * 改造为AOP
     */
    
    public void redisLock(ProceedingJoinPoint joinpoint){
        long lockTimeout = Long.parseLong(PropertiesUtil.getProperty("lock.timeout","5000"));
        Long setnxResult = RedisShardedPoolUtil.setnx(REDIS_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));
        if(setnxResult != null && setnxResult.intValue() == 1){
        	try {
				joinpoint.proceed();
			} catch (Throwable e) {
				e.printStackTrace();
			}
        }else{
            //未获取到锁，继续判断，判断时间戳，看是否可以重置并获取到锁
            String lockValueStr = RedisShardedPoolUtil.get(REDIS_LOCK);
            if(lockValueStr != null && System.currentTimeMillis() > Long.parseLong(lockValueStr)){
                String getSetResult = RedisShardedPoolUtil.getSet(REDIS_LOCK,String.valueOf(System.currentTimeMillis()+lockTimeout));
                //再次用当前时间戳getset。
                //返回给定的key的旧值，->旧值判断，是否可以获取锁
                //当key没有旧值时，即key不存在时，返回nil ->获取锁
                //这里我们set了一个新的value值，获取旧的值。
                if(getSetResult == null || (getSetResult != null && StringUtils.equals(lockValueStr,getSetResult))){
                    //真正获取到锁
                	try {
        				joinpoint.proceed();
        			} catch (Throwable e) {
        				e.printStackTrace();
        			}
                }else{
                    log.info("没有获取到分布式锁:{}",REDIS_LOCK);
                }
            }else{
                log.info("没有获取到分布式锁:{}",REDIS_LOCK);
            }
        }
    }

    @ConditionalOnClass(org.redisson.config.Config.class)
    public void redissonLock(ProceedingJoinPoint joinpoint){
        RLock lock = redissonManager.getRedisson().getLock(REDIS_LOCK);
        boolean getLock = false;
        try {
            if(getLock = lock.tryLock(0,50, TimeUnit.SECONDS)){
                log.info("Redisson获取到分布式锁:{},ThreadName:{}",REDIS_LOCK,Thread.currentThread().getName());
                int hour = Integer.parseInt(PropertiesUtil.getProperty("close.order.task.time.hour","2"));
//                //真正获取到锁
            	try {
    				joinpoint.proceed();
    			} catch (Throwable e) {
    				e.printStackTrace();
    			}
            }else{
                log.info("Redisson没有获取到分布式锁:{},ThreadName:{}",REDIS_LOCK,Thread.currentThread().getName());
            }
        } catch (InterruptedException e) {
            log.error("Redisson分布式锁获取异常",e);
        } finally {
            if(!getLock){
                return;
            }
            lock.unlock();
            log.info("Redisson分布式锁释放锁");
        }
    }
}
