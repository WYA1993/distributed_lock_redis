package demo.controller;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class StockController {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redisson;

    /**
     * 模拟下单减库存操作
     * 1.存在高并发问题
     *
     * @return
     */
    @GetMapping("order0")
    public String order0() {

        ValueOperations<String, Integer> redis = redisTemplate.opsForValue();

        String key = "stock";

        Integer stock = redis.get(key);

        if (stock > 0) {
            System.out.println("当前库存:" + stock);
            stock = stock - 1;
            redis.set(key, stock);
        } else {
            System.out.println("库存不足");
        }
        return "end";
    }

    /**
     * 模拟下单减库存操作
     * 2.单机部署使用锁解决
     *
     * @return
     */
    @GetMapping("order1")
    public String order1() {

        ValueOperations<String, Integer> redis = redisTemplate.opsForValue();

        String key = "stock";
        synchronized (this) {
            Integer stock = redis.get(key);
            if (stock > 0) {
                System.out.println("当前库存:" + stock);
                stock = stock - 1;
                redis.set(key, stock);
            } else {
                System.out.println("库存不足");
            }
        }
        return "end";
    }

    /**
     * 模拟下单减库存操作
     * 3.redis实现分布式锁，使用nginx负载均衡模拟，不完美
     *
     * @return
     */
    @GetMapping("order2")
    public String order2() {
        String lockName = "lock";
        // 获取锁
        Boolean lock = redisTemplate.opsForValue().setIfAbsent(lockName,"lock");
        redisTemplate.expire(lockName,2,TimeUnit.MINUTES);
        if (!lock) {
            System.out.println("没有获得锁");
            return "";
        }
        try {
            ValueOperations<String, Integer> redis = redisTemplate.opsForValue();
            String key = "stock";
            Integer stock = redis.get(key);
            if (stock > 0) {
                System.out.println("当前库存:" + stock);
                stock = stock - 1;
                redis.set(key, stock);
            } else {
                System.out.println("库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 释放锁
            redisTemplate.delete(lockName);
        }
        return "end";
    }

    /**
     * 模拟下单减库存操作
     * 4.Redisson实现分布式锁
     *
     * @return
     */
    @GetMapping("order")
    public String order() {

        String key = "stock";
        String lockName = "lock";
        // 获取锁
        RLock lock = redisson.getLock(lockName);
        //lock.lock(2, TimeUnit.MINUTES); //lock提供带timeout参数，timeout结束强制解锁，防止死锁
        lock.lock();
        try {
            ValueOperations<String, Integer> redis = redisTemplate.opsForValue();
            Integer stock = redis.get(key);
            if (stock > 0) {
                System.out.println("当前库存:" + stock);
                stock = stock - 1;
                redis.set(key, stock);
            } else {
                System.out.println("库存不足");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 释放锁
            lock.unlock();
        }

        return "end";
    }
}
