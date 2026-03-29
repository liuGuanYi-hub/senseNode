package com.example.sensenodebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("device_data")
public class DeviceData {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private BigDecimal temperature;
    private BigDecimal humidity;
    private String level; // NORMAL, WARNING, CRITICAL
    private LocalDateTime createTime;
}
