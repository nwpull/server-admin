package com.serveradmin.service.impl;

import com.serveradmin.service.MonitorService;
import com.serveradmin.service.SshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final SshService sshService;

    @Override
    public Map<String, Object> getSystemInfo(String serverId) throws Exception {
        Map<String, Object> info = new HashMap<>();

        // CPU 使用率
        String cpuOutput = sshService.execCommand(serverId,
                "top -bn1 | grep 'Cpu(s)' | awk '{print $2}' | cut -d'%' -f1");
        double cpuUsage = 0;
        try {
            cpuUsage = Double.parseDouble(cpuOutput.trim());
        } catch (NumberFormatException ignored) {
        }
        info.put("cpuUsage", cpuUsage);

        // 内存信息
        String memOutput = sshService.execCommand(serverId,
                "free -m | awk 'NR==2{print $2,$3,$4,$6,$7}'");
        String[] memParts = memOutput.trim().split("\\s+");
        Map<String, Object> memory = new HashMap<>();
        try {
            memory.put("total", Long.parseLong(memParts[0]));
            memory.put("used", Long.parseLong(memParts[1]));
            memory.put("free", Long.parseLong(memParts[2]));
            memory.put("buffers", Long.parseLong(memParts[3]));
            memory.put("cached", Long.parseLong(memParts[4]));
            memory.put("usagePercent", Double.parseDouble(memParts[1]) * 100.0 / Double.parseDouble(memParts[0]));
        } catch (Exception e) {
            memory.put("total", 0);
            memory.put("used", 0);
            memory.put("free", 0);
            memory.put("usagePercent", 0);
        }
        info.put("memory", memory);

        // 磁盘信息
        String diskOutput = sshService.execCommand(serverId,
                "df -h | awk 'NR>1{print $1,$2,$3,$4,$5,$6}'");
        java.util.List<Map<String, String>> disks = new java.util.ArrayList<>();
        String[] diskLines = diskOutput.split("\n");
        for (String diskLine : diskLines) {
            diskLine = diskLine.trim();
            if (diskLine.isEmpty()) continue;
            String[] dp = diskLine.split("\\s+");
            if (dp.length >= 6) {
                Map<String, String> disk = new HashMap<>();
                disk.put("filesystem", dp[0]);
                disk.put("size", dp[1]);
                disk.put("used", dp[2]);
                disk.put("available", dp[3]);
                disk.put("usage", dp[4]);
                disk.put("mount", dp[5]);
                disks.add(disk);
            }
        }
        info.put("disks", disks);

        // 负载
        String loadOutput = sshService.execCommand(serverId, "cat /proc/loadavg | awk '{print $1,$2,$3}'");
        String[] loadParts = loadOutput.trim().split("\\s+");
        Map<String, Object> load = new HashMap<>();
        try {
            load.put("load1", Double.parseDouble(loadParts[0]));
            load.put("load5", Double.parseDouble(loadParts[1]));
            load.put("load15", Double.parseDouble(loadParts[2]));
        } catch (Exception e) {
            load.put("load1", 0);
            load.put("load5", 0);
            load.put("load15", 0);
        }
        info.put("load", load);

        // 网络流量 (通过 /proc/net/dev)
        String netOutput = sshService.execCommand(serverId,
                "cat /proc/net/dev | awk 'NR>2{print $1,$2,$10}' | grep -v lo:");
        java.util.List<Map<String, Object>> networks = new java.util.ArrayList<>();
        String[] netLines = netOutput.split("\n");
        for (String netLine : netLines) {
            netLine = netLine.trim();
            if (netLine.isEmpty()) continue;
            String[] np = netLine.split("\\s+");
            if (np.length >= 3) {
                Map<String, Object> net = new HashMap<>();
                net.put("interface", np[0].replace(":", ""));
                try {
                    net.put("rxBytes", Long.parseLong(np[1]));
                    net.put("txBytes", Long.parseLong(np[2]));
                } catch (NumberFormatException e) {
                    net.put("rxBytes", 0);
                    net.put("txBytes", 0);
                }
                networks.add(net);
            }
        }
        info.put("networks", networks);

        // 运行时间
        String uptimeOutput = sshService.execCommand(serverId, "uptime -p 2>/dev/null || uptime");
        info.put("uptime", uptimeOutput.trim());

        return info;
    }
}
