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
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class MockDataGenerator {

    private final DeviceDataMapper deviceDataMapper;
    private final AlertLogMapper alertLogMapper;
    private final Random random = new Random();
    
    // 缓存状态用于清洗和防重
    private BigDecimal lastTemp = null;
    private BigDecimal lastHum = null;
    private String lastPayloadHash = ""; // 用于重复值判断

    @Scheduled(fixedRate = 2000)
    @Transactional(rollbackFor = Exception.class)
    public void generateMockData() {
        String mockDeviceId = "SERVER-009";
        LocalDateTime now = LocalDateTime.now();

        // ----------------------------------------------------
        // 😈 1. 模拟产生各种“毒数据” (原始负载)
        // ----------------------------------------------------
        Object rawTempObj = 40 + (90 - 40) * random.nextDouble();
        Object rawHumObj = 30 + (80 - 30) * random.nextDouble();
        
        int randomChance = random.nextInt(100);
        if (randomChance < 5) {
            rawTempObj = null; // 模拟：缺失值
        } else if (randomChance < 10) {
            rawTempObj = "NaN_ERROR"; // 模拟：乱码/错误值
        } else if (randomChance < 15) {
            rawTempObj = 5000.0; // 模拟：物理极值错误 (机房不可能 5000 度)
        } else if (randomChance < 20) {
            rawTempObj = (Double) rawTempObj + 50.0; // 模拟：电磁干扰导致的瞬间突变 (毛刺)
        }

        // ----------------------------------------------------
        // 🛡️ 2. 重复值处理 (Duplicate Checking)
        // ----------------------------------------------------
        // 业务场景：传感器因为网络重传，连续发了两次一模一样的数据包
        String currentPayloadHash = String.valueOf(rawTempObj) + "_" + String.valueOf(rawHumObj);
        if (currentPayloadHash.equals(lastPayloadHash)) {
            log.info("【重复值拦截】检测到重复的数据包请求，已丢弃");
            return; // 直接丢弃，不入库
        }
        lastPayloadHash = currentPayloadHash; // 更新防重令牌

        // ----------------------------------------------------
        // 🛠️ 3. 调用数据清洗引擎进行治理，并记录可视化日志
        // ----------------------------------------------------
        BigDecimal temperature = DataCleaner.cleanData(rawTempObj, lastTemp, -20.0, 120.0, 15.0, 25.0, "温度");
        BigDecimal humidity = DataCleaner.cleanData(rawHumObj, lastHum, 0.0, 100.0, 20.0, 45.0, "湿度");

        // 如果原始数据和清洗后的数据差距很大，说明触发了清洗引擎！
        if (rawTempObj != null && !rawTempObj.toString().equals("NaN_ERROR")) {
            try {
                double raw = Double.parseDouble(rawTempObj.toString());
                if (Math.abs(raw - temperature.doubleValue()) > 0.1) {
                    // 将清洗动作推送到前端的日志墙！
                    saveAlert(mockDeviceId, "【数据治理】拦截温度异常波动: " + raw + "℃，已平滑修复为 " + temperature + "℃", now);
                }
            } catch (Exception e) {
                 saveAlert(mockDeviceId, "【数据治理】拦截到乱码数据，已启动安全值兜底机制", now);
            }
        } else if (rawTempObj == null || rawTempObj.toString().equals("NaN_ERROR")) {
             saveAlert(mockDeviceId, "【数据治理】拦截到乱码/缺失数据，已启动安全值兜底机制", now);
        }

        lastTemp = temperature;
        lastHum = humidity;

        // ----------------------------------------------------
        // 🚨 4. 分级告警联动
        // ----------------------------------------------------
        double tempDouble = temperature.doubleValue();
        String alertLevel = "NORMAL";

        if (tempDouble >= 80) {
            alertLevel = "CRITICAL";
            saveAlert(mockDeviceId, "严重故障：设备温度极高 (" + tempDouble + "℃)，请立即停机！", now);
        } else if (tempDouble >= 70) {
            alertLevel = "WARNING";
            saveAlert(mockDeviceId, "预警：设备温度偏高 (" + tempDouble + "℃)，请注意观察。", now);
        }

        // 保存入库
        DeviceData data = new DeviceData();
        data.setDeviceId(mockDeviceId);
        data.setTemperature(temperature);
        data.setHumidity(humidity);
        data.setLevel(alertLevel);
        data.setCreateTime(now);
        deviceDataMapper.insert(data);
    }
    
    private void saveAlert(String deviceId, String msg, LocalDateTime time) {
        AlertLog alert = new AlertLog();
        alert.setDeviceId(deviceId);
        alert.setAlertMessage(msg);
        alert.setStatus(0);
        alert.setCreateTime(time);
        alertLogMapper.insert(alert);
    }
}
