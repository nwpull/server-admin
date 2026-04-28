package com.serveradmin.service.impl;

import com.serveradmin.service.LogService;
import com.serveradmin.service.SshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    private final SshService sshService;

    @Override
    public String readLog(String serverId, String path, int lines) throws Exception {
        String command = "tail -n " + lines + " " + path;
        return sshService.execCommand(serverId, command);
    }
}
