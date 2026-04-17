package com.xy.bi.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.bi.bizmq.BIMessageProducer;
import com.xy.bi.common.ErrorCode;
import com.xy.bi.exception.BusinessException;
import com.xy.bi.exception.ThrowUtils;
import com.xy.bi.manager.AiManager;
import com.xy.bi.model.dto.chart.GenChartByAiRequest;
import com.xy.bi.model.entity.Chart;
import com.xy.bi.model.entity.User;
import com.xy.bi.model.vo.BiResponse;
import com.xy.bi.service.ChartService;
import com.xy.bi.mapper.ChartMapper;
import com.xy.bi.service.UserService;
import com.xy.bi.utils.ExcelToCSVUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import static com.xy.bi.model.enums.ChartStatusEnum.*;

/**
 * 图表服务实现
 * @author 25133
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart> implements ChartService {

    private final UserService userService;
    private final AiManager aiManager;
    private final ChartMapper chartMapper;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final BIMessageProducer biMessageProducer;

    //常量定义
    private static final long MAX_FILE_SIZE = 1024L * 1024L;
    private static final String[] SUPPORTED_SUFFIXES = {"xlsx", "xls"};

    //同步生成
    @Transactional
    @Override
    public BiResponse genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        // 1. 公共参数校验 + 获取用户输入
        validChartParams(multipartFile,genChartByAiRequest);
        String csvData = extractCsvData(multipartFile);
        String userInput = buildAiUserInput(csvData, genChartByAiRequest);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();

        // 2. 调用AI
        String res = aiManager.doChat(userInput);
        String[] split = res.split("【【【【【");
        if (split.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成格式错误");
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();

        int firstEndIndex = genResult.indexOf("\"},");
        if (firstEndIndex > 0) {
            genResult = genResult.substring(0, firstEndIndex).trim();
        }
        // 3. 保存图表
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(userId);
        chart.setStatus(SUCCESS.getStatus());
        this.save(chart);

        // 4. 返回
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());
        return biResponse;
    }

    //异步生成
    @Override
    @Transactional
    public BiResponse genCharAsynctByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        // 公共校验 + 获取输入
        validChartParams(multipartFile,genChartByAiRequest);
        String csvData = extractCsvData(multipartFile);
        String userInput = buildAiUserInput(csvData, genChartByAiRequest);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();


        // 保存初始记录
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(userId);
        this.save(chart);
        Long chartId = chart.getId();

        // 异步执行
        CompletableFuture.runAsync(() -> {
            try {
                // 更新为执行中
                updateChartStatus(chartId, RUNNING.getStatus());

                // AI生成
                String res = aiManager.doChat(userInput);
                String[] split = res.split("【【【【【");
                if (split.length < 3) {
                    handleChartUpdateError(chartId, "AI返回格式错误");
                    return;
                }
                String genChart = split[1].trim();
                String genResult = split[2].trim();

                int firstEndIndex = genResult.indexOf("\"},");
                if (firstEndIndex > 0) {
                    genResult = genResult.substring(0, firstEndIndex).trim();
                }

                // 更新成功
                Chart updateChart = new Chart();
                updateChart.setId(chartId);
                updateChart.setGenChart(genChart);
                updateChart.setGenResult(genResult);
                updateChart.setStatus(SUCCESS.getStatus());
                boolean updateSuccess = this.updateById(updateChart);
                if (!updateSuccess) {
                    handleChartUpdateError(chartId, "更新图表结果失败");
                }
            } catch (Exception e) {
                log.error("异步生成图表异常，chartId:{}", chartId, e);
                handleChartUpdateError(chartId, "系统异常：" + e.getMessage());
            }
        }, threadPoolExecutor);

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        return biResponse;
    }

    //MQ生成
    @Override
    @Transactional
    public BiResponse genChartByAiMQ(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        validChartParams(multipartFile,genChartByAiRequest);
        String csvData = extractCsvData(multipartFile);
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();


        // 保存
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(userId);
        this.save(chart);
        Long chartId = chart.getId();

        // 更新状态为执行中
        updateChartStatus(chartId, RUNNING.getStatus());

        // 发送MQ
        biMessageProducer.sendMessage(String.valueOf(chartId));

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        return biResponse;
    }

    //构建给AI的用户输入
    @Override
    public String buildAiUserInput(String csvData, GenChartByAiRequest request) {
        //拼接AI输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        userInput.append(request.getGoal()).append("\n");
        userInput.append("原始数据：").append("\n");
        userInput.append(csvData).append("\n");
        return userInput.toString();
    }

    //参数校验
    private void validChartParams(MultipartFile file, GenChartByAiRequest request) {
        String goal = request.getGoal();
        String name = request.getName();

        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "分析目标不能为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() > 80, ErrorCode.PARAMS_ERROR, "图表名称不能超过80字符");
        ThrowUtils.throwIf(file.getSize() > MAX_FILE_SIZE, ErrorCode.PARAMS_ERROR, "文件不能超过1MB");

        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        ThrowUtils.throwIf(!ArrayUtils.contains(SUPPORTED_SUFFIXES, suffix), ErrorCode.PARAMS_ERROR, "仅支持xlsx/xls文件");
    }

    //获取CSV数据
    private String extractCsvData(MultipartFile file) {
        return ExcelToCSVUtils.excelToCSV(file);
    }

    //更新图表状态
    private void updateChartStatus(Long chartId, String status) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(status);
        boolean success = this.updateById(updateChart);
        if (!success) {
            handleChartUpdateError(chartId, "更新图表状态失败");
        }
    }

    //错误处理
    private void handleChartUpdateError(Long id, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(id);
        updateChart.setStatus(FAILED.getStatus());
        updateChart.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateChart);
        if (!updateResult) {
            log.error("更新图表失败状态失败，chartId:{}, execMessage:{}", id, execMessage);
        }
    }

    //分表存储CSV
    private void saveCsvData(Long chartId, String csvData) {
        String[] split = csvData.split("\n");
        ThrowUtils.throwIf(split.length == 0, ErrorCode.PARAMS_ERROR, "CSV数据不能为空");

        String[] columns = split[0].split(",");
        int expectedColumnCount = columns.length;
        String tableName = "chart_" + chartId;

        // 建表
        StringBuilder createSql = new StringBuilder();
        createSql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        for (int i = 0; i < columns.length; i++) {
            String col = columns[i].trim();
            if (StringUtils.isBlank(col)) {
                col = "column_" + i;
            }
            createSql.append("`").append(col).append("` VARCHAR(255)");
            if (i != columns.length - 1) {
                createSql.append(",");
            }
        }
        createSql.append(");");
        chartMapper.createTable(createSql.toString());

        // 插入数据
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO ").append(tableName).append(" VALUES ");
        boolean first = true;

        for (int i = 1; i < split.length; i++) {
            String line = split[i].trim();
            if (StringUtils.isBlank(line)) continue;

            String[] data = line.split(",", -1);
            if (!first) insertSql.append(",");
            first = false;

            insertSql.append("(");
            for (int j = 0; j < expectedColumnCount; j++) {
                if (j > 0) insertSql.append(",");
                if (j < data.length && StringUtils.isNotBlank(data[j])) {
                    String val = data[j].trim().replace("'", "''");
                    insertSql.append("'").append(val).append("'");
                } else {
                    insertSql.append("NULL");
                }
            }
            insertSql.append(")");
        }
        insertSql.append(";");
        chartMapper.insertData(insertSql.toString());
    }

    //查询图表数据
    private List<Map<String, Object>> queryChartData(Long chartId) {
        return chartMapper.queryData(chartId);
    }
}