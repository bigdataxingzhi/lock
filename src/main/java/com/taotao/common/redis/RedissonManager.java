package com.taotao.common.redis;

import javax.annotation.PostConstruct;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.taotao.util.common.PropertiesUtil;
import com.taotao.util.redis.RedisShardedPoolUtil;

/**
 * Created by geely
 */
@Component
public class RedissonManager {
	
	private static final Logger log = LoggerFactory.getLogger(RedissonManager.class);


    private Config config = new Config();

    private Redisson redisson = null;

    public Redisson getRedisson() {
        return redisson;
    }

    private static String redis1Ip = PropertiesUtil.getProperty("redis1.ip");
    private static Integer redis1Port = Integer.parseInt(PropertiesUtil.getProperty("redis1.port"));
    private static String redis2Ip = PropertiesUtil.getProperty("redis2.ip");
    private static Integer redis2Port = Integer.parseInt(PropertiesUtil.getProperty("redis2.port"));

    @PostConstruct
    private void init(){
        try {
        	/**
        	 * 集群时使用
        	 */
        	//config.useClusterServers().addNodeAddress((new StringBuilder().append(redis1Ip).append(":").append(redis1Port).toString(),(new StringBuilder().append(redis2Ip).append(":").append(redis2Port).toString());
           /**
            * 单节点时使用
            */
        	config.useSingleServer().setAddress(new StringBuilder().append(redis1Ip).append(":").append(redis1Port).toString());

            redisson = (Redisson) Redisson.create(config);

            log.info("初始化Redisson结束");
        } catch (Exception e) {
            log.error("redisson init error",e);
        }
    }



}
