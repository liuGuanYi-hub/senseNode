package com.example.sensenodebackend.utils;

import java.math.BigDecimal;

public class DataCleaner {

    /**
     * 工业级传感器去噪：限幅滤波算法
     * 目的：防止传感器受到电磁脉冲干扰干扰产生跳变（毛刺数据）
     * @param current 当前采集值
     * @param last    上一次的有效采集值
     * @return 过滤后的有效值
     */
    public static BigDecimal cleanTemperature(BigDecimal current, BigDecimal last) {
        if (last == null) {
            return current;
        }
        
        // 如果瞬时跳变超过 30 度，判定为传感器电气噪声，予以平滑处理或丢弃跳变点
        double diff = Math.abs(current.doubleValue() - last.doubleValue());
        if (diff > 30.0) {
            // 剔除噪声，保持原值
            return last;
        }
        
        return current;
    }
}
