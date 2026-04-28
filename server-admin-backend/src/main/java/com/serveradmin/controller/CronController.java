package com.serveradmin.controller;

import com.serveradmin.common.Result;
import com.serveradmin.service.CronService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cron")
@RequiredArgsConstructor
public class CronController {

    private final CronService cronService;

    @GetMapping
    public Result<List<String>> listCrontab(@RequestParam String serverId) {
        try {
            List<String> entries = cronService.listCrontab(serverId);
            return Result.success(entries);
        } catch (Exception e) {
            return Result.error("获取定时任务失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<Void> addCrontab(@RequestParam String serverId, @RequestBody CronEntryRequest request) {
        try {
            cronService.addCrontab(serverId, request.getEntry());
            return Result.success();
        } catch (Exception e) {
            return Result.error("添加定时任务失败: " + e.getMessage());
        }
    }

    @PutMapping
    public Result<Void> editCrontab(
            @RequestParam String serverId,
            @RequestParam Integer index,
            @RequestBody CronEntryRequest request) {
        try {
            cronService.editCrontab(serverId, index, request.getEntry());
            return Result.success();
        } catch (Exception e) {
            return Result.error("编辑定时任务失败: " + e.getMessage());
        }
    }

    @DeleteMapping
    public Result<Void> deleteCrontab(@RequestParam String serverId, @RequestParam Integer index) {
        try {
            cronService.deleteCrontab(serverId, index);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除定时任务失败: " + e.getMessage());
        }
    }

    @Data
    public static class CronEntryRequest {
        private String entry;
    }
}
