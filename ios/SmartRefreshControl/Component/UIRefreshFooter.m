//
//  UIRefreshFooter.m
//  Refresh
//
//  Created by SCWANG on 2021/7/1.
//  Copyright © 2021 Teeyun. All rights reserved.
//

#import "UIRefreshFooter.h"

@interface UIRefreshComponent (UIRefreshFooter)

@property (nonatomic, assign) BOOL isExpanded;
@property (nonatomic, assign) CGFloat dragOffset;

@end

@interface UIRefreshFooter ()

@property (nonatomic, assign) BOOL isDrawed;
@property (nonatomic, assign) BOOL isSuccess;
@property (nonatomic, assign) CGSize frameSize;
@property (nonatomic, assign) CGSize contentSize;
@property (nonatomic, assign) UISmartFooterStatus status;

- (void)updateFrameForScrollView:(UIScrollView *)scrollView;
- (void)updateFrameForScrollView:(UIScrollView *)scrollView contentSize:(CGSize)contentSize;

@end

@implementation UIRefreshFooter

#pragma mark - Init

/**
 * 初始化参数
 */
- (void)setUpComponent {
    [super setUpComponent];
    [self setDragOffset:0];//父类是-1，初始化是 offset=0，0！= -1，会导致计算拖动
    //self.finishDuration = 0;
    //self.status = UISmartFooterStatusIdle;
//    self.triggerRate = 1;
    
}

- (void)setIsAutoLoadMore:(BOOL)isAutoLoadMore {
    if (self.isAttached) {
        id name = @"调用错误";
        id reason = @"setIsAutoLoadMore 必须在setUpComponent中设置";
        @throw [NSException exceptionWithName:name reason:reason userInfo:nil];
    } else {
        _isAutoLoadMore = isAutoLoadMore;
        if (isAutoLoadMore) {
            //如果是自动加载更多的话，需要添加点击加载功能
            self.userInteractionEnabled = TRUE;
            [self addTarget:self action:@selector(beginLoadMore) forControlEvents:UIControlEventTouchUpInside];
        } else {
            [self removeTarget:self action:@selector(beginLoadMore) forControlEvents:UIControlEventTouchUpInside];
            self.userInteractionEnabled = FALSE;
        }
    }
}

- (void)layoutSubviews {
    [super layoutSubviews];
    UIScrollView *scrollView = self.scrollView;
    if (scrollView != nil) {
        [self updateFrameForScrollView:scrollView];
    }
}

#pragma mark - Getter

- (BOOL)isLoading {
    return [self inStatus:UISmartFooterStatusWillLoadMore,UISmartFooterStatusReleasing,UISmartFooterStatusLoading,0];
}

- (BOOL)isNoMoreData {
    return self.status == UISmartFooterStatusNoMoreData;
}

- (BOOL)inStatus:(UISmartFooterStatus)status, ... {
    va_list args;
    va_start(args, status);
    UISmartFooterStatus arg = status;
    do {
        if (self.status == arg) {
            return true;
        }
        arg = va_arg(args, UISmartFooterStatus);
    } while (arg != 0);
    va_end(args);
    return false;
}

- (void)setStatus:(UISmartFooterStatus)status {
    UISmartFooterStatus old = _status;

    _status = status;
    
    BOOL changed = old != status;
    UIScrollView* scrollView = self.scrollView;
    if (changed && scrollView != nil) {
        [self onStatus:old changed:status];
    }
}

#pragma mark - Method

- (void)scrollView:(UIScrollView *)scrollView attached:(BOOL)attach {
    [super scrollView:scrollView attached:attach];
    // The footer lives after the scroll content in coordinate space. Keep it
    // above the FlatList content so its loading label/spinner is visible when
    // the user pulls past the last row.
    [scrollView bringSubviewToFront:self];
    // Fabric can attach the footer after a FlatList has already measured its
    // content. KVO only reports subsequent changes, so seed the geometry from
    // the current values as part of attachment as well.
    [self updateFrameForScrollView:scrollView];
    [self scrollView:scrollView didChange:CGPointMake(scrollView.contentOffset.x, scrollView.contentOffset.y)
       contentOffset:scrollView.contentOffset];
}

- (void)addObservers:(UIScrollView *)scrollView {
    NSUInteger opt = NSKeyValueObservingOptionNew|NSKeyValueObservingOptionOld;
    [scrollView addObserver:self forKeyPath:@"contentSize" options:opt context:nil];
    [super addObservers:scrollView];
    if (self.isAutoLoadMore) {
        [self adjustInset:scrollView expand:TRUE];
    }
}

