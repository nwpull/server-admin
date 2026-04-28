package com.serveradmin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.serveradmin.entity.Server;
import com.serveradmin.mapper.ServerMapper;
import com.serveradmin.service.ServerService;
import com.serveradmin.service.SshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerServiceImpl extends ServiceImpl<ServerMapper, Server> implements ServerService {

    private final SshService sshService;

    @Override
    public boolean testConnection(String serverId) {
        try {
            Server server = getById(serverId);
            if (server == null) {
                throw new RuntimeException("服务器不存在");
            }
            String result = sshService.execCommand(server, "echo 'connected'");
            boolean success = result != null && result.contains("connected");
            server.setStatus(success ? "online" : "offline");
            updateById(server);
            return success;
        } catch (Exception e) {
            log.error("测试连接失败: serverId={}", serverId, e);
            Server server = getById(serverId);
            if (server != null) {
                server.setStatus("offline");
                updateById(server);
            }
            return false;
        }
    }
}
