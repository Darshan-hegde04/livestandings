package com.phinixlabs.livestandings.config;

import com.phinixlabs.livestandings.common.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Write endpoints require {@code X-Organiser-Token} matching {@code ORGANISER_TOKEN}.
 */
@Component
public class OrganiserAuthInterceptor implements HandlerInterceptor {

    public static final String HEADER = "X-Organiser-Token";

    private final String organiserToken;

    public OrganiserAuthInterceptor(
            @Value("${livestandings.organiser-token:}")
            String organiserToken
    ) {
        this.organiserToken = organiserToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!StringUtils.hasText(organiserToken)) {
            throw new ForbiddenException("Organiser token is not configured");
        }
        String provided = request.getHeader(HEADER);
        if (!organiserToken.equals(provided)) {
            throw new ForbiddenException("Invalid or missing organiser token");
        }
        return true;
    }
}
