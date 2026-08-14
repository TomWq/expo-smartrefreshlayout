#import "ExpoSmartSecondFloorSlotViews.h"

#import <React/RCTComponentViewFactory.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/ComponentDescriptors.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/RCTComponentViewHelpers.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/Props.h>

using namespace facebook::react;

@interface ExpoSmartSecondFloorContentSlotView () <RCTExpoSmartSecondFloorContentSlotViewProtocol>
@end

@implementation ExpoSmartSecondFloorContentSlotView

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<ExpoSmartSecondFloorContentSlotComponentDescriptor>();
}

+ (void)load
{
  [RCTComponentViewFactory.currentComponentViewFactory registerComponentViewClass:self];
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ExpoSmartSecondFloorContentSlotProps>();
    _props = defaultProps;
  }
  return self;
}

@end

@interface ExpoSmartSecondFloorFloorSlotView () <RCTExpoSmartSecondFloorFloorSlotViewProtocol>
@end

@implementation ExpoSmartSecondFloorFloorSlotView

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<ExpoSmartSecondFloorFloorSlotComponentDescriptor>();
}

+ (void)load
{
  [RCTComponentViewFactory.currentComponentViewFactory registerComponentViewClass:self];
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ExpoSmartSecondFloorFloorSlotProps>();
    _props = defaultProps;
  }
  return self;
}

@end

@interface ExpoSmartSecondFloorFloorContentSlotView () <RCTExpoSmartSecondFloorFloorContentSlotViewProtocol>
@end

@implementation ExpoSmartSecondFloorFloorContentSlotView

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<ExpoSmartSecondFloorFloorContentSlotComponentDescriptor>();
}

+ (void)load
{
  [RCTComponentViewFactory.currentComponentViewFactory registerComponentViewClass:self];
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ExpoSmartSecondFloorFloorContentSlotProps>();
    _props = defaultProps;
  }
  return self;
}

@end
