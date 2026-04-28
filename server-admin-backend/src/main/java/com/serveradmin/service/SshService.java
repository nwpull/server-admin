package com.serveradmin.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import com.serveradmin.entity.Server;

public interface SshService {

    Session getConnection(Server server) throws Exception;

    Session getConnection(String serverId) throws Exception;

    String execCommand(Server server, String command) throws Exception;

    String execCommand(String serverId, String command) throws Exception;

    ChannelSftp createSftpChannel(Server server) throws Exception;

    ChannelSftp createSftpChannel(String serverId) throws Exception;

    ChannelShell createShellChannel(Server server) throws Exception;

    ChannelShell createShellChannel(String serverId) throws Exception;

    void closeConnection(String serverId);

    void closeAllConnections();

    String decryptPassword(String encryptedPassword);

    String encryptPassword(String plainPassword);
}
