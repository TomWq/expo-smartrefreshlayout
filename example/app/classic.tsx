import { useState } from 'react';
import { Pressable, Switch, Text, View } from 'react-native';

import { RefreshPreview } from '../components/RefreshPreview';
import { ThemePicker } from '../components/ThemePicker';
import { CLASSIC_THEMES } from '../data';
import type { ClassicSpinnerStyle } from '../data';
import { styles } from '../styles';

const spinnerOptions: Array<{ value: ClassicSpinnerStyle; label: string }> = [
  { value: 'scale', label: '拉伸' },
  { value: 'translate', label: '平移' },
  { value: 'fixed-behind', label: '固定背后' },
];

export default function ClassicConfigurationPage() {
  const [spinnerStyle, setSpinnerStyle] = useState<ClassicSpinnerStyle>('fixed-behind');
  const [enableLastTime, setEnableLastTime] = useState(true);
  const [themeId, setThemeId] = useState('default');
  const theme = CLASSIC_THEMES.find((item) => item.id === themeId) ?? CLASSIC_THEMES[0];

  return (
    <RefreshPreview
      title="Classic 配置"
      subtitle="ClassicsHeader：Spinner、最后更新时间与主题色"
      headerStyle="classic"
      primaryColor={theme.primary}
      indicatorColor={theme.indicator}
      titleColor={theme.title}
      classicSpinnerStyle={spinnerStyle}
      classicEnableLastTime={enableLastTime}
      loadMoreEnabled
      controls={
        <>
          <View style={styles.optionRow}>
            <Text style={styles.optionLabel}>Spinner</Text>
            <View style={styles.segmentedControl}>
              {spinnerOptions.map((option) => (
                <Pressable
                  key={option.value}
                  style={[styles.segment, spinnerStyle === option.value && styles.segmentActive]}
                  onPress={() => setSpinnerStyle(option.value)}
                >
                  <Text
                    style={[
                      styles.segmentText,
                      spinnerStyle === option.value && styles.segmentTextActive,
                    ]}
                  >
                    {option.label}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>
          <View style={styles.switchRow}>
            <Text style={styles.optionLabel}>显示最后更新时间</Text>
            <Switch
              value={enableLastTime}
              onValueChange={setEnableLastTime}
              trackColor={{ false: '#d9d9d9', true: '#91caff' }}
              thumbColor="#ffffff"
            />
          </View>
          <ThemePicker themes={CLASSIC_THEMES} selectedId={themeId} onSelect={setThemeId} />
        </>
      }
    />
  );
}
