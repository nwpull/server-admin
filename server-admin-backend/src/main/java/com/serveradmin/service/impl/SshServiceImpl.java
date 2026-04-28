package com.serveradmin.service.impl;

import com.jcraft.jsch.*;
import com.serveradmin.common.BusinessException;
import com.serveradmin.entity.Server;
import com.serveradmin.service.ServerService;
import com.serveradmin.service.SshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SshServiceImpl implements SshService {

    @Value("${encrypt.key}")
    private String encryptKey;

    private final Map<String, Session> sessionPool = new ConcurrentHashMap<>();
    private final ServerService serverService;

    public SshServiceImpl(ServerService serverService) {
        this.serverService = serverService;
    }

    @Override
    public Session getConnection(Server server) throws Exception {
        String key = server.getId();
        Session session = sessionPool.get(key);

        if (session != null && session.isConnected()) {
            return session;
        }

        JSch jsch = new JSch();
        session = jsch.getSession(server.getUsername(), server.getHost(), server.getPort());

        if ("password".equals(server.getAuthType())) {
            String password = decryptPassword(server.getPassword());
            session.setPassword(password);
        } else if ("key".equals(server.getAuthType())) {
            String privateKey = server.getPrivateKey();
            if (privateKey != null && !privateKey.isEmpty()) {
                // 私钥可能已加密存储
                String keyContent = privateKey;
                if (keyContent.startsWith("ENC:")) {
                    keyContent = decryptPassword(keyContent.substring(4));
                }
                jsch.addIdentity("server-key", keyContent.getBytes(StandardCharsets.UTF_8), null, null);
            }
        }

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect(10000);

        sessionPool.put(key, session);
        log.info("SSH 连接成功: {}@{}:{}", server.getUsername(), server.getHost(), server.getPort());
        return session;
    }

    @Override
    public Session getConnection(String serverId) throws Exception {
        Server server = serverService.getById(serverId);
        if (server == null) {
            throw new BusinessException("服务器不存在: " + serverId);
        }
        return getConnection(server);
    }

    @Override
    public String execCommand(Server server, String command) throws Exception {
        Session session = getConnection(server);
        ChannelExec channel = null;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            ByteArrayOutputStream errStream = new ByteArrayOutputStream();
            channel.setExtOutputStream(errStream);

            InputStream in = channel.getInputStream();
            channel.connect(5000);

            byte[] buffer = new byte[4096];
            StringBuilder output = new StringBuilder();
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                output.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
            }

            String errorOutput = errStream.toString(StandardCharsets.UTF_8);
            if (!errorOutput.isEmpty()) {
                output.append("\n").append(errorOutput);
            }

            return output.toString().trim();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public String execCommand(String serverId, String command) throws Exception {
        Server server = serverService.getById(serverId);
        if (server == null) {
            throw new BusinessException("服务器不存在: " + serverId);
        }
        return execCommand(server, command);
    }

    @Override
    public ChannelSftp createSftpChannel(Server server) throws Exception {
        Session session = getConnection(server);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect(5000);
        return channel;
    }

    @Override
    public ChannelSftp createSftpChannel(String serverId) throws Exception {
        Server server = serverService.getById(serverId);
        if (server == null) {
            throw new BusinessException("服务器不存在: " + serverId);
        }
        return createSftpChannel(server);
    }

    @Override
    public ChannelShell createShellChannel(Server server) throws Exception {
        Session session = getConnection(server);
        ChannelShell channel = (ChannelShell) session.openChannel("shell");
        channel.setPty(true);
        channel.connect(5000);
        return channel;
    }

    @Override
    public ChannelShell createShellChannel(String serverId) throws Exception {
        Server server = serverService.getById(serverId);
        if (server == null) {
            throw new BusinessException("服务器不存在: " + serverId);
        }
        return createShellChannel(server);
    }

    @Override
    public void closeConnection(String serverId) {
        Session session = sessionPool.remove(serverId);
        if (session != null && session.isConnected()) {
            session.disconnect();
            log.info("SSH 连接已关闭: serverId={}", serverId);
        }
    }

    @Override
    public void closeAllConnections() {
        sessionPool.forEach((id, session) -> {
            if (session.isConnected()) {
                session.disconnect();
            }
        });
        sessionPool.clear();
        log.info("所有 SSH 连接已关闭");
    }

    @Override
    public String decryptPassword(String encryptedPassword) {
        try {
            if (encryptedPassword == null || encryptedPassword.isEmpty()) {
                return "";
            }
            if (!encryptedPassword.startsWith("ENC:")) {
                return encryptedPassword;
            }
            String encoded = encryptedPassword.substring(4);
            byte[] decoded = Base64.getDecoder().decode(encoded);
            SecretKeySpec keySpec = new SecretKeySpec(
                    encryptKey.getBytes(StandardCharsets.UTF_8).clone(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("密码解密失败", e);
            throw new BusinessException("密码解密失败");
        }
    }

    @Override
    public String encryptPassword(String plainPassword) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    encryptKey.getBytes(StandardCharsets.UTF_8).clone(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
            return "ENC:" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("密码加密失败", e);
            throw new BusinessException("密码加密失败");
        }
    }
}
