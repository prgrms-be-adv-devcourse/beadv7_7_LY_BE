package site.coreservice.global.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 헤더 없으면 BusinessException(GlobalErrorCode.MEMBER_ID_HEADER_REQUIRED)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface MemberId {

    String HEADER_NAME = "Member-Id";
}
