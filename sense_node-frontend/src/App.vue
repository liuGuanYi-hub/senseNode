<template>
  <div class="dashboard-container">
    <header class="header">
      <div class="header-left">
        <h1 class="title">SenseNode 实时在线监测大屏</h1>
      </div>
      <div class="header-right">
        <div class="status-indicator">
          <span>智能诊断状态：</span>
          <div :class="['led', ledClass]"></div>
          <span :class="textClass">{{ statusText }}</span>
        </div>
      </div>
    </header>

    <main class="main-content">
      <section class="chart-section">
        <div class="section-title">实时温度趋势及基准对标分析</div>
        <div class="chart-container" ref="chartRef"></div>
        <div class="bottom-charts">
          <div ref="radarChartRef" class="chart-dom sub-chart"></div>
          <div ref="pieChartRef" class="chart-dom sub-chart"></div>
        </div>
      </section>

      <section class="alert-section">
        <div class="section-title">分级告警协同追踪日志</div>
        <div class="alert-list-container">
          <transition-group name="list" tag="ul" class="alert-list">
            <li v-for="log in alertLogs" :key="log.id" 
                :class="[
                  'alert-item', 
                  log.alertMessage.includes('预警') ? 'item-warning' : '',
                  log.alertMessage.includes('治理') ? 'item-governance' : '' 
                ]">
              <span class="alert-time">{{ formatTime(log.createTime) }}</span>
              <span class="alert-device">[{{ log.deviceId }}]</span>
              <span class="alert-msg">{{ log.alertMessage }}</span>
              
              <span :class="[
                  'alert-status', 
                  log.alertMessage.includes('预警') ? 'status-warning' : '',
                  log.alertMessage.includes('治理') ? 'status-governance' : ''
                ]">
                {{ log.alertMessage.includes('治理') ? '治理' : '实时' }}
              </span>
            </li>
          </transition-group>
          <div v-if="alertLogs.length === 0" class="empty-state">
            暂无历史告警数据
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

const chartRef = ref(null)
const chartInstance = shallowRef(null)
const alertLogs = ref([])

// 新增两个 ref
const radarChartRef = ref(null);
const pieChartRef = ref(null);
const radarChart = shallowRef(null);
const pieChart = shallowRef(null);

// 新增的分级告警状态 (NORMAL, WARNING, CRITICAL)
const alertLevel = ref('NORMAL')

// 计算属性，根据当前 level 返回不同的视觉样式
const ledClass = computed(() => {
  if (alertLevel.value === 'CRITICAL') return 'led-danger' // 红
  if (alertLevel.value === 'WARNING') return 'led-warning' // 黄
  return 'led-normal' // 青
})
const textClass = computed(() => {
  if (alertLevel.value === 'CRITICAL') return 'text-danger'
  if (alertLevel.value === 'WARNING') return 'text-warning'
  return 'text-normal'
})
const statusText = computed(() => {
  if (alertLevel.value === 'CRITICAL') return '严重告警 (紧急停机)'
  if (alertLevel.value === 'WARNING') return '运行偏高 (观察预警)'
  return '运行表现正常'
})

let timer = null
let xData = []
let yData = []

