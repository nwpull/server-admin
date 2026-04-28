package com.serveradmin.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serveradmin.service.MonitorService;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ServerEndpoint(value = "/ws/monitor", configurator = SpringWebSocketConfigurator.class)
public class MonitorWebSocket {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ScheduledExecutorService scheduler;
    private String serverId;

    @OnOpen
    public void onOpen(Session session, @PathParam("serverId") String serverId,
                       @PathParam("token") String token) {
        this.serverId = serverId;
        try {
            MonitorService monitorService = SpringContextHolder.getBean(MonitorService.class);

            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            this.scheduler.scheduleAtFixedRate(() -> {
                try {
                    Map<String, Object> info = monitorService.getSystemInfo(serverId);
                    String json = objectMapper.writeValueAsString(info);
                    session.getAsyncRemote().sendText(json);
                } catch (Exception e) {
                    log.error("推送监控数据失败: serverId={}", serverId, e);
                }
            }, 0, 3, TimeUnit.SECONDS);

            log.info("Monitor WebSocket 已连接: serverId={}", serverId);
        } catch (Exception e) {
            log.error("Monitor WebSocket 连接失败: serverId={}", serverId, e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, e.getMessage()));
            } catch (Exception ignored) {
            }
        }
    }

    @OnClose
    public void onClose(Session session) {
        cleanup();
        log.info("Monitor WebSocket 已断开: serverId={}", serverId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("Monitor WebSocket 错误: serverId={}", serverId, error);
        cleanup();
    }

    private void cleanup() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
