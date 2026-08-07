//
//  UIRefreshHeader.m
//  Teecloud
//
//  Created by Teeyun on 2020/8/17.
//  Copyright © 2020 SCWANG. All rights reserved.
//

#import "UIRefreshHeader.h"

@interface UIRefreshComponent (UIRefreshHeader)

@property (nonatomic, assign) BOOL isExpanded;

@end

@interface UIRefreshHeader ()

@property (nonatomic, assign) BOOL isDrawed;
@property (nonatomic, assign) BOOL isSuccess;
@property (nonatomic, assign) UIRefreshStatus status;

@end

@implementation UIRefreshHeader

#pragma mark - Init

/**
 * 初始化参数
 */
- (void)setUpComponent {
    [super setUpComponent];
    self.staysBehindContent = NO;
    self.expandsContentInset = NO;
    //self.finishDuration = 0;
    //self.status = UIRefreshStatusIdle;
//    self.triggerRate = 1;
    self.nameForLastRefreshTime = @"上次刷新：";
    self.keyForLastRefreshTime = @"UIRefreshHeader_lastRefreshTime";
}

- (NSDate *)lastRefreshTime {
    return [NSUserDefaults.standardUserDefaults objectForKey:self.keyForLastRefreshTime];
}

- (void)setLastRefreshTime:(NSDate *)lastRefreshTime {
    [NSUserDefaults.standardUserDefaults setObject:lastRefreshTime forKey:self.keyForLastRefreshTime];
}

- (void)addTarget:(id)target action:(SEL)action forControlEvents:(UIControlEvents)controlEvents {
    NSString *pre = @"UIRefreshHeader_LastRefreshTime_%@";
    NSString *key = [NSString stringWithFormat:pre, NSStringFromClass([target class])];
    [self setKeyForLastRefreshTime:key];
    [super addTarget:target action:action forControlEvents:controlEvents];
}

- (void)layoutSubviews {
    [super layoutSubviews];
    if (self.scrollMode == UISmartScrollModeMove) {
        self.frame = CGRectMake(0, -self.height, self.width, self.height);
    }
}

#pragma mark - Getter

- (NSDateFormatter *)lastRefreshTimeFormatter {
    if (_lastRefreshTimeFormatter == nil) {
        NSDateFormatter* format = [NSDateFormatter new];
        format.dateFormat = @"M-d HH:mm";
        self.lastRefreshTimeFormatter = format;
    }
    return _lastRefreshTimeFormatter;
}

- (NSString *)textLastRefreshTime {
    NSString* time = @"无记录";
    NSDate *lastTime = self.lastRefreshTime;
    if (lastTime != nil) {
        time = [self.lastRefreshTimeFormatter stringFromDate:lastTime];
    }
    return [NSString stringWithFormat:@"%@%@", self.nameForLastRefreshTime, time];
}

- (BOOL)isRefreshing {
    return [self inStatus:UIRefreshStatusWillRefresh,UIRefreshStatusReleasing,UIRefreshStatusRefreshing,0];
}

- (BOOL)inStatus:(UIRefreshStatus)status, ... {
    va_list args;
    va_start(args, status);
    UIRefreshStatus arg = status;
    do {
        if (self.status == arg) {
            return true;
        }
        arg = va_arg(args, UIRefreshStatus);
    } while (arg != 0);
    va_end(args);
    return false;
}

- (void)setStatus:(UIRefreshStatus)status {
    UIRefreshStatus old = _status;

    _status = status;
    
    BOOL changed = old != status;
    UIScrollView* scrollView = self.scrollView;
    if (changed && scrollView != nil) {
        [self onStatus:old changed:status];
//        [self scrollView:scrollView didChange:old status:status];
    }
}

#pragma mark - Method

- (void)scrollView:(UIScrollView *)scrollView attached:(BOOL)attach {
    [super scrollView:scrollView attached:attach];
    CGPoint contentOffset = scrollView.contentOffset;
    [self scrollView:scrollView didChange:contentOffset contentOffset:contentOffset];
}

- (void)scrollView:(UIScrollView *)scrollView detached:(BOOL)detach {
    if (self.isExpanded &&
        (self.scrollMode != UISmartScrollModeFront || self.expandsContentInset)) {
        UIEdgeInsets inset = scrollView.contentInset;
        inset.top -= self.expandHeight;
        scrollView.contentInset = inset;
        self.isExpanded = FALSE;
    }
    [super scrollView:scrollView detached:detach];
}