const initChart = () => {
  if (!chartRef.value) return
  chartInstance.value = echarts.init(chartRef.value)
  
  const option = {
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross', label: { backgroundColor: '#283b56' } } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '5%', containLabel: true },
    xAxis: {
      type: 'category', boundaryGap: false, data: xData,
      axisLine: { lineStyle: { color: '#00f6ff' } },
      axisLabel: { color: '#8ab4f8', formatter: (value) => value ? value.split('T')[1].split('.')[0] : '' }
    },
    yAxis: {
      type: 'value', name: '温度 (℃)', nameTextStyle: { color: '#8ab4f8' },
      axisLine: { show: true, lineStyle: { color: '#00f6ff' } },
      splitLine: { show: true, lineStyle: { color: 'rgba(0, 246, 255, 0.1)', type: 'dashed' } },
      axisLabel: { color: '#8ab4f8' },
      min: (value) => Math.max(0, value.min - 10), max: (value) => value.max + 10
    },
    visualMap: {
      show: false,
      pieces: [
        { gt: 0, lte: 70, color: '#00f6ff' },    // 70 度内正常
        { gt: 70, lte: 80, color: '#faad14' },   // 70~80 黄色预警
        { gt: 80, color: '#ff4d4f' }             // 大于 80 红色告警
      ],
      outOfRange: { color: '#999' }
    },
    series: [
      {
        name: '实时温度', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6, data: yData,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0, 246, 255, 0.4)' },
            { offset: 1, color: 'rgba(0, 246, 255, 0.0)' }
          ])
        },
        lineStyle: { width: 3 }
      },
      // ====== 新增：离线试验基准 ======
      {
        name: '离线额定标准',
        type: 'line',
        step: false,
        symbol: 'none',
        // 渲染成平直的基准线
        data: new Array(30).fill(65),
        lineStyle: {
          color: '#ff9c6e',
          width: 1.5,
          type: 'dashed' // 虚线刻画
        },
        silent: true 
      }
    ]
  }
  chartInstance.value.setOption(option)

  // ==================== 3. 综合健康度雷达图 ====================
  if (radarChartRef.value) {
    radarChart.value = echarts.init(radarChartRef.value);
    const radarOption = {
      title: { text: '设备综合健康度 (5维)', fontSize: 14, textStyle: { color: '#fff' } },
      tooltip: {},
      radar: {
        center: ['50%', '60%'],
        indicator: [
          { name: '温度状态', max: 100 },
          { name: '湿度状态', max: 100 },
          { name: 'CPU负载', max: 100 },
          { name: '内存使用', max: 100 },
          { name: '网络延迟', max: 100 }
        ],
        axisName: { color: '#aaa' },
        splitArea: { areaStyle: { color: ['rgba(250,250,250,0.05)', 'rgba(200,200,200,0.02)'] } },
        splitLine: { lineStyle: { color: '#444' } }
      },
      series: [{
        name: '健康得分',
        type: 'radar',
        data: [ { value: [85, 90, 75, 60, 95], name: '当前服务器' } ],
        itemStyle: { color: '#722ed1' },
        areaStyle: { color: 'rgba(114, 46, 209, 0.4)' }
      }]
    };
    radarChart.value.setOption(radarOption);
  }

  // ==================== 4. 告警状态分布环形图 ====================
  if (pieChartRef.value) {
    pieChart.value = echarts.init(pieChartRef.value);
    const pieOption = {
      title: { text: '历史运行状态占比', fontSize: 14, textStyle: { color: '#fff' } },
      tooltip: { trigger: 'item' },
      series: [{
        name: '状态占比',
        type: 'pie',
        radius: ['40%', '70%'], // 挖空中间，变成环形图
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 5, borderColor: '#1e222d', borderWidth: 2 },
        label: { show: false }, // 隐藏多余标签保持整洁
        data: [
          { value: 850, name: '正常运行', itemStyle: { color: '#52c41a' } },
          { value: 120, name: '二级预警', itemStyle: { color: '#faad14' } },
          { value: 30,  name: '一级告警', itemStyle: { color: '#f5222d' } }
        ]
      }]
    };
    pieChart.value.setOption(pieOption);
  }
}

const fetchData = async () => {
  try {
    const historyRes = await axios.get('http://localhost:8080/api/device/history')
    const records = historyRes.data || []
    
    if (records.length > 0) {
      const latestData = records[records.length - 1]
      // 读取后端判定好的级别，实现前后端状态联动
      alertLevel.value = latestData.level || 'NORMAL'
      
      xData = records.map(item => item.createTime)
      yData = records.map(item => item.temperature)
      
      if (chartInstance.value) {
        chartInstance.value.setOption({
          xAxis: { data: xData },
          series: [
            { data: yData },
            // 实时保持数组长度与基准线对应
            { data: new Array(records.length).fill(65) }
          ]
        })
      }
    }

    const alertsRes = await axios.get('http://localhost:8080/api/device/alerts')
    alertLogs.value = alertsRes.data || []

    // 获取雷达图数据
    try {
      const healthRes = await axios.get('http://localhost:8080/api/device/health')
      if (radarChart.value && healthRes.data) {
        radarChart.value.setOption({
          series: [{ data: [ { value: [healthRes.data.temperature, healthRes.data.humidity, healthRes.data.cpu, healthRes.data.memory, healthRes.data.network], name: '当前服务器' } ] }]
        })
      }
    } catch (e) {
      console.error('获取健康度数据失败', e)
    }

    // 获取饼图数据
    try {
      const statsRes = await axios.get('http://localhost:8080/api/device/stats')
      if (pieChart.value && statsRes.data) {
        pieChart.value.setOption({
          series: [{ data: statsRes.data }]
        })
      }
    } catch (e) {
      console.error('获取状态统计数据失败', e)
    }

  } catch (error) {
    console.error('拉取接口数据失败:', error)
  }
}

