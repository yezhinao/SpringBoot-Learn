package com.example.mybatis;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//是用作日志输出的
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class MybatisApplicationTests {

    @Autowired
    private UserMapper userMapper;

    @Test
//   测试结束回滚数据，保证测试单元每次运行的数据环境独立
    @Transactional
    public void test() throws Exception {
        userMapper.insert("AAA", 20);
        User u = userMapper.findByName("AAA");
        Assert.assertEquals(20, u.getAge().intValue());
    }


    @Test
    @Transactional
    public void test2(){
        Map<String, Object> map = new HashMap<>();
        map.put("name", "BBB");
        map.put("age", 20);
        userMapper.insertByMap(map);
        User u = userMapper.findByName("BBB");
        Assert.assertEquals(20, u.getAge().intValue());
    }


    @Test
    @Transactional
    public void testUserMapper() throws Exception {
        // insert一条数据，并select出来验证
        userMapper.insert("AAA", 20);
        User u = userMapper.findByName("AAA");
        Assert.assertEquals(20, u.getAge().intValue());
        // update一条数据，并select出来验证
        u.setAge(30);
        userMapper.update(u);
        u = userMapper.findByName("AAA");
        Assert.assertEquals(30, u.getAge().intValue());
        // 删除这条数据，并select验证
        userMapper.delete(u.getId());
        u = userMapper.findByName("AAA");
        Assert.assertEquals(null, u);
    }

    @Test
    @Transactional
    public void testUserMapper2() throws Exception {
        List<User> userList = userMapper.findAll();
        for(User user : userList) {
            System.out.println(user);
            Assert.assertEquals(null, user.getId());
            Assert.assertNotEquals(null, user.getName());
        }
    }
}
