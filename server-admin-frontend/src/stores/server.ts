import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getServerList, type ServerInfo } from '@/api/server'

const useServerStore = defineStore('server', () => {
  const servers = ref<ServerInfo[]>([])
  const currentServerId = ref<number>(
    Number(localStorage.getItem('currentServerId')) || 0
  )

  const currentServer = computed(() => {
    return servers.value.find((s) => s.id === currentServerId.value) || null
  })

  /** 获取服务器列表 */
  async function fetchServers() {
    const res = await getServerList()
    servers.value = res
    // 如果当前选中的服务器不在列表中，选择第一个
    if (currentServerId.value && !res.find((s) => s.id === currentServerId.value)) {
      if (res.length > 0) {
        setCurrentServer(res[0].id)
      }
    }
    return res
  }

  /** 设置当前服务器 */
  function setCurrentServer(id: number) {
    currentServerId.value = id
    localStorage.setItem('currentServerId', String(id))
  }

  /** 获取当前服务器 ID */
  function getCurrentServerId(): number {
    return currentServerId.value
  }

  return {
    servers,
    currentServerId,
    currentServer,
    fetchServers,
    setCurrentServer,
    getCurrentServerId,
  }
})

export default useServerStore
