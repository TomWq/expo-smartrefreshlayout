#import "ExpoSmartRefreshHeaderSlotView.h"

#import <React/RCTComponentViewFactory.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/ComponentDescriptors.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/Props.h>
#import <react/renderer/components/ExpoSmartRefreshLayoutSpec/RCTComponentViewHelpers.h>

using namespace facebook::react;

@interface ExpoSmartRefreshHeaderSlotView () <RCTExpoSmartRefreshHeaderSlotViewProtocol>
@end

@implementation ExpoSmartRefreshHeaderSlotView

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
  return concreteComponentDescriptorProvider<ExpoSmartRefreshHeaderSlotComponentDescriptor>();
}

+ (void)load
{
  [RCTComponentViewFactory.currentComponentViewFactory registerComponentViewClass:self];
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const ExpoSmartRefreshHeaderSlotProps>();
    _props = defaultProps;
    self.clipsToBounds = YES;
  }
  return self;
}

@end
