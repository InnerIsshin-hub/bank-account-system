import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,                 // 前端端口
    proxy: {
      '/api': {                 // 匹配所有以 /api 开头的请求
        target: 'http://localhost:8080',  // 后端地址
        changeOrigin: true,     // 修改请求头中的 Origin 为 target
        // 如果你的后端接口路径本身就包含 /api，就不需要 rewrite
        // 如果你的后端接口没有 /api 前缀，才需要下面这行
        // rewrite: (path) => path.replace(/^\/api/, '')
      }
    }
  }
})