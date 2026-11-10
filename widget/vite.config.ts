import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig(({ command }) => ({
  plugins: [react()],

  // Library mode leaves process.env.NODE_ENV for the host's bundler; a <script> has none, so set it here
  // (also selects React's small production build)
  define: command === 'build' ? { 'process.env.NODE_ENV': JSON.stringify('production') } : {},

  build: {
    // One self-contained file (React included) that any page loads with a <script> tag
    lib: {
      entry: 'src/element.tsx',
      name: 'ApplianceStoreWidget',
      formats: ['iife'],
      fileName: () => 'store-widget.js',
    },
    outDir: 'dist',
    target: 'es2022',
    sourcemap: true,
  },

  // `npm run dev`: index.html is a fake third-party page; API calls go to the app on :8080
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },

  test: {
    environment: 'jsdom',
    setupFiles: ['src/test/setup.ts'],
    restoreMocks: true,
  },
}));
