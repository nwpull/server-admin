package com.serveradmin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.serveradmin.common.PageResult;
import com.serveradmin.common.Result;
import com.serveradmin.entity.Server;
import com.serveradmin.service.ServerService;
import com.serveradmin.service.SshService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;
    private final SshService sshService;

    @GetMapping
    public Result<PageResult<Server>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String groupName,
            @RequestParam(required = false) String keyword) {

        Page<Server> page = new Page<>(current, size);
        LambdaQueryWrapper<Server> wrapper = new LambdaQueryWrapper<>();

        if (groupName != null && !groupName.isEmpty()) {
            wrapper.eq(Server::getGroupName, groupName);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Server::getName, keyword)
                    .or().like(Server::getHost, keyword));
        }
        wrapper.orderByDesc(Server::getCreatedAt);

        Page<Server> result = serverService.page(page, wrapper);
        PageResult<Server> pageResult = new PageResult<>(
                result.getRecords(), result.getTotal(), result.getCurrent(), result.getSize());
        return Result.success(pageResult);
    }

    @GetMapping("/{id}")
    public Result<Server> getById(@PathVariable String id) {
        Server server = serverService.getById(id);
        if (server == null) {
            return Result.error(404, "服务器不存在");
        }
        // 不返回密码和私钥
        server.setPassword(null);
        server.setPrivateKey(null);
        return Result.success(server);
    }

    @PostMapping
    public Result<Server> create(@RequestBody Server server) {
        // 加密密码
        if (server.getPassword() != null && !server.getPassword().isEmpty()) {
            server.setPassword(sshService.encryptPassword(server.getPassword()));
        }
        // 加密私钥
        if (server.getPrivateKey() != null && !server.getPrivateKey().isEmpty()) {
            server.setPrivateKey(sshService.encryptPassword(server.getPrivateKey()));
        }
        serverService.save(server);
        // 返回时隐藏敏感信息
        server.setPassword(null);
        server.setPrivateKey(null);
        return Result.success(server);
    }

    @PutMapping("/{id}")
    public Result<Server> update(@PathVariable String id, @RequestBody Server server) {
        Server existing = serverService.getById(id);
        if (existing == null) {
            return Result.error(404, "服务器不存在");
        }
        server.setId(id);
        // 加密密码（如果提供了新密码）
        if (server.getPassword() != null && !server.getPassword().isEmpty()) {
            server.setPassword(sshService.encryptPassword(server.getPassword()));
        } else {
            server.setPassword(existing.getPassword());
        }
        // 加密私钥
        if (server.getPrivateKey() != null && !server.getPrivateKey().isEmpty()) {
            server.setPrivateKey(sshService.encryptPassword(server.getPrivateKey()));
        } else {
            server.setPrivateKey(existing.getPrivateKey());
        }
        serverService.updateById(server);
        server.setPassword(null);
        server.setPrivateKey(null);
        return Result.success(server);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        serverService.removeById(id);
        sshService.closeConnection(id);
        return Result.success();
    }

    @PutMapping("/{id}/test")
    public Result<Boolean> testConnection(@PathVariable String id) {
        boolean success = serverService.testConnection(id);
        return Result.success(success);
    }
}
