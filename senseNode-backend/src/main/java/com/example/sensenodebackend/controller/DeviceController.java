package com.example.sensenodebackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.sensenodebackend.entity.AlertLog;
import com.example.sensenodebackend.entity.DeviceData;
import com.example.sensenodebackend.mapper.AlertLogMapper;
import com.example.sensenodebackend.mapper.DeviceDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceDataMapper deviceDataMapper;
    private final AlertLogMapper alertLogMapper;

    @GetMapping("/history")
    public List<DeviceData> getDeviceHistory() {
        // 先降序查最近 30 条，然后再翻转为时间升序，方便前端图表展示
        List<DeviceData> recentData = deviceDataMapper.selectList(
                new LambdaQueryWrapper<DeviceData>()
                        .orderByDesc(DeviceData::getCreateTime)
                        .last("LIMIT 30")
        );
        Collections.reverse(recentData);
        return recentData;
    }

    @GetMapping("/alerts")
    public List<AlertLog> getRecentAlerts() {
        return alertLogMapper.selectList(
                new LambdaQueryWrapper<AlertLog>()
                        .eq(AlertLog::getStatus, 0)
                        .orderByDesc(AlertLog::getCreateTime)
                        .last("LIMIT 10")
        );
    }

    // ==========================================
    // 新增模块 1：获取设备综合健康度 (供雷达图使用)
    // ==========================================
    @GetMapping("/health")
    public Map<String, Object> getDeviceHealth() {
        // 在真实企业项目中，这里会调用 Oshi 等探针库获取真实的 CPU 和内存。
        // 为了面试展示系统的多维分析能力，这里利用算法生成波动的拟合数据。
        Map<String, Object> healthData = new HashMap<>();
        
        // 模拟各维度的健康得分 (满分 100)
        healthData.put("temperature", 85 + Math.random() * 10); // 温度健康度
        healthData.put("humidity", 90 - Math.random() * 5);     // 湿度健康度
        healthData.put("cpu", 60 + Math.random() * 20);         // CPU 负载得分
        healthData.put("memory", 75 + Math.random() * 15);      // 内存健康度
        healthData.put("network", 95 - Math.random() * 5);      // 网络连通性
        
        return healthData;
    }

    // ==========================================
    // 新增模块 2：获取历史告警状态占比 (供环形图使用)
    // ==========================================
    @GetMapping("/stats")
    public List<Map<String, Object>> getAlertStats() {
        // 真实项目中这里会写复杂的 SQL 分组聚合查询：
        // SELECT status, COUNT(*) FROM alert_log GROUP BY status
        
        List<Map<String, Object>> stats = new ArrayList<>();
        
        // 组装前端 ECharts 需要的 {"name": "xxx", "value": xxx} 格式
        Map<String, Object> normal = new HashMap<>();
        normal.put("name", "正常运行");
        normal.put("value", 850 + (int)(Math.random() * 10)); // 加入轻微波动，让前端图表有呼吸感
        stats.add(normal);

        Map<String, Object> warning = new HashMap<>();
        warning.put("name", "二级预警");
        warning.put("value", 120 + (int)(Math.random() * 5));
        stats.add(warning);

        Map<String, Object> critical = new HashMap<>();
        critical.put("name", "一级告警");
        critical.put("value", 30 + (int)(Math.random() * 2));
        stats.add(critical);

        return stats;
    }
}
