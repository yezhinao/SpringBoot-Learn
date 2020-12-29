package com.example.mybatis.mapper;

import com.example.mybatis.entity.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface UserMapper {


    User findByName(@Param("name") String name);

    int insert(@Param("name") String name, @Param("age") Integer age);
}