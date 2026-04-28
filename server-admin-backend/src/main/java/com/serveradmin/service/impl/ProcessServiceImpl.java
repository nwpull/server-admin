package com.serveradmin.service.impl;

import com.serveradmin.service.ProcessService;
import com.serveradmin.service.SshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    private final SshService sshService;

    @Override
    public List<Map<String, Object>> listProcesses(String serverId) throws Exception {
        String output = sshService.execCommand(serverId, "ps aux --sort=-%mem | head -100");
        List<Map<String, Object>> processes = new ArrayList<>();
        String[] lines = output.split("\n");

        // 跳过标题行
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+", 11);
            if (parts.length < 11) continue;

            Map<String, Object> process = new LinkedHashMap<>();
            process.put("user", parts[0]);
            process.put("pid", parts[1]);
            process.put("cpu", parts[2]);
            process.put("mem", parts[3]);
            process.put("vsz", parts[4]);
            process.put("rss", parts[5]);
            process.put("tty", parts[6]);
            process.put("stat", parts[7]);
            process.put("start", parts[8]);
            process.put("time", parts[9]);
            process.put("command", parts[10]);
            processes.add(process);
        }

        return processes;
    }

    @Override
    public void killProcess(String serverId, String pid, String signal) throws Exception {
        String sig = signal != null && !signal.isEmpty() ? "-" + signal : "-15";
        String output = sshService.execCommand(serverId, "kill " + sig + " " + pid);
        log.info("Kill 进程: serverId={}, pid={}, signal={}, output={}", serverId, pid, signal, output);
    }
}
