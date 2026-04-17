package com.xy.bi.bizmq;

import com.rabbitmq.client.Channel;
import com.xy.bi.common.ErrorCode;
import com.xy.bi.constant.BiMqConstant;
import com.xy.bi.exception.BusinessException;
import com.xy.bi.manager.AiManager;
import com.xy.bi.model.entity.Chart;
import com.xy.bi.service.ChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

import static com.xy.bi.model.enums.ChartStatusEnum.*;

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
            //异步生成图表
            String res = aiManager.doChat(chartData);
            String[] split = res.split("【【【【【");
            if (split.length < 3) {
                handleChartUpdateError(chartId,"AI生成错误");
            }
            String genChart = split[1].trim();
            String genResult = split[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(SUCCESS.getStatus());
            boolean b1 = chartService.updateById(updateChartResult);
            if (!b1) {
                channel.basicNack(deliveryTag,false,false);
                handleChartUpdateError(chartId,"更新图表成功状态失败");
            }
            channel.basicAck(deliveryTag, false);  // 确认消息
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
        if (!updateResult){
            log.error("更新图表状态失败{},{}", id, execMessage);
        }
    }
}
