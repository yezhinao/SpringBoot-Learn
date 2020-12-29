package com.example.mybatis;

import com.example.mybatis.entity.User;
import com.example.mybatis.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;



//是用作日志输出的
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
class MybatisApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
    @Rollback(true)
    public void test() throws Exception {
        userMapper.insert("AAA", 20);
        User u = userMapper.findByName("AAA");
        Assert.assertEquals(20, u.getAge().intValue());
    }
}
