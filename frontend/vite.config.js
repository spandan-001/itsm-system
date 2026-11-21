import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', ['ITSM_', 'VITE_SUPABASE_']);
  if (env.VITE_SUPABASE_PUBLISHABLE_KEY && !env.VITE_SUPABASE_PUBLISHABLE_KEY.startsWith('sb_publishable_')) {
    throw new Error('VITE_SUPABASE_PUBLISHABLE_KEY must be a Supabase publishable key.');
  }
  const target = env.ITSM_BACKEND_URL || 'http://127.0.0.1:8080';
  const proxy = { '/api': { target, changeOrigin: true } };
  return {
    plugins: [react()],
    server: { host: 'localhost', port: 3000, strictPort: true, proxy },
    preview: { host: 'localhost', port: 3000, strictPort: true, proxy },
  };
});
