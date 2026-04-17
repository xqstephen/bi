package com.xy.bi.controller;


import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/thread")
@Slf4j
@Deprecated
public class ThreadController {

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @PostMapping("/add")
    public void add(String name) {
        CompletableFuture.runAsync(()->{
            log.info("任务执行中:" + name + ",执行人:" + Thread.currentThread().getName());
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        },threadPoolExecutor);

    }

    @PostMapping("/get")
    public String get(){
        HashMap<String, Object> map = new HashMap<>();
        map.put("执行线程数量",threadPoolExecutor.getActiveCount());
        map.put("总任务数量",threadPoolExecutor.getTaskCount());
        map.put("已完成任务数量",threadPoolExecutor.getCompletedTaskCount());
        map.put("队列长度",threadPoolExecutor.getQueue().size());
        return JSONUtil.toJsonStr(map);
    }
}
