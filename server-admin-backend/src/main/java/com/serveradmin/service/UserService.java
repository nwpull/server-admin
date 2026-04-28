package com.serveradmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.serveradmin.entity.User;

public interface UserService extends IService<User> {

    String login(String username, String password);
}
