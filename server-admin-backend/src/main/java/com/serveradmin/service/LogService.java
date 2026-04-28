package com.serveradmin.service;

public interface LogService {

    String readLog(String serverId, String path, int lines) throws Exception;
}
