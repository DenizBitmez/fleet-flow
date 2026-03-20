import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/ws-tracking': {
        target: 'http://localhost:8083',
        ws: true,
        changeOrigin: true
      },

      '/courier': {
        target: 'http://localhost:8080'
      },
      '/matching': {
        target: 'http://localhost:8080'
      },
      '/auth': {
        target: 'http://localhost:8080'
      }
    }
  }
})
