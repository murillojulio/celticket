import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import react from '@vitejs/plugin-react'
import type { Plugin } from 'vite'
import { defineConfig } from 'vite'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

/** Sirve ../layout_builder.html en /layout-builder durante el dev del admin */
function layoutBuilderDevPlugin(): Plugin {
  return {
    name: 'layout-builder-dev',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = req.url?.split('?')[0]
        if (url === '/layout-builder') {
          const file = path.resolve(__dirname, '../layout_builder.html')
          res.setHeader('Content-Type', 'text/html; charset=utf-8')
          res.end(fs.readFileSync(file))
          return
        }
        next()
      })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), layoutBuilderDevPlugin()],
  base: process.env.NODE_ENV === 'production' ? '/admin/' : '/',
  /** Sin VITE_API_BASE_URL, el cliente usa URLs relativas /api/... y Vite las reenvía al backend local. */
  server: {
    proxy: {
      '/api': {
        target: 'https://jainnovatedev.uk',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: path.resolve(__dirname, '../admin-dist'),
    emptyOutDir: true,
  },
})
