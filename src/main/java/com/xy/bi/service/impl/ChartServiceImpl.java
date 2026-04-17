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
* @author 25133
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2026-04-07 08:44:11
*/
@Service
@RequiredArgsConstructor
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService{

    private final UserService userService;

    private final AiManager aiManager;
    private final ChartMapper chartMapper;

    private final ThreadPoolExecutor threadPoolExecutor;

    private final BIMessageProducer biMessageProducer;

    @Transactional
    @Override
    public BiResponse genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        //校验参数获取拼接的字符串
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();

        // 校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length() > 80, ErrorCode.PARAMS_ERROR, "名称过长");
        //内存大小校验
        final long size=1024L * 1024L;
        ThrowUtils.throwIf(multipartFile.getSize() > size, ErrorCode.PARAMS_ERROR, "文件过大");
        //支持的后缀
        final String[] SUFFIX_ARRAY = new String[]{"xlsx", "xls"};
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ArrayUtils.contains(SUFFIX_ARRAY, suffix), ErrorCode.PARAMS_ERROR, "不支持的文件类型");
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        userInput.append(goal).append("\n");
        //压缩后的数据
        String csvData = null;
        userInput.append("原始数据：").append("\n");
        if (suffix == null ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
        csvData = ExcelToCSVUtils.excelToCSV(multipartFile);
        userInput.append(csvData).append("\n");

        String res = aiManager.doChat(userInput.toString());
        String[] split = res.split("【【【【【");
        if (split.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
        }
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        //插入到数据库
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        //可改成单独保存
        chart.setChartData(csvData);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(userId);
        this.save(chart);
        //获取chartId,分表保存数据
        Long chartId = chart.getId();
//        saveCsvData(chartId, result.csvData);
        //封装返回
        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chartId);
        return biResponse;
    }



    @Override
    @Transactional
    public BiResponse genCharAsynctByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        //校验参数获取拼接的字符串
        //校验参数获取拼接的字符串
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();

        // 校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length() > 80, ErrorCode.PARAMS_ERROR, "名称过长");
        //内存大小校验
        final long size=1024L * 1024L;
        ThrowUtils.throwIf(multipartFile.getSize() > size, ErrorCode.PARAMS_ERROR, "文件过大");
        //支持的后缀
        final String[] SUFFIX_ARRAY = new String[]{"xlsx", "xls"};
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ArrayUtils.contains(SUFFIX_ARRAY, suffix), ErrorCode.PARAMS_ERROR, "不支持的文件类型");
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        userInput.append(goal).append("\n");
        //压缩后的数据
        String csvData = null;
        userInput.append("原始数据：").append("\n");
        if (suffix == null ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
        csvData = ExcelToCSVUtils.excelToCSV(multipartFile);
        userInput.append(csvData).append("\n");
        //先保存未生成完的图表的前置参数
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(userId);
        this.save(chart);
        Long chartId = chart.getId();
//        saveCsvData(chartId, result.csvData);
        //异步生成图表
        CompletableFuture.runAsync(()->{
            Chart updateChart = new Chart();
            updateChart.setId(chartId);
            updateChart.setStatus(RUNNING.getStatus());
            boolean b = this.updateById(updateChart);
            if (!b) {
                handleChartUpdateError(chartId,"更新图表状态失败");
                return;
            }
            String res = aiManager.doChat(userInput.toString());
            String[] split = res.split("【【【【【");
            if (split.length < 3) {
                handleChartUpdateError(chartId,"AI生成错误");
                return;
            }
            String genChart = split[1].trim();
            String genResult = split[2].trim();
            Chart updateChartResult = new Chart();
            updateChartResult.setId(chartId);
            updateChartResult.setGenChart(genChart);
            updateChartResult.setGenResult(genResult);
            updateChartResult.setStatus(SUCCESS.getStatus());
            boolean b1 = this.updateById(updateChartResult);
            if (!b1) {
                handleChartUpdateError(chartId,"更新图表成功状态失败");
                return;
            }
        },threadPoolExecutor);
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        return biResponse;
    }


    @Override
    @Transactional
    public BiResponse genChartByAiMQ(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser) {
        //校验参数获取拼接的字符串
        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        Long userId = loginUser.getId();

        // 校验参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name)&&name.length() > 80, ErrorCode.PARAMS_ERROR, "名称过长");
        //内存大小校验
        final long size=1024L * 1024L;
        ThrowUtils.throwIf(multipartFile.getSize() > size, ErrorCode.PARAMS_ERROR, "文件过大");
        //支持的后缀
        final String[] SUFFIX_ARRAY = new String[]{"xlsx", "xls"};
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ArrayUtils.contains(SUFFIX_ARRAY, suffix), ErrorCode.PARAMS_ERROR, "不支持的文件类型");
        //用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求：").append("\n");
        userInput.append(goal).append("\n");
        //压缩后的数据
        String csvData = null;
        userInput.append("原始数据：").append("\n");
        if (suffix == null ) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误");
        }
        csvData = ExcelToCSVUtils.excelToCSV(multipartFile);
        userInput.append(csvData).append("\n");
        //先保存未生成完的图表的前置参数
        Chart chart = new Chart();
        chart.setName(name);
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(userId);
        this.save(chart);
        Long chartId = chart.getId();
