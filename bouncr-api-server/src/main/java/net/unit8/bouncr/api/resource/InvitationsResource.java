package net.unit8.bouncr.api.resource;

import enkan.component.BeansConverter;
import enkan.security.bouncr.UserPermissionPrincipal;
import enkan.util.jpa.EntityTransactionManager;
import kotowari.restful.Decision;
import kotowari.restful.component.BeansValidator;
import kotowari.restful.data.Problem;
import kotowari.restful.data.RestContext;
import kotowari.restful.resource.AllowedMethods;
import net.unit8.bouncr.api.boundary.BouncrProblem;
import net.unit8.bouncr.api.boundary.InvitationCreateRequest;
import net.unit8.bouncr.component.BouncrConfiguration;
import net.unit8.bouncr.entity.Invitation;
import net.unit8.bouncr.util.RandomUtils;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.ConstraintViolation;

import java.util.Optional;
import java.util.Set;

import static enkan.util.BeanBuilder.builder;
import static kotowari.restful.DecisionPoint.*;

@AllowedMethods({"GET", "POST"})
public class InvitationsResource {
    @Inject
    private BouncrConfiguration config;

    @Inject
    private BeansConverter converter;

    @Inject
    private BeansValidator validator;

    @Decision(AUTHORIZED)
    public boolean isAuthorized(UserPermissionPrincipal principal) {
        return principal != null;
    }

    @Decision(value = ALLOWED, method= "GET")
    public boolean isGetAllowed(UserPermissionPrincipal principal) {
        return Optional.ofNullable(principal)
                .filter(p -> p.hasPermission("invitation:create"))
                .isPresent();
    }

    @Decision(value = MALFORMED, method = "POST")
    public Problem vaidateCreateRequest(InvitationCreateRequest createRequest, RestContext context) {
        if (createRequest == null) {
            return builder(Problem.valueOf(400, "request is empty"))
                    .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                    .build();
        }
        Set<ConstraintViolation<InvitationCreateRequest>> violations = validator.validate(createRequest);
        if (violations.isEmpty()) {
            context.putValue(createRequest);
        }
        return violations.isEmpty() ? null : builder(Problem.fromViolations(violations))
                .set(Problem::setType, BouncrProblem.MALFORMED.problemUri())
                .build();
    }

    @Decision(POST)
    public Invitation create(InvitationCreateRequest createRequest, EntityManager em) {
        String code = RandomUtils.generateRandomString(8, config.getSecureRandom());
        Invitation invitation = converter.createFrom(createRequest, Invitation.class);
        invitation.setCode(code);

        EntityTransactionManager tx = new EntityTransactionManager(em);
        tx.required(() -> em.persist(invitation));
        return invitation;
    }

}
