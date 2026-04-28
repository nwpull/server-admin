package com.serveradmin.service;

import java.util.List;
import java.util.Map;

public interface SystemService {

    List<Map<String, Object>> listServices(String serverId, String filter) throws Exception;

    void serviceAction(String serverId, String name, String action) throws Exception;
}
