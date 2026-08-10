# expo-smartrefreshlayout documentation

The documentation site is built with VitePress and deployed to GitHub Pages.

```bash
npm install
npm run docs:dev
```

Create a production build with:

```bash
npm run docs:build
npm run docs:preview
```

GitHub Actions publishes `docs/.vitepress/dist` when `website/**` or the Pages
workflow changes on `main`.
