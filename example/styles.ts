import { StyleSheet } from 'react-native';

export const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#f5f7fa',
  },
  homeContent: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 20,
  },
  homeIntro: {
    marginBottom: 14,
    color: '#8c8c8c',
    fontSize: 13,
    lineHeight: 20,
  },
  demoRow: {
    minHeight: 76,
    marginBottom: 10,
    paddingHorizontal: 16,
    paddingVertical: 13,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ffffff',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#e8e8e8',
    borderRadius: 6,
  },
  demoRowCopy: {
    flex: 1,
    paddingRight: 12,
  },
  demoRowTitle: {
    color: '#262626',
    fontSize: 16,
    fontWeight: '600',
  },
  demoRowText: {
    marginTop: 4,
    color: '#8c8c8c',
    fontSize: 12,
    lineHeight: 18,
  },
  demoRowArrow: {
    color: '#8c8c8c',
    fontSize: 23,
    lineHeight: 25,
  },
  preview: {
    flex: 1,
  },
  toolbar: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 10,
    backgroundColor: '#ffffff',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#d9d9d9',
  },
  headingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  headingCopy: {
    flex: 1,
  },
  title: {
    color: '#1f1f1f',
    fontSize: 20,
    fontWeight: '700',
  },
  subtitle: {
    marginTop: 3,
    color: '#8c8c8c',
    fontSize: 12,
  },
  configurationBand: {
    marginTop: 10,
  },
  countBadge: {
    minWidth: 54,
    paddingVertical: 5,
    paddingHorizontal: 8,
    alignItems: 'center',
    backgroundColor: '#e6f4ff',
    borderRadius: 6,
  },
  countValue: {
    color: '#0958d9',
    fontSize: 18,
    fontWeight: '700',
  },
  countLabel: {
    color: '#1677ff',
    fontSize: 11,
  },
  actionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 12,
    gap: 8,
  },
  primaryButton: {
    paddingVertical: 9,
    paddingHorizontal: 14,
    backgroundColor: '#1677ff',
    borderRadius: 6,
  },
  primaryButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '600',
  },
  secondaryButton: {
    paddingVertical: 9,
    paddingHorizontal: 14,
    backgroundColor: '#f0f5ff',
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#adc6ff',
    borderRadius: 6,
  },
  secondaryButtonText: {
    color: '#0958d9',
    fontSize: 14,
    fontWeight: '600',
  },
  failureSwitch: {
    flexDirection: 'row',
    alignItems: 'center',
    marginLeft: 'auto',
  },
  switchLabel: {
    color: '#595959',
    fontSize: 12,
  },
  optionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 10,
    gap: 7,
  },
  switchRow: {
    minHeight: 34,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  optionLabel: {
    color: '#8c8c8c',
    fontSize: 11,
  },
  themeRow: {
    minHeight: 32,
    flexDirection: 'row',
    alignItems: 'center',
  },
  swatchGroup: {
    flexDirection: 'row',
    marginLeft: 10,
  },
  swatchButton: {
    width: 26,
    height: 26,
    marginRight: 7,
    padding: 3,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: 'transparent',
    borderRadius: 4,
  },
  swatchButtonActive: {
    borderColor: '#1677ff',
  },
  swatch: {
    flex: 1,
    borderRadius: 2,
  },
  themeName: {
    marginLeft: 'auto',
    color: '#595959',
    fontSize: 12,
  },
  segmentedControl: {
    flexDirection: 'row',
    flex: 1,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: '#d9d9d9',
    borderRadius: 5,
    overflow: 'hidden',
  },
  segment: {
    flex: 1,
    minHeight: 30,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#ffffff',
  },
  segmentActive: {
    backgroundColor: '#e6f4ff',
  },
  segmentText: {
    color: '#8c8c8c',
    fontSize: 11,
  },
  segmentTextActive: {
    color: '#0958d9',
    fontWeight: '600',
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 26,
    marginTop: 8,
  },
  stateDot: {
    width: 7,
    height: 7,
    marginRight: 7,
    borderRadius: 4,
    backgroundColor: '#fa8c16',
  },
  stateDotIdle: {
    backgroundColor: '#52c41a',
  },
  statusText: {
    flex: 1,
    color: '#595959',
    fontSize: 11,
  },
  nativeState: {
    marginLeft: 8,
    color: '#8c8c8c',
    fontSize: 10,
  },
  refreshLayout: {
    flex: 1,
  },
  list: {
    flex: 1,
  },
  listContent: {
    padding: 12,
    paddingBottom: 28,
  },
  listHeader: {
    marginBottom: 10,
    padding: 14,
    backgroundColor: '#ffffff',
    borderRadius: 6,
  },
  listHeaderTitle: {
    color: '#262626',
    fontSize: 15,
    fontWeight: '600',
  },
  listHeaderText: {
    marginTop: 4,
    color: '#8c8c8c',
    fontSize: 12,
    lineHeight: 18,
  },
  itemRow: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 68,
    marginBottom: 8,
    padding: 14,
    backgroundColor: '#ffffff',
    borderRadius: 6,
  },
  itemMarker: {
    width: 4,
    height: 38,
    marginRight: 12,
    borderRadius: 2,
  },
  itemCopy: {
    flex: 1,
  },
  itemTitle: {
    color: '#262626',
    fontSize: 15,
    fontWeight: '600',
  },
  itemDetail: {
    marginTop: 4,
    color: '#8c8c8c',
    fontSize: 12,
  },
  listFooter: {
    alignItems: 'center',
    paddingVertical: 14,
  },
  listFooterText: {
    color: '#8c8c8c',
    fontSize: 12,
    textAlign: 'center',
  },
  lottieListContent: {
    padding: 12,
    paddingBottom: 28,
  },
  lottieHeader: {
    height: 80,
    alignItems: 'center',
    justifyContent: 'center',
    // backgroundColor: '#e6f4ff',
  },
  lottieAnimation: {
    width: 64,
    height: 64,
  },
  lottieHeaderText: {
    marginTop: -4,
    color: '#0958d9',
    fontSize: 11,
    fontWeight: '600',
  },
  taobaoPage: {
    flex: 1,
  },
  taobaoScroll: {
    flex: 1,
    backgroundColor: 'transparent',
  },
  taobaoHomeImage: {
    width: '100%',
    aspectRatio: 400 / 1073,
  },
  taobaoFloorImage: {
    ...StyleSheet.absoluteFillObject,
    width: '100%',
    height: '100%',
  },
  androidOnlyNotice: {
    flex: 1,
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#ffffff',
  },
  androidOnlyTitle: {
    color: '#262626',
    fontSize: 20,
    fontWeight: '700',
    textAlign: 'center',
  },
  androidOnlyText: {
    marginTop: 10,
    color: '#8c8c8c',
    fontSize: 13,
    lineHeight: 20,
    textAlign: 'center',
  },
});
