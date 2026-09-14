/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** 백엔드 오리진. 미설정 시 http://localhost:8081 (로컬 docker-compose 앱 포트). */
  readonly VITE_API_ORIGIN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
