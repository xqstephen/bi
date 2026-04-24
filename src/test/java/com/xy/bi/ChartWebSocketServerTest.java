//package com.xy.bi;
//
//import com.xy.bi.model.vo.ChartWsMsgVO;
//import com.xy.bi.websocket.ChartWebSocketServer;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.boot.test.context.SpringBootTest;
//
///**
// * WebSocket ���Ͳ�����
// */
//@SpringBootTest
//@Slf4j
//public class ChartWebSocketServerTest {
//
//    /**
//     * ��������ͼ�����ɽ����ǰ��
//     */
//    @Test
//    public void testPushChartResult() {
//        // ģ��������Ϣ
//        Long userId = 2L;           // �û�ID����Ҫ�滻��ʵ�ʵ�¼���û�ID��
//        Long chartId = 1001L;       // ͼ��ID
//        String status = "success";  // ״̬��success/failed
//        String msg = "ͼ�����ɳɹ�"; // ��Ϣ����
//
//        log.info("��ʼ����������Ϣ: userId={}, chartId={}, status={}, msg={}",
//                userId, chartId, status, msg);
//
//        // �������ͷ���
//        ChartWebSocketServer.pushChartResult(userId, chartId, status, msg);
//
//        log.info("���Ͳ������");
//    }
//
//    /**
//     * ��������ʧ����Ϣ
//     */
//    @Test
//    public void testPushChartResultFailed() {
//        Long userId = 2L;
//        Long chartId = 1001L;
//        String status = "failed";
//        String msg = "ͼ������ʧ�ܣ�AI����ʱ";
//
//        log.info("��������ʧ����Ϣ");
//        ChartWebSocketServer.pushChartResult(userId, chartId, status, msg);
//    }
//
//    /**
//     * ���� userId Ϊ�յ����
//     */
//    @Test
//    public void testPushWithNullUserId() {
//        log.info("���� userId Ϊ�յ����");
//        ChartWebSocketServer.pushChartResult(null, 1001L, "failed", "userIdΪ��");
//    }
//
//    /**
//     * �����û������ߵ����
//     */
//    @Test
//    public void testPushUserOffline() {
//        log.info("�����û������ߵ����");
//        ChartWebSocketServer.pushChartResult(99999L, 1001L, "succeed", "�û�������");
//    }
//}
