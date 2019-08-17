
package com.taotao.redisLockAnnocation;

/**
 * 分布式锁冲突异常
 *
 */
public class GlobalLockConflictException extends RuntimeException {

   
    private static final long serialVersionUID = -4247606794782331656L;

    public GlobalLockConflictException(String lockPath) {
        super("获取全局锁失败,锁路径为:"+lockPath);
    }

}
