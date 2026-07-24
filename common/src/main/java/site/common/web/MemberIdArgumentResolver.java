package site.common.web;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import site.common.exception.BusinessException;

import static site.common.exception.GlobalErrorCode.MEMBER_ID_HEADER_REQUIRED;

public class MemberIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(final MethodParameter parameter) {
        return parameter.hasParameterAnnotation(MemberId.class);
    }

    @Override
    public Object resolveArgument(
        final MethodParameter parameter,
        final ModelAndViewContainer mavContainer,
        final NativeWebRequest webRequest,
        final WebDataBinderFactory binderFactory
    ) {
        final String value = webRequest.getHeader(MemberId.HEADER_NAME);
        if (value == null || value.isBlank()) {
            throw new BusinessException(MEMBER_ID_HEADER_REQUIRED);
        }
        return Long.valueOf(value);
    }
}
