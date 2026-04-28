package com.serveradmin.service;

import java.util.List;

public interface CronService {

    List<String> listCrontab(String serverId) throws Exception;

    void addCrontab(String serverId, String entry) throws Exception;

    void editCrontab(String serverId, int index, String entry) throws Exception;

    void deleteCrontab(String serverId, int index) throws Exception;
}
