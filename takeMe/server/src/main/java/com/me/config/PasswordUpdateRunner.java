
//密码更新器，将数据库内所有密码更新为加密后的123456以验证登录

//package com.me.init;
//
//import com.me.entity.Admin;
//import com.me.entity.User;
//import com.me.entity.Volunteer;
//import com.me.mapper.AdminMapper;
//import com.me.mapper.UserMapper;
//import com.me.mapper.VolunteerMapper;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Slf4j
//@Component
//public class PasswordUpdateRunner implements CommandLineRunner {
//
//    @Autowired
//    private AdminMapper adminMapper;
//    @Autowired
//    private UserMapper userMapper;
//    @Autowired
//    private VolunteerMapper volunteerMapper;
//    @Autowired
//    private PasswordEncoder passwordEncoder; // 你的 BCryptPasswordEncoder
//
//    @Override
//    public void run(String... args) throws Exception {
//        // 为了避免每次启动都执行，可以加一个标志（比如配置文件、系统属性或数据库标志）
//        // 这里简单起见：检查是否已经执行过，根据业务自己决定。以下只执行一次，注释即可。
//        // 或者判断数据库里是否还有明文（长度<60或不以$2开头）
//
//        log.info("开始更新密码为 BCrypt 哈希...");
//
//        // 1. 更新 Admin 表
//        List<Admin> admins = adminMapper.selectList(null);
//        for (Admin admin : admins) {
//            String rawPassword = "123456";
//            if (needUpdate(admin.getPassword())) {
//                String encoded = passwordEncoder.encode(rawPassword);
//                admin.setPassword(encoded);
//                adminMapper.updateById(admin);
//                log.info("更新 Admin id={} 的密码", admin.getId());
//            }
//        }
//
//        // 2. 更新 User 表
//        List<User> users = userMapper.selectList(null);
//        for (User user : users) {
//            if (needUpdate(user.getPassword())) {
//                String encoded = passwordEncoder.encode("123456");
//                user.setPassword(encoded);
//                userMapper.updateById(user);
//                log.info("更新 User id={} 的密码", user.getId());
//            }
//        }
//
//        // 3. 更新 Volunteer 表
//        List<Volunteer> volunteers = volunteerMapper.selectList(null);
//        for (Volunteer volunteer : volunteers) {
//            if (needUpdate(volunteer.getPassword())) {
//                String encoded = passwordEncoder.encode("123456");
//                volunteer.setPassword(encoded);
//                volunteerMapper.updateById(volunteer);
//                log.info("更新 Volunteer id={} 的密码", volunteer.getId());
//            }
//        }
//
//        log.info("密码更新完成（如果无输出表示无需更新）。");
//    }
//
//    /**
//     * 判断是否需要更新：密码为空、长度小于60、或者不以$2a/$2b/$2y开头
//     */
//    private boolean needUpdate(String password) {
//        if (password == null || password.length() < 60) {
//            return true;
//        }
//        return !(password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
//    }
//}