- (void)scrollView:(UIScrollView *)scrollView didScroll:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL) isDragging {
    
    const UIRefreshStatus status = self.status;
    
    if ([self inStatus:UIRefreshStatusIdle,UIRefreshStatusPullToRefresh,UIRefreshStatusReleaseToRefresh,0]) {
        if (isDragging) {
            if (status != UIRefreshStatusReleaseToRefresh && percent >= 1) {
                self.status = UIRefreshStatusReleaseToRefresh;
            } else if (status != UIRefreshStatusPullToRefresh && percent < 1) {
                self.status = UIRefreshStatusPullToRefresh;
            }
        } else if (status == UIRefreshStatusReleaseToRefresh) {
            [self beginRefresh];
        } else if (status != UIRefreshStatusFinish && status != UIRefreshStatusReleasing) {
            self.status = UIRefreshStatusIdle;
        }
    }

    [self onScrollingWithOffset:offset percent:percent drag:isDragging];
}

- (void)scrollView:(UIScrollView *)scrollView didChange:(UIRefreshStatus)old status:(UIRefreshStatus)status {
    if (old == UIRefreshStatusIdle && self.scrollMode == UISmartScrollModeFront && !self.staysBehindContent) {
        [scrollView bringSubviewToFront:self];
    }
    if (status == UIRefreshStatusReleasing) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self adjustInset:scrollView expand:TRUE];
            [self onStartAnimationWhenRealeasing];
        });
    } else if (status == UIRefreshStatusRefreshing) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self onStartAnimationWhenRefreshing];
        });
    } else if (status == UIRefreshStatusFinish) {
        CGFloat duration = [self onRefreshFinishing:self.isSuccess];
        if (duration > 0) {
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, duration * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
                [self adjustInset:scrollView expand:FALSE];
            });
        } else {
            dispatch_async(dispatch_get_main_queue(), ^{
                [self adjustInset:scrollView expand:FALSE];
            });
        }
    }
}

- (void)adjustFrameWithHeight:(CGFloat)expandHeight inset:(CGFloat)insetTop expand:(BOOL)isExpanded offset:(CGFloat)offset {
    const UISmartScrollMode mode = self.scrollMode;
    if (mode == UISmartScrollModeFront) {
        if (isExpanded) {
            self.frame = CGRectMake(0, offset + insetTop - expandHeight, self.width, self.height);
        } else {
            self.frame = CGRectMake(0, offset + insetTop, self.width, self.height);
        }
    } else if (mode == UISmartScrollModeStretch) {
        if (isExpanded) {
            CGFloat height = MAX(- offset - insetTop + expandHeight, 0);
            self.frame = CGRectMake(0, offset + insetTop - expandHeight, self.width, height);
        } else {
            CGFloat height = MAX(- offset - insetTop, 0);
            self.frame = CGRectMake(0, offset + insetTop, self.width, height);
        }
        if (!self.isDrawed && self.height > 0) {
            [self setNeedsDisplay];
        }
    }
}

/**
 * 调整 ScrollView 的 Inset，留出刷新动画显示的空间
 */
- (void)adjustInset:(UIScrollView*) scrollView expand:(BOOL) expand {
    CGFloat start = scrollView.contentOffset.y;
    if (self.scrollMode != UISmartScrollModeFront || self.expandsContentInset) {
        if (expand != self.isExpanded) {
            self.isExpanded = expand;
            CGFloat expandHeight = self.expandHeight;
            // SmartRefreshControl predates public inset helpers and used
            // private UIScrollView ivars. Keep the same behavior through the
            // public contentInset property so the component remains App Store
            // compatible on current iOS releases.
            UIEdgeInsets inset = scrollView.contentInset;
            inset.top += expand ? expandHeight : -expandHeight;
            scrollView.contentInset = inset;

            // A contentInset change does not always publish a matching
            // contentOffset KVO update. Refresh custom layouts immediately so
            // their frame uses the same expanded-inset state as the content.
            [self adjustFrameWithHeight:expandHeight
                                   inset:[self finalyContentInsetsFrom:scrollView]
                                 expand:self.isExpanded
                                 offset:scrollView.contentOffset.y];
        }
    }
    [self resetScrollContentOffset:scrollView start:start];
}

/**
 * ScrollView 滚动结束事件，用于松手回弹结束后，执行刷新动画
 */
- (void)scrollViewDidEndScrollingAnimation:(UIScrollView *)scrollView {
    if (self.status == UIRefreshStatusReleasing) {
        [self setStatus:UIRefreshStatusRefreshing];
        [self performRefreshEvent];
    } else if (self.status != UIRefreshStatusWillRefresh) {
        [self setStatus:UIRefreshStatusIdle];
    }
}

