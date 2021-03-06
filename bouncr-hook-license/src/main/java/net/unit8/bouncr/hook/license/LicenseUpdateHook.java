package net.unit8.bouncr.hook.license;

import enkan.collection.Headers;
import enkan.data.Cookie;
import enkan.data.jpa.EntityManageable;
import kotowari.restful.data.RestContext;
import net.unit8.bouncr.entity.User;
import net.unit8.bouncr.hook.Hook;
import net.unit8.bouncr.hook.license.entity.LicenseLastActivity;
import net.unit8.bouncr.hook.license.entity.UserLicense;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Optional;

import static enkan.util.BeanBuilder.builder;

public class LicenseUpdateHook implements Hook<RestContext> {
    private final LicenseConfig config;

    public LicenseUpdateHook(LicenseConfig config) {
        this.config = config;
    }

    @Override
    public void run(RestContext context) {
        final EntityManager em = ((EntityManageable) context.getRequest()).getEntityManager();
        context.getValue(User.class).ifPresent(user -> {
            UserLicense userLicense = context.getValue(UserLicense.class).orElseGet(() -> {
                LicenseKey newLicenseKey = LicenseKey.createNew();

                UserLicense userLic = builder(new UserLicense())
                        .set(UserLicense::setUser, user)
                        .set(UserLicense::setLicenseKey, newLicenseKey.asBytes())
                        .build();

                LicenseLastActivity lastActivity = builder(new LicenseLastActivity())
                        .set(LicenseLastActivity::setUserLicense, userLic)
                        .set(LicenseLastActivity::setUserAgent, Optional.ofNullable(context.getRequest().getHeaders())
                                .map(headers -> headers.get("User-Agent"))
                                .map(ua -> ua.substring(0, Math.min(ua.length()-1, 255)))
                                .orElse(null))
                        .set(LicenseLastActivity::setLastUsedAt, LocalDateTime.now())
                        .build();
                em.persist(userLic);
                em.persist(lastActivity);

                Cookie cookie = Cookie.create(config.getCookieName(), newLicenseKey.asString());
                ZoneId zone = ZoneId.systemDefault();
                Date expires = Date.from(
                        ZonedDateTime.of(LocalDate.now()
                                .plusYears(10)
                                .atTime(0, 0), zone)
                                .toInstant());
                cookie.setExpires(expires);
                cookie.setPath("/");
                context.setHeaders(Headers.of("Set-Cookie", cookie.toHttpString()));

                return userLic;
            });
        });
    }
}
