const CACHE_NAME = 'corrida-cache-v1';
const urlsToCache = [
  '/',
  '/login',
  '/css/style.css', // ajuste para o caminho real do seu CSS principal
  '/js/main.js',    // ajuste para o caminho real do seu JS principal
  '/corrida.ico'
];

// Instala o Service Worker e guarda os arquivos essenciais no cache
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(urlsToCache))
  );
});

// Intercepta as requisições: tenta buscar na rede, se falhar (offline), busca no cache
self.addEventListener('fetch', event => {
  // Ignora requisições POST (como login/cadastro) para não quebrar o envio de dados
  if (event.request.method !== 'GET') return;

  event.respondWith(
    caches.match(event.request)
      .then(response => {
        return response || fetch(event.request).catch(() => {
          // Opcional: retornar uma página customizada de "Você está offline" aqui
        });
      })
  );
});