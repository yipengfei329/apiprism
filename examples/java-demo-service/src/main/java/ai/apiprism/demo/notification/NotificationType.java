package ai.apiprism.demo.notification;

/**
 * 通知类型枚举。
 */
public enum NotificationType {

    /** 系统公告，如版本更新、维护通知 */
    SYSTEM,

    /** 订单状态变更通知 */
    ORDER,

    /** 营销活动推送，如优惠券发放、限时折扣 */
    PROMOTION,

    /** 互动消息，如评论回复、关注提醒 */
    SOCIAL
}