- (void)removeObservers:(UIScrollView *)scrollView {
    [scrollView removeObserver:self forKeyPath:@"contentSize"];
    [super removeObservers:scrollView];
    if (self.isExpanded) {
        [self adjustInset:scrollView expand:FALSE];
    }
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary<NSKeyValueChangeKey,id> *)change context:(void *)context {
    
    UIScrollView* scrollView = self.scrollView;
    if (scrollView == nil || change == nil) {
        return;
    }
    
    [super observeValueForKeyPath:keyPath ofObject:object change:change context:context];
    
    if ([@"contentSize" isEqualToString:keyPath]) {
        CGSize newValue = [[change valueForKey:NSKeyValueChangeNewKey] CGSizeValue];
        CGSize oldValue = [[change valueForKey:NSKeyValueChangeOldKey] CGSizeValue];
        
        [self scrollView:scrollView didChange:oldValue contentSize:newValue];
    }
}

- (void)scrollView:(UIScrollView *)scrollView didChange:(CGSize)old contentSize:(CGSize)value {
    (void)old;
    [self updateFrameForScrollView:scrollView contentSize:value];
}

- (void)updateFrameForScrollView:(UIScrollView *)scrollView {
    [self updateFrameForScrollView:scrollView contentSize:scrollView.contentSize];
}

- (void)updateFrameForScrollView:(UIScrollView *)scrollView
                      contentSize:(CGSize)contentSize {
    if (scrollView == nil) {
        return;
    }

    CGSize boundsSize = scrollView.bounds.size;
    UIEdgeInsets inset = scrollView.adjustedContentInset;
    // A short list still needs a usable footer location at the bottom of the
    // viewport. Long lists keep the footer directly after their content.
    CGFloat minimumContentHeight = MAX(0, boundsSize.height - inset.top - inset.bottom);
    CGFloat originY = MAX(contentSize.height, minimumContentHeight);
    CGFloat height = MAX(self.height, 1);
    CGRect frame = self.frame;
    frame.origin.x = 0;
    frame.origin.y = originY;
    frame.size.width = boundsSize.width;
    frame.size.height = height;

    [self setContentSize:contentSize];
    [self setFrameSize:boundsSize];
    if (!CGRectEqualToRect(self.frame, frame)) {
        [self setFrame:frame];
    }
}

- (void)scrollView:(UIScrollView *)scrollView didScroll:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL) isDragging {
    
    const UISmartFooterStatus status = self.status;
    
    if ([self inStatus:UISmartFooterStatusIdle,UISmartFooterStatusPullToLoadMore,UISmartFooterStatusReleaseToLoadMore,0]) {
        if (self.isNoMoreData) {}
        else if (self.isAutoLoadMore) {
            CGSize frameSize = scrollView.frame.size;
            CGSize contentSize = scrollView.contentSize;
            CGPoint contentOffset = scrollView.contentOffset;
            UIEdgeInsets contentInset = scrollView.contentInset;
            if (frameSize.height < contentSize.height + contentInset.top) {
                if (contentOffset.y >= contentSize.height - frameSize.height + (self.triggerRate-1)*self.expandHeight + contentInset.bottom) {
                    [self beginLoadMore];
                }
            }
        } else if (isDragging) {
            if (status != UISmartFooterStatusReleaseToLoadMore && percent >= 1) {
                self.status = UISmartFooterStatusReleaseToLoadMore;
            } else if (status != UISmartFooterStatusPullToLoadMore && percent < 1) {
                self.status = UISmartFooterStatusPullToLoadMore;
            }
        } else if (status == UISmartFooterStatusReleaseToLoadMore) {
            [self beginLoadMore];
        } else if (status != UISmartFooterStatusFinish && status != UISmartFooterStatusReleasing) {
            self.status = UISmartFooterStatusIdle;
        }
    }

    [self onScrollingWithOffset:offset percent:percent drag:isDragging];
}

