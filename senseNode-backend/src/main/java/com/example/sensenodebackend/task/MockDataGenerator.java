package com.example.sensenodebackend.task;

import com.example.sensenodebackend.entity.AlertLog;
import com.example.sensenodebackend.entity.DeviceData;
import com.example.sensenodebackend.mapper.AlertLogMapper;
import com.example.sensenodebackend.mapper.DeviceDataMapper;
import com.example.sensenodebackend.utils.DataCleaner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataGenerator {

    private final DeviceDataMapper deviceDataMapper;
    private final AlertLogMapper alertLogMapper;
    private final Random random = new Random();
    
    // 缓存上一次的有效温度用于滤波计算
    private BigDecimal lastTemp = null;

    @Scheduled(fixedRate = 2000)
    @Transactional(rollbackFor = Exception.class)
    public void generateMockData() {
        String mockDeviceId = "SN-DEVICE-001";
        LocalDateTime now = LocalDateTime.now();

        // 随机生成 40~90度 的温度
        double rawTempVal = 40 + (90 - 40) * random.nextDouble();
        
        // 刻意制造传感器采集噪声：5% 概率出现 +50 度的恐怖峰值突刺（用于面试展示限幅滤波效果）
        if (random.nextInt(100) < 5) {
            rawTempVal += 50.0;
        }

        double humVal = 30 + (80 - 30) * random.nextDouble();
        
        BigDecimal rawTemperature = BigDecimal.valueOf(rawTempVal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal humidity = BigDecimal.valueOf(humVal).setScale(2, RoundingMode.HALF_UP);

        // ===== 引入面试亮点数据治理：去噪清洗 =====
        BigDecimal temperature = DataCleaner.cleanTemperature(rawTemperature, lastTemp);
        if (rawTemperature.compareTo(temperature) != 0) {
            log.info("【过滤】拦截到异常传感器噪声：突变前={}, 当前有效恢复={}", rawTemperature, temperature);
        }
        lastTemp = temperature; // 缓存有效值

        // ===== 引入面试亮点评级告警：分级逻辑 =====
        double tempDouble = temperature.doubleValue();
        String alertLevel = "NORMAL";

        if (tempDouble >= 80) {
            alertLevel = "CRITICAL"; // 一级告警：红灯停机
            saveAlert(mockDeviceId, "严重故障：设备温度极高，请立即停机！", now);
        } else if (tempDouble >= 70) {
            alertLevel = "WARNING"; // 二级告警：黄灯观察
            saveAlert(mockDeviceId, "预警：设备温度偏高，请注意观察。", now);
        }

        // 保存设备监测记录
        DeviceData data = new DeviceData();
        data.setDeviceId(mockDeviceId);
        data.setTemperature(temperature);
        data.setHumidity(humidity);
        data.setLevel(alertLevel); // 植入当前温度层级
        data.setCreateTime(now);
        deviceDataMapper.insert(data);
        
        log.info("【设备监控】设备[{}] 上报 -> 温度验证:{}℃, 湿度:{}%, 级别:{}", mockDeviceId, temperature, humidity, alertLevel);
    }
    
    /**
     * 通用的告警日志写入保存方法
     */
    private void saveAlert(String deviceId, String msg, LocalDateTime time) {
        AlertLog alert = new AlertLog();
        alert.setDeviceId(deviceId);
        alert.setAlertMessage(msg);
        alert.setStatus(0); // 0-未处理
        alert.setCreateTime(time);
        alertLogMapper.insert(alert);
        log.warn("【触发告警机制】: {}", msg);
    }
}
