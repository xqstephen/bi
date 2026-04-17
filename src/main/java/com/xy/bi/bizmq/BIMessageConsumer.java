package com.xy.bi.bizmq;

import com.rabbitmq.client.Channel;
import com.xy.bi.common.ErrorCode;
import com.xy.bi.constant.BiMqConstant;
import com.xy.bi.exception.BusinessException;
import com.xy.bi.manager.AiManager;
import com.xy.bi.model.dto.chart.GenChartByAiRequest;
import com.xy.bi.model.entity.Chart;
import com.xy.bi.service.ChartService;
import com.xy.bi.websocket.ChartWebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

import static com.xy.bi.model.enums.ChartStatusEnum.*;

/**
 * BI消息消费者
 * @author 25133
 */

@Component
@Slf4j
public class BIMessageConsumer {

    @Resource
    private ChartService chartService;
    @Resource
    private AiManager aiManager;
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive message: {}", message);
        try {
            // 消息为空就消费失败
            if (StringUtils.isBlank(message)){
                throwExceptionAndNackMessage(channel,deliveryTag);
            }
            //解析id,获取csv数据
            long chartId= Long.parseLong(message);
            Chart chart = chartService.getById(chartId);
            String chartData = chart.getChartData();
            if (StringUtils.isBlank(chartData)){
                throwExceptionAndNackMessage(channel,deliveryTag);
            }
            //构建GenChartByAiRequest
            GenChartByAiRequest genChartByAiRequest = new GenChartByAiRequest();
            genChartByAiRequest.setName(chart.getName());
            genChartByAiRequest.setGoal(chart.getGoal());
            genChartByAiRequest.setChartType(chart.getChartType());
            //获取输入提示词
            String userInput = chartService.buildAiUserInput(chartData, genChartByAiRequest);
            //异步生成图表
            String res = aiManager.doChat(userInput);
            String[] split = res.split("【【【【【");
            if (split.length < 3) {
                handleChartUpdateError(chartId,"AI生成错误");
            }
            String genChart = split[1].trim();
            String genResult = split[2].trim();

            int firstEndIndex = genResult.indexOf("\"},");
            if (firstEndIndex > 0) {
                genResult = genResult.substring(0, firstEndIndex).trim();
            }
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(SUCCESS.getStatus());
            boolean success = chartService.updateById(updateChartResult);
            if (success){
                ChartWebSocketServer.pushChartResult(chart.getUserId(),chartId,SUCCESS.getStatus(),"图表生成成功");
                channel.basicAck(deliveryTag, false);  // 确认消息
            }else {
                handleChartUpdateError(chartId,"更新图表失败");
                throwExceptionAndNackMessage(channel,deliveryTag);
            }
        } catch (IOException e) {
            log.error("消息队列异步出错,{}",e);
        }
    }
    /**
     * 抛异常同时拒绝消息
     *
     * @param channel
     * @param deliveryTag
     */
    private void throwExceptionAndNackMessage(Channel channel, long deliveryTag) {
        try {
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR);
    }

    private void handleChartUpdateError(Long id,String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(id);
        updateChart.setStatus(FAILED.getStatus());
        updateChart.setExecMessage(execMessage);
        boolean updateResult = chartService.updateById(updateChart);
        ChartWebSocketServer.pushChartResult(updateChart.getUserId(),id,FAILED.getStatus(),"图表生成失败");
        if (!updateResult){
            log.error("更新图表状态失败{},{}", id, execMessage);
        }
    }
}
