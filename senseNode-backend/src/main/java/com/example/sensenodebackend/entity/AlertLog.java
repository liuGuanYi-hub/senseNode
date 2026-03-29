package com.example.sensenodebackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("alert_log")
public class AlertLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deviceId;
    private String alertMessage;
    private Integer status; // 0-未处理，1-已处理
    private LocalDateTime createTime;
}
