package com.example.sensenodebackend.utils;

import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class DataCleaner {

    /**
     * 工业级通用数据清洗与标准化引擎
     *
     * @param rawValue     前端或传感器传来的原始值 (使用 Object 接收，防止乱码报错)
     * @param lastValue    上一次的有效值 (用于兜底和滤波)
     * @param minLimit     物理允许最小值 (极值拦截)
     * @param maxLimit     物理允许最大值 (极值拦截)
     * @param maxSpike     最大允许跳变率 (限幅滤波)
     * @param defaultValue 绝对默认安全值 (系统冷启动兜底)
     * @param metricName   指标名称 (用于打日志)
     * @return 清洗后的标准化有效值
     */
    public static BigDecimal cleanData(Object rawValue, BigDecimal lastValue, 
                                       double minLimit, double maxLimit, 
                                       double maxSpike, double defaultValue, 
                                       String metricName) {
        
        // ==========================================
        // 1. 缺失值处理 (Missing Values)
        // ==========================================
        if (rawValue == null || rawValue.toString().trim().isEmpty() || "null".equalsIgnoreCase(rawValue.toString())) {
            log.warn("【缺失值拦截】{} 数据丢失，采用上一时刻有效值兜底", metricName);
            return lastValue != null ? lastValue : BigDecimal.valueOf(defaultValue);
        }

        double current;
        // ==========================================
        // 2. 乱码/格式错误值处理 (Type Errors)
        // ==========================================
        try {
            current = Double.parseDouble(rawValue.toString());
        } catch (NumberFormatException e) {
            log.error("【乱码错误拦截】{} 收到无法解析的数据: '{}'", metricName, rawValue);
            return lastValue != null ? lastValue : BigDecimal.valueOf(defaultValue);
        }

        // ==========================================
        // 3. 物理极值错误处理 (Out of Bounds)
        // ==========================================
        if (current < minLimit || current > maxLimit) {
            log.warn("【物理极值拦截】{} 收到不可能的数值: {}，判定为传感器故障", metricName, current);
            return lastValue != null ? lastValue : BigDecimal.valueOf(defaultValue);
        }

        // ==========================================
        // 4. 突变异常值处理 (Spike Noise / 限幅滤波)
        // ==========================================
        if (lastValue != null) {
            double last = lastValue.doubleValue();
            if (Math.abs(current - last) > maxSpike) {
                log.warn("【突变噪声拦截】{} 数值跳变过大 ({} -> {})，执行平滑限幅", metricName, last, current);
                // 如果突变过大，强制将其压制在最大允许的变化幅度内
                current = current > last ? last + maxSpike : last - maxSpike;
            }
        }

        // 数据标准化：统一保留两位小数
        return BigDecimal.valueOf(current).setScale(2, RoundingMode.HALF_UP);
    }
}
