package com.xy.bi.service;

import com.xy.bi.model.dto.chart.GenChartByAiRequest;
import com.xy.bi.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.bi.model.entity.User;
import com.xy.bi.model.vo.BiResponse;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
* @author 25133
* @description 针对表【chart(图表信息表)】的数据库操作Service
* @createDate 2026-04-07 08:44:11
*/
public interface ChartService extends IService<Chart> {

    /**
     * Ai生成图表
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    BiResponse genChartByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest,User loginUser);


    /**
     * Ai生成图表异步线程池版
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     */
    BiResponse genCharAsynctByAi(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser);

    /**
     * AI生成图表mq版本
     * @param multipartFile
     * @param genChartByAiRequest
     * @param loginUser
     * @return
     */
    BiResponse genChartByAiMQ(MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, User loginUser);



    //构建给AI的用户输入
    String buildAiUserInput(String csvData, GenChartByAiRequest request);
}