//        saveCsvData(chartId, result.csvData);
        //异步生成图表
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(RUNNING.getStatus());
        boolean b = this.updateById(updateChart);
        if (!b) {
            handleChartUpdateError(chartId,"更新图表状态失败");
        }
        biMessageProducer.sendMessage(String.valueOf(chartId));
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chartId);
        return biResponse;
    }



    private void handleChartUpdateError(Long id,String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(id);
        updateChart.setStatus(FAILED.getStatus());
        updateChart.setExecMessage(execMessage);
        boolean updateResult = this.updateById(updateChart);
        if (!updateResult){
            log.error("更新图表状态失败"+id+","+execMessage);
        }
    }


    /**
     * 保存csv数据到数据库
     * @param chartId
     * @param csvData
     */
    private void saveCsvData(Long chartId, String csvData) {
        //1. 创建表
        //1.1 获取csv数据中的字段（第一行数据）
        String[] split = csvData.split("\n");
        if (split.length == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "csv数据为空");
        }
        String[] column = split[0].split(",");
        int expectedColumnCount = column.length;

        //1.2 拼接建表语句
        String tableName = "chart_" + chartId;
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE IF NOT EXISTS ");
        createTableSql.append(tableName).append(" (");
        //拼接列名
        for (int i = 0; i < column.length; i++) {
            //去空
            String columnName=column[i].trim();
            // 只处理空列名或纯数字列名
            if (StringUtils.isBlank(columnName)) {
                columnName = "column_" + i;
            }
            //拼接
            createTableSql.append("`").append(columnName).append("` ").append("VARCHAR(255)").append("\n");
            if(i != column.length - 1){
                createTableSql.append(",");
            }
        }
        createTableSql.append(");");
        chartMapper.createTable(createTableSql.toString());

        //2. 插入数据
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO ").append(tableName).append(" VALUES ");

        boolean isFirstRow = true;
        for (int i = 1; i < split.length; i++){
            String line = split[i].trim();
            if (StringUtils.isBlank(line)) {
                continue;
            }

            String[] data = line.split(",", -1);

            if (!isFirstRow) {
                insertSql.append(",");
            }
            isFirstRow = false;

            insertSql.append("(");
            for (int j = 0; j < expectedColumnCount; j++){
                if (j > 0) {
                    insertSql.append(",");
                }

                if (j < data.length && StringUtils.isNotBlank(data[j])) {
                    String value = data[j].trim().replace("'", "''");
                    insertSql.append("'").append(value).append("'");
                } else {
                    insertSql.append("NULL");
                }
            }
            insertSql.append(")");
        }
        insertSql.append(";");
        chartMapper.insertData(insertSql.toString());
    }


    private List<Map<String, Object>> queryChartData(final Long chartId) {
        return chartMapper.queryData(chartId);
    }





//        final String prompt = "你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
//                "分析需求：\n" +
//                "{数据分析的需求或者目标}\n" +
//                "原始数据：\n" +
//                "{csv格式的原始数据，用,作为分隔符}\n" +
//                "请根据这两部分内容，按照以下指定格式生成内容（此外不要输出任何多余的开头、结尾、注释）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象js代码，合理地将数据进行可视化，不要生成任何多余的内容，比如注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
    // 分析需求：
    // 分析网站用户的增长情况
    // 原始数据：
    // 日期,用户数
    // 1号,10
    // 2号,20
    // 3号,30


    //    /**
//     * 读取 CSV 文件内容
//     * @param file 上传的 CSV 文件
//     * @return CSV 格式的字符串
//     */
//    private String readCsvFile(MultipartFile file) {
//        try (InputStream inputStream = file.getInputStream();
//             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
//
//            StringBuilder csvContent = new StringBuilder();
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//                csvContent.append(line).append("\n");
//            }
//
//            return csvContent.toString();
//
//        } catch (IOException e) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取CSV文件失败: " + e.getMessage());
//        }
//    }

}




