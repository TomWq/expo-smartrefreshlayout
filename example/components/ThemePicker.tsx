import { Pressable, Text, View } from 'react-native';

import type { HeaderTheme } from '../data';
import { styles } from '../styles';

type ThemePickerProps = {
  themes: HeaderTheme[];
  selectedId: string;
  onSelect: (id: string) => void;
};

export function ThemePicker({ themes, selectedId, onSelect }: ThemePickerProps) {
  const selectedTheme = themes.find((theme) => theme.id === selectedId) ?? themes[0];

  return (
    <View style={styles.themeRow}>
      <Text style={styles.optionLabel}>主题</Text>
      <View style={styles.swatchGroup}>
        {themes.map((theme) => (
          <Pressable
            key={theme.id}
            accessibilityRole="button"
            accessibilityLabel={`使用${theme.label}主题`}
            style={[styles.swatchButton, selectedId === theme.id && styles.swatchButtonActive]}
            onPress={() => onSelect(theme.id)}
          >
            <View style={[styles.swatch, { backgroundColor: theme.primary }]} />
          </Pressable>
        ))}
      </View>
      <Text style={styles.themeName}>{selectedTheme.label}</Text>
    </View>
  );
}
