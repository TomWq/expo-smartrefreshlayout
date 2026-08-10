import DefaultTheme from 'vitepress/theme'
import RefreshHome from './components/RefreshHome.vue'
import './custom.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('RefreshHome', RefreshHome)
  },
}
