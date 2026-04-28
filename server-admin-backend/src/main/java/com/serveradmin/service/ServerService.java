package com.serveradmin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.serveradmin.entity.Server;

public interface ServerService extends IService<Server> {

    boolean testConnection(String serverId);
}
