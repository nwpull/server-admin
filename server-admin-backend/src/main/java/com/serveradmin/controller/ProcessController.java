package com.serveradmin.controller;

import com.serveradmin.common.Result;
import com.serveradmin.service.ProcessService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;

    @GetMapping
    public Result<List<Map<String, Object>>> listProcesses(@RequestParam String serverId) {
        try {
            List<Map<String, Object>> processes = processService.listProcesses(serverId);
            return Result.success(processes);
        } catch (Exception e) {
            return Result.error("获取进程列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/kill")
    public Result<Void> killProcess(@RequestParam String serverId, @RequestBody KillRequest request) {
        try {
            processService.killProcess(serverId, request.getPid(), request.getSignal());
            return Result.success();
        } catch (Exception e) {
            return Result.error("终止进程失败: " + e.getMessage());
        }
    }

    @Data
    public static class KillRequest {
        private String pid;
        private String signal;
    }
}
