package com.serveradmin.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import com.serveradmin.service.SshService;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@ServerEndpoint(value = "/ws/terminal", configurator = SpringWebSocketConfigurator.class)
public class TerminalWebSocket {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Session sshSession;
    private ChannelShell shellChannel;
    private OutputStream shellOutput;
    private InputStream shellInput;
    private Thread readThread;
    private String serverId;

    @OnOpen
    public void onOpen(Session session, @PathParam("serverId") String serverId,
                       @PathParam("token") String token) {
        this.serverId = serverId;
        try {
            SshService sshService = SpringContextHolder.getBean(SshService.class);
            this.sshSession = sshService.getConnection(serverId);
            this.shellChannel = sshService.createShellChannel(serverId);

            this.shellOutput = shellChannel.getOutputStream();
            this.shellInput = shellChannel.getInputStream();

            // 启动读取线程
            this.readThread = new Thread(() -> {
                try {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = shellInput.read(buffer)) != -1) {
                        String data = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                        session.getAsyncRemote().sendText(data);
                    }
                } catch (Exception e) {
                    log.debug("Shell 读取线程结束: {}", e.getMessage());
                }
            });
            this.readThread.setDaemon(true);
            this.readThread.start();

            log.info("Terminal WebSocket 已连接: serverId={}", serverId);
        } catch (Exception e) {
            log.error("Terminal WebSocket 连接失败: serverId={}", serverId, e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.getMessage()));
            } catch (Exception ignored) {
            }
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonNode json = objectMapper.readTree(message);
            String type = json.has("type") ? json.get("type").asText() : "input";

            switch (type) {
                case "input":
                    String data = json.get("data").asText();
                    if (shellOutput != null) {
                        shellOutput.write(data.getBytes(StandardCharsets.UTF_8));
                        shellOutput.flush();
                    }
                    break;
                case "resize":
                    int cols = json.get("cols").asInt();
                    int rows = json.get("rows").asInt();
                    if (shellChannel != null) {
                        shellChannel.setPtySize(cols, rows, cols * 8, rows * 16);
                    }
                    break;
            }
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {
        cleanup();
        log.info("Terminal WebSocket 已断开: serverId={}", serverId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Terminal WebSocket 错误: serverId={}", serverId, error);
        cleanup();
    }

    private void cleanup() {
        if (readThread != null) {
            readThread.interrupt();
        }
        if (shellChannel != null) {
            shellChannel.disconnect();
        }
        // 注意：不关闭 sshSession，因为可能被其他 WebSocket 使用
    }
}
