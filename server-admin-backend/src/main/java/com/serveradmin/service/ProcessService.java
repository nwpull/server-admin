package com.serveradmin.service;

import java.util.List;
import java.util.Map;

public interface ProcessService {

    List<Map<String, Object>> listProcesses(String serverId) throws Exception;

    void killProcess(String serverId, String pid, String signal) throws Exception;
}
