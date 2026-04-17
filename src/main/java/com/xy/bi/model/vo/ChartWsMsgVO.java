package com.xy.bi.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成图表通知
 *
 * @author 25133
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChartWsMsgVO {
    private Long chartId;
    private String status;
    private String msg;
}
