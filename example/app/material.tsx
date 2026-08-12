import { useState } from 'react';
import { Switch, Text, View } from 'react-native';

import { RefreshPreview } from '../components/RefreshPreview';
import { ThemePicker } from '../components/ThemePicker';
import { MATERIAL_THEMES } from '../data';
import { styles } from '../styles';

export default function MaterialConfigurationPage() {
  const [showBezierWave, setShowBezierWave] = useState(false);
  const [translateContent, setTranslateContent] = useState(false);
  const [themeId, setThemeId] = useState('blue');
  const theme = MATERIAL_THEMES.find((item) => item.id === themeId) ?? MATERIAL_THEMES[0];

  return (
    <RefreshPreview
      title="Material 配置"
      subtitle="MaterialHeader：贝塞尔背景、内容偏移与进度圆主题"
      headerStyle="material"
      primaryColor={theme.primary}
      indicatorColor={theme.indicator}
      titleColor={theme.title}
      materialShowBezierWave={showBezierWave}
      materialEnableHeaderTranslationContent={translateContent}
      materialProgressBackgroundColor={theme.primary}
      loadMoreEnabled
      controls={
        <>
          <View style={styles.switchRow}>
            <Text style={styles.optionLabel}>显示贝塞尔背景</Text>
            <Switch
              value={showBezierWave}
              onValueChange={setShowBezierWave}
              trackColor={{ false: '#d9d9d9', true: '#91caff' }}
              thumbColor="#ffffff"
            />
          </View>
          <View style={styles.switchRow}>
            <Text style={styles.optionLabel}>内容跟随 Header 偏移</Text>
            <Switch
              value={translateContent}
              onValueChange={setTranslateContent}
              trackColor={{ false: '#d9d9d9', true: '#91caff' }}
              thumbColor="#ffffff"
            />
          </View>
          <ThemePicker themes={MATERIAL_THEMES} selectedId={themeId} onSelect={setThemeId} />
        </>
      }
    />
  );
}
