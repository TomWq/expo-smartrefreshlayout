<script setup lang="ts">
import { computed } from 'vue'
import { withBase } from 'vitepress'

const props = withDefaults(defineProps<{ locale?: 'zh' | 'en' }>(), {
  locale: 'zh',
})

const content = computed(() =>
  props.locale === 'en'
    ? {
        eyebrow: 'Native refresh interactions for React Native 0.76+',
        title: 'expo-smartrefreshlayout',
        lead:
          'Fabric-native pull-to-refresh and load-more behavior for Android and iOS, with predictable completion for gesture and programmatic requests.',
        start: 'Quick start',
        api: 'View API',
        npm: 'Open on npm',
        featuresLabel: 'One container, a complete request lifecycle',
        featuresTitle: 'Native motion with predictable JavaScript state.',
        featuresLead:
          'Promise-aware callbacks, controlled state, instance commands, and stale-request protection keep gesture and network state aligned.',
        features: [
          ['01', 'Pull to refresh', 'Classic and Material headers, shared messages, colors, haptics, and completion behavior.'],
          ['02', 'Load more', 'Pull-release by default or guarded automatic loading after a real upward scroll.'],
          ['03', 'Android second-floor interaction', 'Android TwoLevelHeader provides a separate full-screen interaction with open and close commands.'],
        ],
        flowLabel: 'Request lifecycle',
        flowTitle: 'Accept once. Finish the matching request.',
        phases: [
          ['01', 'Gesture or command', 'Native accepts a refresh or page request only while the instance is idle.'],
          ['02', 'JavaScript work', 'The callback receives a requestId and may return a Promise or the next hasMore value.'],
          ['03', 'Native completion', 'Uncontrolled mode closes automatically; controlled mode follows your prop state.'],
        ],
        installLabel: 'Install',
        installTitle: 'Add one Fabric component.',
        installNote: 'New Architecture is required. Expo projects need a development build; Expo Go is not supported.',
        installLink: 'Read the setup guide',
      }
    : {
        eyebrow: '适用于 React Native 0.76+ 的原生刷新交互',
        title: 'expo-smartrefreshlayout',
        lead:
          '为 Android 与 iOS 提供 Fabric 原生下拉刷新和上拉加载，让手势、Promise 与实例命令保持一致。',
        start: '快速开始',
        api: '查看 API',
        npm: '前往 npm',
        featuresLabel: '一个容器，完整的请求生命周期',
        featuresTitle: '原生手势，清晰可控的 JavaScript 状态。',
        featuresLead:
          'Promise 回调、受控状态、实例命令和过期请求保护，让网络请求与原生动画始终对应。',
        features: [
          ['01', '下拉刷新', 'Classic 与 Material Header，共享文案、颜色、触觉反馈和结束行为。'],
          ['02', '上拉加载', '默认上拉释放触发，也支持在真实向上滚动后安全解锁自动加载。'],
          ['03', 'Android 二楼交互', '基于 TwoLevelHeader 的独立全屏交互，支持打开和关闭命令。'],
        ],
        flowLabel: '请求生命周期',
        flowTitle: '只接受一次，只结束对应请求。',
        phases: [
          ['01', '手势或实例命令', '原生实例仅在空闲时接受刷新或分页请求。'],
          ['02', 'JavaScript 请求', '回调收到 requestId，可返回 Promise 或下一页的 hasMore。'],
          ['03', '原生动画结束', '非受控模式自动收起；受控模式跟随调用方的 Prop 状态。'],
        ],
        installLabel: '安装',
        installTitle: '接入一个 Fabric 组件。',
        installNote: '必须启用新架构。Expo 项目需要 development build，不支持 Expo Go。',
        installLink: '阅读接入指南',
      }
)

const prefix = props.locale === 'en' ? '/en' : ''
const docsLink = (path: string) => withBase(`${prefix}${path}`)
</script>

<template>
  <main class="refresh-home">
    <section class="refresh-hero">
      <div class="refresh-hero__inner">
        <div class="refresh-hero__copy">
          <p class="refresh-eyebrow">{{ content.eyebrow }}</p>
          <h1>{{ content.title }}</h1>
          <p class="refresh-hero__lead">{{ content.lead }}</p>
          <div class="refresh-actions">
            <a class="refresh-action refresh-action--primary" :href="docsLink('/guide/getting-started')">
              {{ content.start }}
            </a>
            <a class="refresh-action" :href="docsLink('/api/')">{{ content.api }}</a>
            <a class="refresh-text-link" href="https://www.npmjs.com/package/expo-smartrefreshlayout">
              {{ content.npm }}
            </a>
          </div>
          <div class="refresh-compat" aria-label="Compatibility">
            <span>Fabric</span>
            <span>Android</span>
            <span>iOS</span>
            <span>Expo dev build</span>
          </div>
        </div>
        <div class="refresh-hero__visual" aria-hidden="true">
          <div class="refresh-signal">
            <div class="refresh-signal__backdrop"></div>
            <div class="refresh-signal__device">
              <div class="refresh-signal__notch"></div>
              <div class="refresh-signal__header">
                <div class="refresh-signal__loop"></div>
                <div class="refresh-signal__pulse">
                  <i></i>
                  <i></i>
                  <i></i>
                </div>
              </div>
              <div class="refresh-signal__feed">
                <span></span>
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div class="refresh-signal__footer">
                <i></i>
                <i></i>
                <i></i>
              </div>
            </div>
            <div class="refresh-signal__orbit refresh-signal__orbit--one"></div>
            <div class="refresh-signal__orbit refresh-signal__orbit--two"></div>
          </div>
        </div>
      </div>
    </section>

    <section class="refresh-section">
      <div class="refresh-section__intro">
        <p class="refresh-section__label">{{ content.featuresLabel }}</p>
        <h2>{{ content.featuresTitle }}</h2>
        <p>{{ content.featuresLead }}</p>
      </div>
      <div class="refresh-feature-grid">
        <article v-for="feature in content.features" :key="feature[0]" class="refresh-feature">
          <span>{{ feature[0] }}</span>
          <h3>{{ feature[1] }}</h3>
          <p>{{ feature[2] }}</p>
        </article>
      </div>
    </section>

    <section class="refresh-section refresh-flow">
      <div class="refresh-section__intro">
        <p class="refresh-section__label">{{ content.flowLabel }}</p>
        <h2>{{ content.flowTitle }}</h2>
      </div>
      <ol class="refresh-flow__steps">
        <li v-for="phase in content.phases" :key="phase[0]">
          <span>{{ phase[0] }}</span>
          <h3>{{ phase[1] }}</h3>
          <p>{{ phase[2] }}</p>
        </li>
      </ol>
    </section>

    <section class="refresh-install">
      <div>
        <p class="refresh-section__label">{{ content.installLabel }}</p>
        <h2>{{ content.installTitle }}</h2>
        <p>{{ content.installNote }}</p>
      </div>
      <div class="refresh-install__command">
        <code>npm install expo-smartrefreshlayout@next</code>
        <a :href="docsLink('/guide/getting-started')">{{ content.installLink }}</a>
      </div>
    </section>
  </main>
</template>
