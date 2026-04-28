package com.serveradmin.controller;

import com.serveradmin.common.Result;
import com.serveradmin.service.SystemService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final SystemService systemService;

    @GetMapping
    public Result<List<Map<String, Object>>> listServices(
            @RequestParam String serverId,
            @RequestParam(required = false) String filter) {
        try {
            List<Map<String, Object>> services = systemService.listServices(serverId, filter);
            return Result.success(services);
        } catch (Exception e) {
            return Result.error("获取服务列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/action")
    public Result<Void> serviceAction(@RequestParam String serverId, @RequestBody ServiceActionRequest request) {
        try {
            systemService.serviceAction(serverId, request.getName(), request.getAction());
            return Result.success();
        } catch (Exception e) {
            return Result.error("服务操作失败: " + e.getMessage());
        }
    }

    @Data
    public static class ServiceActionRequest {
        private String name;
        private String action;
    }
}
