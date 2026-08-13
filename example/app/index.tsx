import { Link } from 'expo-router';
import { Pressable, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { styles } from '../styles';

const demos = [
  {
    href: '/classic' as const,
    title: 'Classic 配置',
    description: 'Spinner、最后更新时间、主题色和上拉分页。',
  },
  {
    href: '/material' as const,
    title: 'Material 配置',
    description: '贝塞尔背景、内容偏移与 Material 主题。',
  },
  {
    href: '/lottie' as const,
    title: 'Lottie 刷新',
    description: '自定义原生 Header，Lottie 进度跟随下拉位移。',
  },
  {
    href: '/custom-header' as const,
    title: '自定义 Header 验证',
    description: '切换高度、布局、倍率和完成时长，查看原生事件与实时几何。',
  },
  {
    href: '/second-floor' as const,
    title: '淘宝二楼',
    description: 'Android 二楼下拉体验；其他平台显示支持提示。',
  },
];

export default function ExampleDirectoryPage() {
  return (
    <SafeAreaView style={styles.screen} edges={['bottom']}>
      <View style={styles.homeContent}>
        <Text style={styles.homeIntro}>选择一个演示，查看对应的原生刷新配置与交互。</Text>
        {demos.map((demo) => (
          <Link key={demo.href} href={demo.href} asChild>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel={`打开${demo.title}示例`}
              style={styles.demoRow}
            >
              <View style={styles.demoRowCopy}>
                <Text style={styles.demoRowTitle}>{demo.title}</Text>
                <Text style={styles.demoRowText}>{demo.description}</Text>
              </View>
              <Text style={styles.demoRowArrow}>{'>'}</Text>
            </Pressable>
          </Link>
        ))}
      </View>
    </SafeAreaView>
  );
}
