import { Stack } from 'expo-router';

export default function RootLayout() {
  return (
    <Stack
      screenOptions={{
        headerBackTitle: '返回',
        headerTintColor: '#1f1f1f',
        headerShadowVisible: false,
        headerStyle: { backgroundColor: '#ffffff' },
        contentStyle: { backgroundColor: '#f5f7fa' },
      }}
    >
      <Stack.Screen name="index" options={{ title: '示例目录' }} />
      <Stack.Screen name="classic" options={{ title: 'Classic 配置' }} />
      <Stack.Screen name="material" options={{ title: 'Material 配置' }} />
      <Stack.Screen name="lottie" options={{ title: 'Lottie 刷新' }} />
      <Stack.Screen name="second-floor" options={{ title: '淘宝二楼' }} />
    </Stack>
  );
}
