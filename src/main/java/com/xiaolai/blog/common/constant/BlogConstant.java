package com.xiaolai.blog.common.constant;

public class BlogConstant {
    public static final int ARTICLE_STATUS_DRAFT = 0;
    public static final int ARTICLE_STATUS_PUBLISHED = 1;

    public static final int COMMENT_STATUS_PENDING = 0;
    public static final int COMMENT_STATUS_APPROVED = 1;
    public static final int COMMENT_STATUS_REJECTED = 2;
    public static final int COMMENT_STATUS_SPAM = 3;

    public static final int MAX_TOP_ARTICLES = 3;
    public static final int LOGIN_ATTEMPT_LIMIT = 5;
    public static final long LOGIN_LOCK_MINUTES = 30;
}