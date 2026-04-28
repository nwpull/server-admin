package com.serveradmin.service;

import java.util.Map;

public interface MonitorService {

    Map<String, Object> getSystemInfo(String serverId) throws Exception;
}
