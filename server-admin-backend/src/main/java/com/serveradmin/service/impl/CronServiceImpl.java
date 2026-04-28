package com.serveradmin.service.impl;

import com.serveradmin.service.CronService;
import com.serveradmin.service.SshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CronServiceImpl implements CronService {

    private final SshService sshService;

    @Override
    public List<String> listCrontab(String serverId) throws Exception {
        String output = sshService.execCommand(serverId, "crontab -l 2>/dev/null");
        List<String> entries = new ArrayList<>();
        if (output != null && !output.isEmpty()) {
            String[] lines = output.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    entries.add(line);
                }
            }
        }
        return entries;
    }

    @Override
    public void addCrontab(String serverId, String entry) throws Exception {
        String currentCrontab = sshService.execCommand(serverId, "crontab -l 2>/dev/null");
        String newCrontab = (currentCrontab == null || currentCrontab.isEmpty())
                ? entry
                : currentCrontab + "\n" + entry;
        sshService.execCommand(serverId, "echo '" + newCrontab.replace("'", "'\\''") + "' | crontab -");
    }

    @Override
    public void editCrontab(String serverId, int index, String entry) throws Exception {
        List<String> entries = listCrontab(serverId);
        if (index < 0 || index >= entries.size()) {
            throw new IllegalArgumentException("索引越界: " + index);
        }
        entries.set(index, entry);
        String newCrontab = String.join("\n", entries);
        sshService.execCommand(serverId, "echo '" + newCrontab.replace("'", "'\\''") + "' | crontab -");
    }

    @Override
    public void deleteCrontab(String serverId, int index) throws Exception {
        List<String> entries = listCrontab(serverId);
        if (index < 0 || index >= entries.size()) {
            throw new IllegalArgumentException("索引越界: " + index);
        }
        entries.remove(index);
        String newCrontab = entries.isEmpty() ? "" : String.join("\n", entries);
        sshService.execCommand(serverId, "echo '" + newCrontab.replace("'", "'\\''") + "' | crontab -");
    }
}
