package com.serveradmin.service.impl;

import com.serveradmin.service.SshService;
import com.serveradmin.service.SystemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final SshService sshService;

    @Override
    public List<Map<String, Object>> listServices(String serverId, String filter) throws Exception {
        String command = "systemctl list-units --type=service --no-pager --no-legend";
        if (filter != null && !filter.isEmpty()) {
            command += " | grep -i " + filter;
        }
        String output = sshService.execCommand(serverId, command);
        List<Map<String, Object>> services = new ArrayList<>();

        String[] lines = output.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 格式: ● service.service    loaded active running   Description
            String[] parts = line.split("\\s+", 4);
            if (parts.length < 3) continue;

            Map<String, Object> service = new LinkedHashMap<>();
            service.put("name", parts[0].replace("●", "").trim());
            service.put("load", parts[1]);
            service.put("active", parts[2]);
            service.put("description", parts.length > 3 ? parts[3] : "");
            services.add(service);
        }

        return services;
    }

    @Override
    public void serviceAction(String serverId, String name, String action) throws Exception {
        String command;
        switch (action.toLowerCase()) {
            case "start":
                command = "sudo systemctl start " + name;
                break;
            case "stop":
                command = "sudo systemctl stop " + name;
                break;
            case "restart":
                command = "sudo systemctl restart " + name;
                break;
            case "enable":
                command = "sudo systemctl enable " + name;
                break;
            case "disable":
                command = "sudo systemctl disable " + name;
                break;
            default:
                throw new IllegalArgumentException("不支持的操作: " + action);
        }
        String output = sshService.execCommand(serverId, command);
        log.info("服务操作: serverId={}, name={}, action={}, output={}", serverId, name, action, output);
    }
}
