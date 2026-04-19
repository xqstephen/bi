package com.xy.bi.websocket;

import cn.hutool.json.JSONUtil;
import com.xy.bi.model.vo.ChartWsMsgVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务
 *
 * @author 25133
 */
@Component
@ServerEndpoint("/ws/chart/{userId}")
@Slf4j
public class ChartWebSocketServer {

    //  userId -> WebSocket 会话
    public static final ConcurrentHashMap<Long, Session> USER_SESSION_MAP = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("userId") Long userId) {
        USER_SESSION_MAP.put(userId, session);
        log.info("WebSocket 用户 {} 连接成功", userId);
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId) {
        USER_SESSION_MAP.remove(userId);
        log.info("WebSocket 用户 {} 断开连接", userId);
    }

    @OnError
    public void onError(Session session, Throwable e) {
        log.error("WebSocket 异常", e);
    }

    /**
     * 给指定用户推送图表生成结果
     */
    public static void pushChartResult(Long userId, Long chartId, String status, String msg) {
        if (userId == null) {
            log.warn("userId 为空，无法推送");
            return;
        }
        Session session = USER_SESSION_MAP.get(userId);
        if (session == null || !session.isOpen()) {
            log.warn("用户 {} 不在线，不推送", userId);
            return;
        }
        try {
            ChartWsMsgVO msgVO = new ChartWsMsgVO(chartId, status, msg);
            String json = JSONUtil.toJsonStr(msgVO);
            session.getBasicRemote().sendText(json);
            log.info("推送成功 userId:{} chartId:{} msg:{}", userId, chartId, msg);
        } catch (Exception e) {
            log.error("推送失败", e);
        }
    }
}