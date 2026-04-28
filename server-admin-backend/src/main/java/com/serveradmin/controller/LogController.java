package com.serveradmin.controller;

import com.serveradmin.common.Result;
import com.serveradmin.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/read")
    public Result<String> readLog(
            @RequestParam String serverId,
            @RequestParam String path,
            @RequestParam(defaultValue = "100") Integer lines) {
        try {
            String content = logService.readLog(serverId, path, lines);
            return Result.success(content);
        } catch (Exception e) {
            return Result.error("读取日志失败: " + e.getMessage());
        }
    }
}
