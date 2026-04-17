package com.xy.bi.mapper;

import com.xy.bi.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;
import java.util.Map;

/**
* @author 25133
* @description 针对表【chart(图表信息表)】的数据库操作Mapper
* @createDate 2026-04-07 08:44:11
* @Entity com.xy.bi.model.entity.Chart
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




