<template>
  <el-container class="main-layout">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapsed ? '64px' : '220px'" class="aside">
      <div class="logo-area">
        <el-icon :size="24" color="#409EFF"><Monitor /></el-icon>
        <span v-show="!isCollapsed" class="logo-text">Server Admin</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapsed"
        :collapse-transition="false"
        router
        background-color="#1f1f1f"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="side-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <template #title>仪表盘</template>
        </el-menu-item>
        <el-menu-item index="/terminal">
          <el-icon><Monitor /></el-icon>
          <template #title>终端</template>
        </el-menu-item>
        <el-menu-item index="/files">
          <el-icon><Folder /></el-icon>
          <template #title>文件管理</template>
        </el-menu-item>
        <el-menu-item index="/monitor">
          <el-icon><DataLine /></el-icon>
          <template #title>系统监控</template>
        </el-menu-item>
        <el-menu-item index="/processes">
          <el-icon><Cpu /></el-icon>
          <template #title>进程管理</template>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Document /></el-icon>
          <template #title>日志查看</template>
        </el-menu-item>
        <el-menu-item index="/services">
          <el-icon><Setting /></el-icon>
          <template #title>服务管理</template>
        </el-menu-item>
        <el-menu-item index="/cron">
          <el-icon><Timer /></el-icon>
          <template #title>定时任务</template>
        </el-menu-item>
        <el-menu-item index="/settings">
          <el-icon><Tools /></el-icon>
          <template #title>服务器管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="right-container">
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="isCollapsed = !isCollapsed"
          >
            <Fold v-if="!isCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-select
            v-model="selectedServerId"
            placeholder="选择服务器"
            class="server-select"
            @change="handleServerChange"
          >
            <el-option
              v-for="server in serverStore.servers"
              :key="server.id"
              :label="server.name"
              :value="server.id"
            >
              <div class="server-option">
                <span
                  class="status-dot"
                  :class="{
                    online: server.status === 'online',
                    offline: server.status === 'offline',
                  }"
                ></span>
                <span>{{ server.name }}</span>
                <span class="server-host">{{ server.host }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-area">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import useUserStore from '@/stores/user'
import useServerStore from '@/stores/server'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const serverStore = useServerStore()

const isCollapsed = ref(false)
const selectedServerId = ref<number>(serverStore.currentServerId)

const activeMenu = computed(() => {
  return route.path
})

onMounted(async () => {
  try {
    await serverStore.fetchServers()
    if (serverStore.servers.length > 0 && !serverStore.currentServerId) {
      serverStore.setCurrentServer(serverStore.servers[0].id)
      selectedServerId.value = serverStore.servers[0].id
    } else {
      selectedServerId.value = serverStore.currentServerId
    }
  } catch {
    // 忽略错误
  }
  try {
    if (userStore.isLoggedIn && !userStore.userInfo) {
      await userStore.fetchUserInfo()
    }
  } catch {
    // 忽略错误
  }
})

function handleServerChange(id: number) {
  serverStore.setCurrentServer(id)
}

function handleUserCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
  }
}
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
  width: 100%;
  overflow: hidden;
}

.aside {
  background-color: #1f1f1f;
  border-right: 1px solid #2b2b2c;
  transition: width 0.3s;
  overflow: hidden;

  .logo-area {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    border-bottom: 1px solid #2b2b2c;

    .logo-text {
      font-size: 16px;
      font-weight: 600;
      color: #e5eaf3;
      white-space: nowrap;
    }
  }

  .side-menu {
    border-right: none;
    height: calc(100vh - 60px);
    overflow-y: auto;
  }
}

.right-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header {
  height: 60px;
  background-color: #1f1f1f;
  border-bottom: 1px solid #2b2b2c;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;

    .collapse-btn {
      cursor: pointer;
      color: #bfcbd9;
      transition: color 0.3s;

      &:hover {
        color: #409eff;
      }
    }

    .server-select {
      width: 240px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;

    .user-area {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      padding: 4px 8px;
      border-radius: 4px;
      transition: background-color 0.3s;

      &:hover {
        background-color: #262626;
      }

      .user-avatar {
        background-color: #409eff;
        color: #fff;
        font-size: 14px;
      }

      .user-name {
        color: #e5eaf3;
        font-size: 14px;
      }
    }
  }
}

.main-content {
  background-color: #141414;
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.server-option {
  display: flex;
  align-items: center;
  gap: 8px;

  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;

    &.online {
      background-color: #67c23a;
    }

    &.offline {
      background-color: #f56c6c;
    }
  }

  .server-host {
    color: #a3a6ad;
    font-size: 12px;
    margin-left: auto;
  }
}
</style>
