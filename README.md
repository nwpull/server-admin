# Server Admin - Linux 服务器 Web 运维管理平台

基于 **Spring Boot 3 + Vue 3 + Element Plus + MySQL** 的服务器运维管理平台。

## 功能

- 🔧 **Web SSH 终端** - xterm.js 多标签页终端
- 📁 **文件管理器** - SFTP 浏览/上传/下载/编辑
- 📊 **系统监控** - CPU/内存/磁盘/网络实时图表
- ⚙️ **进程管理** - 查看/搜索/终止进程
- 📋 **日志查看** - 实时 tail + 非实时读取
- 🔌 **服务管理** - systemd 服务启停管理
- ⏰ **Cron 任务** - 定时任务增删改查
- 🖥️ **仪表盘** - 多服务器概览 + 告警

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.2 + MyBatis-Plus + MySQL + JSch |
| 前端 | Vue 3 + TypeScript + Element Plus + ECharts + xterm.js |
| 通信 | REST API + WebSocket |

## 快速开始

### 后端

```bash
cd server-admin-backend

# 1. 创建 MySQL 数据库
mysql -u root -p < src/main/resources/schema.sql

# 2. 修改数据库配置
# 编辑 src/main/resources/application.yml

# 3. 启动
mvn spring-boot:run
```

### 前端

```bash
cd server-admin-frontend

npm install
npm run dev
```

### Docker 部署

```bash
docker compose up -d --build
```

## 默认账号

- 用户名：`admin`
- 密码：`admin123`

## License

MIT
