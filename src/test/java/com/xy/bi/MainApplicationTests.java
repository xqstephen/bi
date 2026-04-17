package com.xy.bi;

import com.xy.bi.common.ErrorCode;
import com.xy.bi.exception.BusinessException;
import com.xy.bi.mapper.ChartMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 主类测试
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@SpringBootTest
class MainApplicationTests {
    @Autowired
    private ChartMapper chartMapper;

    @Test
    void contextLoads() {



    }
    @Test
    void test() {
        String csvData = "姓名,销售额,月份\n" +
                "张三,10000,1月\n" +
                "李四,15000,2月\n" +
                "王五,8000,3月";

        saveCsvData(1L, csvData);

    }

    @Test
    void test2() {
        String s = readCsvFile("src/main/resources/test_csv.csv");
        System.out.println(s);
    }

    /**
     * 读取 CSV 文件内容
     * @param filePath 上传的 CSV 文件
     * @return CSV 格式的字符串
     */
    private String readCsvFile(String filePath) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(filePath));
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            StringBuilder csvContent = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                csvContent.append(line).append("\n");
            }

            return csvContent.toString();

        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取CSV文件失败: " + e.getMessage());
        }
    }


    private void saveCsvData(Long chartId, String csvData) {
        //1. 创建表
        //1.1 获取csv数据中的字段（第一行数据）
        String[] split = csvData.split("\n");
        if (split.length == 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "csv数据为空");
        }
        String[] column = split[0].split(",");
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
        for (int i = 1; i < split.length; i++){
            System.out.println(split[i]);
            String[] data = split[i].split(",");
            insertSql.append("(");
            for (int j = 0; j < data.length; j++){
                insertSql.append("'").append(data[j].trim()).append("'");
                if(j != data.length - 1){
                    insertSql.append(",");
                }
            }
            insertSql.append(")");
            if(i != split.length - 1){
                insertSql.append(",");
            }
        }
        insertSql.append(";");
        chartMapper.insertData(insertSql.toString());
    }

}