/**
 * 动画调整 ContentOffset 到 ContentInsets 对齐
 * 主要用于调整 ContentInsets 之后滚动列表
 */
- (void)resetScrollContentOffset:(UIScrollView*) scrollView start:(CGFloat) start {
    if (scrollView.isDragging) {
        //不调整滚动，直接通知滚动事件完成
        //用于适配某些 Header 下拉到一定程度自动回弹刷新功能
        [self scrollViewDidEndScrollingAnimation:scrollView];
    } else {
        //不在拖动状态的时候 才调整滚动，
        //调整结束之后，会触发 scrollViewDidEndScrollingAnimation 事件
        CGFloat end = -[self finalyContentInsetsFrom:scrollView];
        
        if (end != scrollView.contentOffset.y) {
            [UIView animateWithDuration:self.durationNormal animations:^{
                [scrollView setContentOffset:CGPointMake(0, end) animated:NO];
            } completion:^(__unused BOOL finished) {
                [self scrollViewDidEndScrollingAnimation:scrollView];
            }];
        } else {
            //UISmartScrollModeFront 模式会触发这里
            [self scrollViewDidEndScrollingAnimation:scrollView];
        }
    }

}

- (void)performRefreshEvent {
    RefreshBlock block = self.refreshBlock;
    if (block != nil) {
        block(self);
    } else if (self.target != nil) {
        [self sendActionsForControlEvents:UIControlEventValueChanged];
    } else {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
            [self finishRefreshWithSuccess:TRUE];
        });
    }
}

- (void)beginRefresh {
    if (!self.isRefreshing) {
        if (self.window != nil) {
            self.status = UIRefreshStatusReleasing;
        } else if (self.status != UIRefreshStatusRefreshing) {
            self.status = UIRefreshStatusWillRefresh;
        }
    }
}

- (void)finishRefreshWithSuccess:(BOOL)success {
    if (success) {
        [NSUserDefaults.standardUserDefaults setObject:[NSDate date] forKey:self.keyForLastRefreshTime];
        [NSUserDefaults.standardUserDefaults synchronize];
    }
    [self setIsSuccess:success];
    [self setStatus:UIRefreshStatusFinish];
    [self onRefreshFinished:success];
}

- (void)finishRefresh {
    [self finishRefreshWithSuccess:TRUE];
}

- (void)drawRect:(CGRect)rect {
//    NSLog(@"%@.life - drawRect", self);
    [super drawRect:rect];
    [self setIsDrawed:TRUE];
}
//- (void)didAddSubview:(UIView *)subview {
//    NSLog(@"%@.life - didAddSubview", self);
//    [super didAddSubview:subview];
//}
//- (void)willRemoveSubview:(UIView *)subview {
//    NSLog(@"%@.life - willRemoveSubview", self);
//    [super willRemoveSubview:subview];
//}
//- (void)willMoveToSuperview:(nullable UIView *)newSuperview {
//    NSLog(@"%@.life - willMoveToSuperview", self);
//    [super willMoveToSuperview:newSuperview];
//}
//- (void)didMoveToSuperview {
//    NSLog(@"%@.life - didMoveToSuperview", self);
//    [super didMoveToSuperview];
//}
- (void)willMoveToWindow:(nullable UIWindow *)newWindow {
//    NSLog(@"%@.life - willMoveToWindow - %@", self, newWindow);
    [super willMoveToWindow:newWindow];
    if (newWindow != nil) {
        if (self.status == UIRefreshStatusWillRefresh) {
            self.status = UIRefreshStatusReleasing;
        }
    }
}
//- (void)didMoveToWindow {
//    NSLog(@"%@.life - didMoveToWindow", self);
//    [super didMoveToWindow];
//}

- (NSString *)description
{
    return [NSString stringWithFormat:@"<%@: %p>", [self class], self];
}

#pragma mark - Event

- (void)onScrollingWithOffset:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL)isDragging {
    
}

- (void)onStatus:(UIRefreshStatus)old changed:(UIRefreshStatus)status {
    [self scrollView:self.scrollView didChange:old status:status];
}

- (void)onStartAnimationWhenRealeasing {
    
}

- (void)onStartAnimationWhenRefreshing {
    
}

- (void)onRefreshFinished:(BOOL)success {
    
}

- (CGFloat)onRefreshFinishing:(BOOL)success {
    return self.finishDuration;
}

@end
