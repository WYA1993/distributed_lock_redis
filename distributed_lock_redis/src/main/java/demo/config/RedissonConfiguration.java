package demo.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfiguration {


    @Bean
    public RedissonClient redisson(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://192.168.0.117:6379").setPassword("123456").setDatabase(0);
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }

}
