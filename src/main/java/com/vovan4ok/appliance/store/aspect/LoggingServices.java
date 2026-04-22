package com.vovan4ok.appliance.store.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingServices {

    private static final Logger log = LoggerFactory.getLogger(LoggingServices.class);

    @Around("@annotation(com.vovan4ok.appliance.store.aspect.Loggable)")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        log.info(">> {}", method);
        try {
            Object result = joinPoint.proceed();
            log.info("<< {} returned: {}", method, result);
            return result;
        } catch (Exception ex) {
            log.error("!! {} threw: {}", method, ex.getMessage());
            throw ex;
        }
    }
}
