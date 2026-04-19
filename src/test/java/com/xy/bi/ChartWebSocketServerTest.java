package com.xy.bi;

import com.xy.bi.model.vo.ChartWsMsgVO;
import com.xy.bi.websocket.ChartWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * WebSocket 推送测试类
 */
@SpringBootTest
@Slf4j
public class ChartWebSocketServerTest {

    /**
     * 测试推送图表生成结果给前端
     */
    @Test
    public void testPushChartResult() {
        // 模拟推送消息
        Long userId = 2L;           // 用户ID（需要替换成实际登录的用户ID）
        Long chartId = 1001L;       // 图表ID
        String status = "success";  // 状态：success/failed
        String msg = "图表生成成功"; // 消息内容

        log.info("开始测试推送消息: userId={}, chartId={}, status={}, msg={}", 
                userId, chartId, status, msg);

        // 调用推送方法
        ChartWebSocketServer.pushChartResult(userId, chartId, status, msg);

        log.info("推送测试完成");
    }

    /**
     * 测试推送失败消息
     */
    @Test
    public void testPushChartResultFailed() {
        Long userId = 2L;
        Long chartId = 1001L;
        String status = "failed";
        String msg = "图表生成失败：AI处理超时";

        log.info("测试推送失败消息");
        ChartWebSocketServer.pushChartResult(userId, chartId, status, msg);
    }

    /**
     * 测试 userId 为空的情况
     */
    @Test
    public void testPushWithNullUserId() {
        log.info("测试 userId 为空的情况");
        ChartWebSocketServer.pushChartResult(null, 1001L, "failed", "userId为空");
    }

    /**
     * 测试用户不在线的情况
     */
    @Test
    public void testPushUserOffline() {
        log.info("测试用户不在线的情况");
        ChartWebSocketServer.pushChartResult(99999L, 1001L, "succeed", "用户不在线");
    }
}
