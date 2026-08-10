import { defineConfig, type HeadConfig, type TransformContext } from 'vitepress'

const repository = 'https://github.com/TomWq/expo-smartrefreshlayout'
const siteBase = '/expo-smartrefreshlayout/'
const siteOrigin = `https://tomwq.github.io${siteBase}`
const previewImage = `${siteOrigin}refresh-preview.png`

function pageRoute(relativePath: string): string {
  if (relativePath === 'index.md') return '/'
  if (relativePath.endsWith('/index.md')) {
    return `/${relativePath.slice(0, -'index.md'.length)}`
  }
  return `/${relativePath.replace(/\.md$/, '')}`
}

function canonicalUrl(route: string): string {
  return `${siteOrigin}${route.replace(/^\//, '')}`
}

function pageDescription(context: TransformContext): string {
  if (context.pageData.description) return context.pageData.description
  return context.pageData.relativePath.startsWith('en/')
    ? 'Native pull-to-refresh, load-more, and Android second-floor interactions for React Native Fabric.'
    : '面向 React Native Fabric 的原生下拉刷新、上拉加载与 Android 二楼交互组件。'
}

function seoHead(context: TransformContext): HeadConfig[] {
  const route = pageRoute(context.pageData.relativePath)
  const isEnglish = route.startsWith('/en/')
  const alternateRoute = isEnglish
    ? route.replace(/^\/en/, '') || '/'
    : route === '/'
      ? '/en/'
      : `/en${route}`
  const canonical = canonicalUrl(route)
  const alternate = canonicalUrl(alternateRoute)
  const title = context.pageData.title || 'expo-smartrefreshlayout'
  const description = pageDescription(context)

  return [
    ['link', { rel: 'canonical', href: canonical }],
    ['link', { rel: 'alternate', hreflang: isEnglish ? 'en' : 'zh-CN', href: canonical }],
    ['link', { rel: 'alternate', hreflang: isEnglish ? 'zh-CN' : 'en', href: alternate }],
    ['link', { rel: 'alternate', hreflang: 'x-default', href: canonicalUrl('/') }],
    ['meta', { property: 'og:title', content: title }],
    ['meta', { property: 'og:description', content: description }],
    ['meta', { property: 'og:url', content: canonical }],
    ['meta', { property: 'og:locale', content: isEnglish ? 'en_US' : 'zh_CN' }],
    ['meta', { name: 'twitter:title', content: title }],
    ['meta', { name: 'twitter:description', content: description }],
  ]
}

const zhSidebar = [
  {
    text: '开始使用',
    items: [
      { text: '快速开始', link: '/guide/getting-started' },
      { text: '兼容性', link: '/guide/compatibility' },
    ],
  },
  {
    text: '核心能力',
    items: [
      { text: '刷新与分页', link: '/guide/refresh-and-load-more' },
      { text: '样式与文案', link: '/guide/customization' },
      { text: 'Android 二楼', link: '/guide/second-floor' },
      { text: '平台说明', link: '/guide/platforms' },
    ],
  },
  {
    text: '实践',
    items: [
      { text: '完整示例', link: '/guide/examples' },
      { text: '从 v1 迁移', link: '/guide/migration' },
    ],
  },
  {
    text: '参考',
    items: [
      { text: 'API', link: '/api/' },
      { text: '故障排查', link: '/troubleshooting' },
    ],
  },
]

const enSidebar = [
  {
    text: 'Start',
    items: [
      { text: 'Quick start', link: '/en/guide/getting-started' },
      { text: 'Compatibility', link: '/en/guide/compatibility' },
    ],
  },
  {
    text: 'Core features',
    items: [
      { text: 'Refresh and load more', link: '/en/guide/refresh-and-load-more' },
      { text: 'Customization', link: '/en/guide/customization' },
      { text: 'Android second floor', link: '/en/guide/second-floor' },
      { text: 'Platforms', link: '/en/guide/platforms' },
    ],
  },
  {
    text: 'Practice',
    items: [
      { text: 'Examples', link: '/en/guide/examples' },
      { text: 'Migrate from v1', link: '/en/guide/migration' },
    ],
  },
  {
    text: 'Reference',
    items: [
      { text: 'API', link: '/en/api/' },
      { text: 'Troubleshooting', link: '/en/troubleshooting' },
    ],
  },
]

export default defineConfig({
  title: 'expo-smartrefreshlayout',
  description: 'React Native Fabric 下拉刷新与上拉加载组件文档',
  lang: 'zh-CN',
  base: siteBase,
  cleanUrls: true,
  lastUpdated: true,
  sitemap: { hostname: siteOrigin },
  transformHead: seoHead,
  head: [
    ['link', { rel: 'icon', type: 'image/png', href: `${siteBase}brand-icon.png` }],
    ['meta', { name: 'theme-color', content: '#0f766e' }],
    ['meta', { property: 'og:type', content: 'website' }],
    ['meta', { property: 'og:image', content: previewImage }],
    ['meta', { name: 'twitter:card', content: 'summary_large_image' }],
    ['meta', { name: 'twitter:image', content: previewImage }],
  ],
  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'expo-smartrefreshlayout',
      description: '面向 React Native Fabric 的原生刷新容器。',
      themeConfig: {
        nav: [
          { text: '首页', link: '/' },
          { text: '快速开始', link: '/guide/getting-started' },
          { text: 'Android 二楼', link: '/guide/second-floor' },
          { text: 'API', link: '/api/' },
        ],
        sidebar: zhSidebar,
        outline: { label: '本页内容', level: [2, 3] },
        docFooter: { prev: '上一页', next: '下一页' },
        lastUpdated: { text: '最后更新于' },
        returnToTopLabel: '返回顶部',
        sidebarMenuLabel: '目录',
        darkModeSwitchLabel: '外观',
        editLink: {
          pattern: `${repository}/edit/main/website/docs/:path`,
          text: '在 GitHub 上编辑此页',
        },
      },
    },
    en: {
      label: 'English',
      lang: 'en-US',
      link: '/en/',
      title: 'expo-smartrefreshlayout',
      description: 'Native refresh interactions for React Native Fabric.',
      themeConfig: {
        nav: [
          { text: 'Home', link: '/en/' },
          { text: 'Quick start', link: '/en/guide/getting-started' },
          { text: 'Android second floor', link: '/en/guide/second-floor' },
          { text: 'API', link: '/en/api/' },
        ],
        sidebar: enSidebar,
        outline: { label: 'On this page', level: [2, 3] },
        editLink: {
          pattern: `${repository}/edit/main/website/docs/:path`,
          text: 'Edit this page on GitHub',
        },
      },
    },
  },
  themeConfig: {
    logo: '/brand-icon.png',
    search: { provider: 'local' },
    socialLinks: [{ icon: 'github', link: repository }],
    footer: {
      message: 'Released under the MIT License.',
      copyright: 'Copyright © TomWq',
    },
  },
})
