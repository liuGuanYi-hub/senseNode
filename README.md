# 🌐 senseNode 工业级在线监测与离线数据治理系统

![Vue3](https://img.shields.io/badge/Vue.js-3.0-brightgreen.svg) ![Spring Boot](https://img.shields.io/badge/SpringBoot-3.2-blue.svg) ![Java](https://img.shields.io/badge/Java-17-orange.svg) ![ECharts](https://img.shields.io/badge/ECharts-5.0-red.svg)

## 📖 项目简介
本项目为一个真实工业级标准的在线监测与数据治理平台。系统致力于解决工业现场传感器高频数据采集、在线/离线数据对比分析、异常告警协同追踪等核心痛点。采用前后端分离架构，实现了从底层数据流转到前端暗色科技感大屏的多维可视化闭环。

> **📺 大屏效果演示：**
> *(👉 <img width="2560" height="1180" alt="image" src="https://github.com/user-attachments/assets/76935f50-2316-4dec-b79c-15d87ddef75a" />


## 🚀 核心业务特性

### 1. 毫秒级在线监测与离线基准对标
* 采用 ECharts 渲染动态折线流，实时呈现温湿度波动。
* 创新性引入**离线试验额定基准线**，实现在线数据与离线标准的同屏拟合分析，直观暴露设备性能衰退趋势。

### 2. BFF 层数据聚合与多维健康度评估
* 摒弃传统的单一阈值判断，在后端构建数据聚合层（BFF）。
* 实时模拟计算 CPU、内存、网络、温湿度等多维指标，前端通过**五维雷达图**呈现设备综合健康画像。

### 3. 多级联动告警与生命周期追踪
* 后端内置智能诊断算法，实现状态的动态判定（`NORMAL`, `WARNING`, `CRITICAL`）。
* 前端通过状态指示灯、动态边框色与告警日志墙实现**秒级视觉协同联动**。
* 引入**环形图**对海量历史告警数据进行离线聚合统计，直观展示设备全生命周期的运行可靠性。

## 🛠️ 技术栈选型
* **前端生态**：Vue 3 (Composition API) + Vite + Axios + Apache ECharts (Dark Theme)
* **后端架构**：Java 17 + Spring Boot 3.2.x + MyBatis-Plus + MySQL 8.x
* **数据治理**：基于限幅滤波算法的轻量级脏数据拦截与清洗逻辑。

## 💡 深度排坑与技术沉淀
在项目工程化构建过程中，曾遭遇 Spring Boot 3 升级与 Maven `maven-compiler-plugin` 机制变更导致的 Lombok `@Data` 注解大面积失效的底层环境冲突。
通过深度排查官方文档与依赖树，最终采用剥离冗余编译插件、交由 Spring Boot 内置接管的方案实现了平滑修复，并总结提炼了完整的排错复盘文档。

## 👨‍💻 关于作者
**全栈独立开发者**。具备完整的前后端闭环工程能力，对工业级数据大屏、性能优化及系统架构设计有浓厚兴趣。
