import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  define: {
    global: 'window',
    'import.meta.env.VITE_API_URL': JSON.stringify('https://codeeditor-production-f2d2.up.railway.app'),
    'import.meta.env.VITE_WS_URL': JSON.stringify('https://codeeditor-production-f2d2.up.railway.app'),
  },
})