- (void)scrollView:(UIScrollView *)scrollView didChange:(UISmartFooterStatus)old status:(UISmartFooterStatus)status {
    if (status != UISmartFooterStatusIdle) {
        [scrollView bringSubviewToFront:self];
    }
    if (old == UISmartFooterStatusIdle && self.scrollMode == UISmartScrollModeFront) {
        [scrollView bringSubviewToFront:self];
    }
//    if (self.isAutoLoadMore) {
//        return;
//    }
    if (status == UISmartFooterStatusReleasing) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self adjustInset:scrollView expand:TRUE];
            [self onStartAnimationWhenRealeasing];
        });
    } else if (status == UISmartFooterStatusLoading) {
        dispatch_async(dispatch_get_main_queue(), ^{
            [self onStartAnimationWhenLoading];
        });
        if (self.isAutoLoadMore) {
            [self performLoadMoreEvent];
        }
    } else if (status == UISmartFooterStatusFinish) {
        CGFloat duration = [self onLoadMoreFinishing:self.isSuccess];
        if (self.isAutoLoadMore) {} //自动加载更多的话，不需要关闭
        else if (duration > 0) {
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
    if (self.isAutoLoadMore || self.isNoMoreData) {
        return;
    } else if (mode == UISmartScrollModeFront) {
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
    if (self.scrollMode != UISmartScrollModeFront) {
        if (expand != self.isExpanded) {
            self.isExpanded = expand;
            CGFloat expandHeight = self.expandHeight;
            // Use the public inset API. The upstream project mutated a
            // private UIKit backing ivar, which is unsafe with modern UIKit.
            UIEdgeInsets inset = scrollView.contentInset;
            inset.bottom += expand ? expandHeight : -expandHeight;
            scrollView.contentInset = inset;
        }
    }
    if (!self.isAutoLoadMore) {
        [self resetScrollContentOffset:scrollView start:start];
    }
}

/**
 * ScrollView 滚动结束事件，用于松手回弹结束后，执行刷新动画
 */
- (void)scrollViewDidEndScrollingAnimation:(UIScrollView *)scrollView {
    if (self.status == UISmartFooterStatusReleasing) {
        [self setStatus:UISmartFooterStatusLoading];
        [self performLoadMoreEvent];
    } else if (self.status != UISmartFooterStatusWillLoadMore) {
        [self setStatus:UISmartFooterStatusIdle];
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
        UIEdgeInsets inset = scrollView.adjustedContentInset;
        CGFloat minimumOffset = -inset.top;
        CGFloat maximumOffset = scrollView.contentSize.height -
            CGRectGetHeight(scrollView.bounds) + inset.bottom;
        CGFloat end = MAX(minimumOffset, maximumOffset);
        
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

- (void)performLoadMoreEvent {
    UISmartFooterBlock block = self.loadMoreBlock;
    if (block != nil) {
        block(self);
    } else if (self.target != nil) {
        [self sendActionsForControlEvents:UIControlEventValueChanged];
    } else {
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2 * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
            [self finishLoadMoreWithSuccess:TRUE];
        });
    }
}

- (void)beginLoadMore {
    if (!self.isLoading && !self.isNoMoreData) {
        if (self.isAutoLoadMore) {
            self.status = UISmartFooterStatusLoading;
        } else if (self.window != nil) {
            self.status = UISmartFooterStatusReleasing;
        } else if (self.status != UISmartFooterStatusLoading) {
            self.status = UISmartFooterStatusWillLoadMore;
        }
    }
}

- (void)finishLoadMoreWithSuccess:(BOOL)success {
    if (self.isLoading) {
        [self setIsSuccess:success];
        [self setStatus:UISmartFooterStatusFinish];
        [self onLoadMoreFinished:success];
        if (self.isAutoLoadMore) {
            [self setStatus:UISmartFooterStatusIdle];
        }
    }
}

- (void)finishLoadMore {
    [self finishLoadMoreWithSuccess:TRUE];
}

- (void)finishLoadMoreWithNoMoreData {
    if (self.isLoading) {
        [self finishLoadMoreWithSuccess:TRUE];
        [self setStatus:UISmartFooterStatusNoMoreData];
    } else if (!self.isNoMoreData) {
        // Allow a controlled RN prop/command to mark an idle footer without
        // first synthesizing a load request.
        [self setStatus:UISmartFooterStatusNoMoreData];
    }
}

- (void)resetNoMoreData {
    if (self.status == UISmartFooterStatusNoMoreData) {
        self.status = UISmartFooterStatusIdle;
    }
}

- (void)drawRect:(CGRect)rect {
    [super drawRect:rect];
    [self setIsDrawed:TRUE];
}

- (void)willMoveToWindow:(nullable UIWindow *)newWindow {
    [super willMoveToWindow:newWindow];
    if (newWindow != nil) {
        if (self.status == UISmartFooterStatusWillLoadMore) {
            self.status = UISmartFooterStatusReleasing;
        }
    }
}

- (NSString *)description
{
    return [NSString stringWithFormat:@"<%@: %p>", [self class], self];
}

- (CGFloat)finalyContentInsetsFrom:(UIScrollView*) scrollView {
    if (@available(iOS 11.0, *)) {
        return scrollView.adjustedContentInset.bottom;
    } else {
        return scrollView.contentInset.bottom;
    }
}

- (CGFloat)finalyDragOffset: (CGPoint) contentOffset inset: (CGFloat) inset height: (CGFloat) expandHeight expand: (BOOL) isExpanded {
    CGFloat offset = contentOffset.y -
        (self.contentSize.height - self.frameSize.height + inset);
    return offset + (isExpanded ? expandHeight : 0);
}

#pragma mark - Event

- (void)onScrollingWithOffset:(CGFloat)offset percent:(CGFloat)percent drag:(BOOL)isDragging {
    if (self.frame.origin.y > 0 && self.isAutoLoadMore) {
        [self beginLoadMore];
    }
}

- (void)onStatus:(UISmartFooterStatus)old changed:(UISmartFooterStatus)status {
    [self scrollView:self.scrollView didChange:old status:status];
}

- (void)onStartAnimationWhenRealeasing {
    
}

- (void)onStartAnimationWhenLoading {
    
}

- (void)onLoadMoreFinished:(BOOL)success {
    
}

- (CGFloat)onLoadMoreFinishing:(BOOL)success {
    return self.finishDuration;
}

@end