const formatTime = (timeStr) => timeStr ? timeStr.replace('T', ' ').split('.')[0] : ''
const handleResize = () => { 
  if (chartInstance.value) chartInstance.value.resize() 
  if (radarChart.value) radarChart.value.resize()
  if (pieChart.value) pieChart.value.resize()
}

onMounted(() => {
  initChart()
  fetchData()
  timer = setInterval(fetchData, 2000)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  if (timer) clearInterval(timer)
  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }
  if (radarChart.value) {
    radarChart.value.dispose()
    radarChart.value = null
  }
  if (pieChart.value) {
    pieChart.value.dispose()
    pieChart.value = null
  }
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.dashboard-container { fill: inherit; min-height: 100vh; background: #0b0f19 url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="60" height="60" opacity="0.03"><path d="M0 0h60v60H0z" fill="none"/><path d="M0 59.5h60M59.5 0v60" stroke="%2300f6ff" stroke-width="1"/></svg>') repeat; color: #e2e8f0; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; display: flex; flex-direction: column; }
.header { height: 80px; display: flex; justify-content: space-between; align-items: center; padding: 0 40px; background: linear-gradient(180deg, rgba(11,21,36,0.9) 0%, rgba(11,21,36,0) 100%); border-bottom: 1px solid rgba(0, 246, 255, 0.2); position: relative; flex-shrink: 0; }
.header::after { content: ''; position: absolute; bottom: -1px; left: 50%; transform: translateX(-50%); width: 30%; height: 3px; background: #00f6ff; box-shadow: 0 0 10px #00f6ff, 0 0 20px rgba(0,246,255,0.8); }
.title { margin: 0; font-size: 26px; font-weight: 600; letter-spacing: 3px; color: #00f6ff; text-shadow: 0 0 12px rgba(0, 246, 255, 0.4); }
.status-indicator { display: flex; align-items: center; font-size: 16px; background: rgba(0, 40, 60, 0.5); padding: 8px 16px; border-radius: 4px; border: 1px solid rgba(0, 246, 255, 0.2); box-shadow: inset 0 0 10px rgba(0, 246, 255, 0.1); }

.led { width: 14px; height: 14px; border-radius: 50%; margin: 0 12px; }
.led-normal { background-color: #00f6ff; box-shadow: 0 0 8px #00f6ff, 0 0 16px rgba(0,246,255,0.5); }
.led-warning { background-color: #faad14; box-shadow: 0 0 10px #faad14, 0 0 20px #faad14; animation: flash-warning 1.2s infinite alternate; }
.led-danger { background-color: #ff4d4f; box-shadow: 0 0 12px #ff4d4f, 0 0 24px #ff4d4f; animation: flash-danger 0.5s infinite alternate; }

@keyframes flash-warning { 0% { opacity: 0.4; } 100% { opacity: 1; box-shadow: 0 0 25px #faad14; } }
@keyframes flash-danger { 0% { opacity: 0.2; transform: scale(0.8); } 100% { opacity: 1; transform: scale(1.2); box-shadow: 0 0 20px #ff4d4f, 0 0 30px #ff4d4f; } }

.text-normal { color: #00f6ff; font-weight: bold; }
.text-warning { color: #faad14; font-weight: bold; text-shadow: 0 0 8px rgba(250,173,20,0.6); }
.text-danger { color: #ff4d4f; font-weight: bold; text-shadow: 0 0 8px rgba(255,77,79,0.8); }

.main-content { flex: 1; display: flex; gap: 24px; padding: 24px 40px; overflow: hidden; }
.chart-section, .alert-section { background: rgba(13, 25, 48, 0.7); border: 1px solid rgba(0, 246, 255, 0.15); border-radius: 8px; padding: 20px; display: flex; flex-direction: column; position: relative; }
.chart-section { flex: 2; box-shadow: inset 0 0 30px rgba(0, 246, 255, 0.02); }
.alert-section { flex: 1; }
.chart-section::before, .alert-section::before { content: ''; position: absolute; top: -1px; left: -1px; width: 16px; height: 16px; border-top: 2px solid #00f6ff; border-left: 2px solid #00f6ff; }
.chart-section::after, .alert-section::after { content: ''; position: absolute; bottom: -1px; right: -1px; width: 16px; height: 16px; border-bottom: 2px solid #00f6ff; border-right: 2px solid #00f6ff; }
.section-title { font-size: 18px; font-weight: 500; color: #fff; margin-bottom: 16px; padding-left: 10px; border-left: 3px solid #00f6ff; letter-spacing: 1px; }
.chart-container { flex: 1; width: 100%; min-height: 200px; }

/* 新增：底部副图表并排显示 */
.bottom-charts {
  display: flex;
  gap: 20px;
  height: 220px; /* 控制底部图表高度 */
  margin-top: 20px;
}

.sub-chart {
  flex: 1; /* 平分空间 */
  height: 100%;
}

.alert-list-container { flex: 1; overflow-y: auto; overflow-x: hidden; position: relative; padding-right: 4px; }
.alert-list-container::-webkit-scrollbar { width: 4px; }
.alert-list-container::-webkit-scrollbar-thumb { background: rgba(0, 246, 255, 0.3); border-radius: 2px; }
.alert-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
.alert-item { display: flex; align-items: center; padding: 12px; background: rgba(255, 77, 79, 0.03); border: 1px solid rgba(255, 77, 79, 0.2); border-radius: 4px; font-size: 13px; border-left: 3px solid #ff4d4f; transition: all 0.2s; }
.alert-item.item-warning { border-left-color: #faad14; background: rgba(250, 173, 20, 0.03); border-color: rgba(250, 173, 20, 0.2); }
.alert-item:hover { background: rgba(255, 77, 79, 0.1); box-shadow: 0 0 10px rgba(255, 77, 79, 0.1); }
.alert-item.item-warning:hover { background: rgba(250, 173, 20, 0.1); box-shadow: 0 0 10px rgba(250, 173, 20, 0.1); }

.alert-time { color: #8ab4f8; margin-right: 12px; font-family: Consolas, monospace; }
.alert-device { color: #a78bfa; margin-right: 8px; }
.alert-msg { flex: 1; color: #ffcfcf; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.item-warning .alert-msg { color: #ffe58f; }

.alert-status { padding: 2px 6px; background: #ff4d4f; color: #fff; border-radius: 2px; font-size: 12px; margin-left: 10px; }
.alert-status.status-warning { background: #faad14; }

.list-enter-active, .list-leave-active { transition: all 0.5s ease; }
.list-enter-from { opacity: 0; transform: translateX(30px) scaleY(0.5); }
.list-leave-to { opacity: 0; transform: translateX(-30px); }
.empty-state { position: absolute; top: 40%; left: 50%; transform: translate(-50%, -50%); color: #8ab4f8; font-size: 14px; opacity: 0.4; letter-spacing: 2px; }

/* 数据治理日志的专属青蓝色样式 */
.alert-item.item-governance { 
  border-left-color: #00f6ff; 
  background: rgba(0, 246, 255, 0.05); 
  border-color: rgba(0, 246, 255, 0.2); 
}
.alert-item.item-governance:hover { 
  background: rgba(0, 246, 255, 0.15); 
  box-shadow: 0 0 10px rgba(0, 246, 255, 0.2); 
}
.item-governance .alert-msg { color: #8ab4f8; } /* 治理文本颜色 */
.alert-status.status-governance { background: #00f6ff; color: #0b1524; } /* 标签变青色 */
</style>

<style>
body, html, #app {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: hidden; /* 防止出现多余的滚动条 */
}
* { /* 👇 加上这段，强制所有元素的 width 包含 padding 和 border */
  box-sizing: border-box;
}
</style>