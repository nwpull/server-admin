package com.serveradmin.controller;

import com.serveradmin.common.Result;
import com.serveradmin.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;

    @GetMapping("/info")
    public Result<Map<String, Object>> getSystemInfo(@RequestParam String serverId) {
        try {
            Map<String, Object> info = monitorService.getSystemInfo(serverId);
            return Result.success(info);
        } catch (Exception e) {
            return Result.error("获取系统信息失败: " + e.getMessage());
        }
    }
}
