package com.xy.bi.model.dto.chart;

import lombok.Data;

/**
 * AI生成图表请求
 * @author 25133
 */
@Data
public class GenChartByAiRequest {

    /**
     * 图表名称
     */
    private String name;
    /**
     * 图表数据
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;
}
