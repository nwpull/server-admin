package com.serveradmin.service.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import com.serveradmin.service.FileService;
import com.serveradmin.service.SshService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final SshService sshService;

    @Override
    public List<Map<String, Object>> listFiles(String serverId, String path) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            Vector<ChannelSftp.LsEntry> entries = channel.ls(path);

            List<Map<String, Object>> files = new ArrayList<>();
            for (ChannelSftp.LsEntry entry : entries) {
                if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                    continue;
                }
                Map<String, Object> file = new HashMap<>();
                file.put("name", entry.getFilename());
                file.put("path", path.endsWith("/") ? path + entry.getFilename() : path + "/" + entry.getFilename());
                file.put("size", entry.getAttrs().getSize());
                file.put("isDirectory", entry.getAttrs().isDir());
                file.put("permissions", entry.getAttrs().getPermissionsString());
                file.put("modifiedTime", entry.getAttrs().getMtimeString());
                files.add(file);
            }
            return files;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void uploadFile(String serverId, String path, String filename, byte[] content) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            String fullPath = path.endsWith("/") ? path + filename : path + "/" + filename;
            channel.put(new java.io.ByteArrayInputStream(content), fullPath, ChannelSftp.OVERWRITE);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public byte[] downloadFile(String serverId, String path) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = channel.get(path);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void mkdir(String serverId, String path) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            channel.mkdir(path);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void delete(String serverId, String path) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            channel.rm(path);
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_FAILURE) {
                // 可能是目录，尝试递归删除
                String cmd = "rm -rf " + path;
                sshService.execCommand(serverId, cmd);
            } else {
                throw e;
            }
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void rename(String serverId, String oldPath, String newPath) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            channel.rename(oldPath, newPath);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public String readFile(String serverId, String path) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = channel.get(path);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toString("UTF-8");
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void writeFile(String serverId, String path, String content) throws Exception {
        ChannelSftp channel = null;
        try {
            channel = sshService.createSftpChannel(serverId);
            channel.put(new java.io.ByteArrayInputStream(content.getBytes("UTF-8")), path, ChannelSftp.OVERWRITE);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }
}
