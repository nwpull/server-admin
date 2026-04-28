package com.serveradmin.websocket;

import com.serveradmin.service.SshService;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ServerEndpoint(value = "/ws/log", configurator = SpringWebSocketConfigurator.class)
public class LogWebSocket {

    private String serverId;
    private String filePath;
    private Thread readThread;
    private volatile boolean running = false;

    @OnOpen
    public void onOpen(Session session,
                       @PathParam("serverId") String serverId,
                       @PathParam("filePath") String filePath,
                       @PathParam("token") String token) {
        this.serverId = serverId;
        this.filePath = filePath;
        this.running = true;

        try {
            SshService sshService = SpringContextHolder.getBean(SshService.class);

            // 使用 tail -f 实时读取日志
            com.jcraft.jsch.Session sshSession = sshService.getConnection(serverId);
            com.jcraft.jsch.ChannelExec channel = (com.jcraft.jsch.ChannelExec) sshSession.openChannel("exec");
            channel.setCommand("tail -f " + filePath);
            InputStream in = channel.getInputStream();
            channel.connect(5000);

            this.readThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        if (session.isOpen()) {
                            session.getAsyncRemote().sendText(line + "\n");
                        } else {
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.debug("日志读取线程结束: {}", e.getMessage());
                } finally {
                    channel.disconnect();
                }
            });
            this.readThread.setDaemon(true);
            this.readThread.start();

            log.info("Log WebSocket 已连接: serverId={}, filePath={}", serverId, filePath);
        } catch (Exception e) {
            log.error("Log WebSocket 连接失败: serverId={}, filePath={}", serverId, filePath, e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.getMessage()));
            } catch (Exception ignored) {
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // 日志 WebSocket 不需要处理客户端消息
    }

    @OnClose
    public void onClose(Session session) {
        cleanup();
        log.info("Log WebSocket 已断开: serverId={}, filePath={}", serverId, filePath);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Log WebSocket 错误: serverId={}, filePath={}", serverId, filePath, error);
        cleanup();
    }

    private void cleanup() {
        running = false;
        if (readThread != null) {
            readThread.interrupt();
        }
    }
}
