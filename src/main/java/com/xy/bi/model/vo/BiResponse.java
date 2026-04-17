package com.xy.bi.model.vo;

import lombok.Data;

/**
 * BI返回结果
 *
 * @author 25133
 */
@Data
public class BiResponse {
    private Long chartId;
    private String genChart;
    private String genResult;
}
