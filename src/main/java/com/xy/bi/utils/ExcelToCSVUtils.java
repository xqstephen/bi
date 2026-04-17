package com.xy.bi.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import org.springframework.web.multipart.MultipartFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class ExcelToCSVUtils {
    public static String excelToCSV(MultipartFile multipartFile) {
        //读出excel文件
        List<Map<Integer, String>> list = null;
        try {
            list=EasyExcel.read(multipartFile.getInputStream())
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (CollUtil.isEmpty(list)) {
            return "";
        }
        //转为csv
        StringBuilder csvBuilder = new StringBuilder();
        //获取表头
        Map<Integer, String> headerMap = list.get(0);
        List<String> headerList = headerMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
        csvBuilder.append(String.join(",", headerList)).append("\n");
        //获取数据
        for (int i = 1; i < list.size(); i++) {
            Map<Integer, String> dataMap = list.get(i);
            List<String> dataList = dataMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
            csvBuilder.append(String.join(",", dataList)).append("\n");
        }

        return csvBuilder.toString();
    }


    /**
     * 重载方法：根据文件路径转换（用于本地测试）
     */
    public static String excelToCSV(String filePath) throws FileNotFoundException {
        List<Map<Integer, String>> list = EasyExcel.read(filePath)
                .sheet(0)
                .headRowNumber(0)
                .doReadSync();

        if (CollUtil.isEmpty(list)) {
            return "";
        }

        StringBuilder csvBuilder = new StringBuilder();
        Map<Integer, String> headerMap = list.get(0);
        List<String> headerList = headerMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
        csvBuilder.append(String.join(",", headerList)).append("\n");

        for (int i = 1; i < list.size(); i++) {
            Map<Integer, String> dataMap = list.get(i);
            List<String> dataList = dataMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
            csvBuilder.append(String.join(",", dataList)).append("\n");
        }

        return csvBuilder.toString();
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        try {
            // 测试 resources 目录下的文件（需要完整路径）
            String filePath = "src/main/resources/test_excel.xlsx";
            String csv = excelToCSV(filePath);
            System.out.println("转换后的CSV内容：");
            System.out.println(csv);
        } catch (FileNotFoundException e) {
            System.err.println("文件不存在：" + e.getMessage());
        }
    }
}
