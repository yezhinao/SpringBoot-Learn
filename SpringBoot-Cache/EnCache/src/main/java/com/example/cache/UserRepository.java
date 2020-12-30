package com.example.cache;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

//@CacheConfig：主要用于配置该类中会用到的一些共用的缓存配置。
// 在这里@CacheConfig(cacheNames = "users")：配置了该数据访问对象中返回的内容将存储于名为users的缓存对象中，
// 我们也可以不使用该注解，直接通过@Cacheable自己配置缓存集的名字来定义。
@CacheConfig(cacheNames = "users")
public interface UserRepository extends JpaRepository<User, Long> {

//    @Cacheable：配置了findByName函数的返回值将被加入缓存。同时在查询时，会先从缓存中获取，若不存在才再发起对数据库的访问。
    @Cacheable
    User findByName(String name);

    User findByNameAndAge(String name, Integer age);

    @Query("from User u where u.name=:name")
    User findUser(@Param("name") String name);

}