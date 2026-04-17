package com.xy.bi.mapper;

import com.xy.bi.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
 * 图表数据库操作
 * @author 25133
*/
public interface ChartMapper extends BaseMapper<Chart> {


    /**
     * 创建表
     * @param createTableSql
     */
    void createTable(final String createTableSql);

    /**
     * 插入数据
     * @param insertSql
     */
    void insertData(final String insertSql);

    /**
     * 查询数据
     * @param chartId
     * @return
     */
    List<Map<String,Object>> queryData(final Long chartId);
}




