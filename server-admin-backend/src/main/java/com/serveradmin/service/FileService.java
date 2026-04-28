package com.serveradmin.service;

import java.util.List;
import java.util.Map;

public interface FileService {

    List<Map<String, Object>> listFiles(String serverId, String path) throws Exception;

    void uploadFile(String serverId, String path, String filename, byte[] content) throws Exception;

    byte[] downloadFile(String serverId, String path) throws Exception;

    void mkdir(String serverId, String path) throws Exception;

    void delete(String serverId, String path) throws Exception;

    void rename(String serverId, String oldPath, String newPath) throws Exception;

    String readFile(String serverId, String path) throws Exception;

    void writeFile(String serverId, String path, String content) throws Exception;
}
