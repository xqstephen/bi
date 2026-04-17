package com.xy.bi.manager;

import com.tencentcloudapi.common.AbstractModel;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.lkeap.v20240522.LkeapClient;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsRequest;
import com.tencentcloudapi.lkeap.v20240522.models.ChatCompletionsResponse;
import com.tencentcloudapi.lkeap.v20240522.models.Message;
import com.xy.bi.common.ErrorCode;
import com.xy.bi.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * AI调用
 * @author 25133
 */
@Slf4j
@Service
public class AiManager {

    @Resource
    private LkeapClient deepSeekClient;

    final String prompt = "你的职责是数据分析师和前端分析专家，接下来我会按照以下固定格式给你提供内容:\n"+
            "分析需求: \n"+
            "{数据分析的需求或者目标,以及可能提到json代码生成图表的类型}\n"+
            "原始数据: \n"+
            "{csv格式的原始数据，用,作为分隔符} \n"+
            "请根据这两部分内容，按照以下格式生成内容（此外不要输出任何开头、结尾、注释）\n"+
            "【【【【【\n"+
            "{前端 Echarts V5 的 option配置对象json代码，合理地对数据进行可视化，一定要生成用户定义的图表类型，不要生成任何多余的内容}\n"+
            "【【【【【\n"+
            "{明确的分析结论，越详细越好，不要生成注释}";


    public String doChat(String userInput)  {
        try {
            ChatCompletionsRequest req = new ChatCompletionsRequest();
            req.setModel("deepseek-v3.2");

            Message[] messages = new Message[2];
            // 系统输入
            Message message0= new Message();
            message0.setRole("system");
            message0.setContent(prompt);
            messages[0] = message0;
            // 用户输入
            Message message1= new Message();
            message1.setRole("user");
            message1.setContent(userInput);
            messages[1] = message1;

            req.setMessages(messages);

            req.setStream(false);
            // 返回的resp是一个ChatCompletionsResponse的实例，与请求对象对应
            ChatCompletionsResponse resp = deepSeekClient.ChatCompletions(req);
            // 输出json格式的字符串回包
            return AbstractModel.toJsonString(resp);
        } catch (TencentCloudSDKException e) {
            log.error("doChat error", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求失败");
        }

    }
}
