package com.remo.realestatemaintainceoptimizer.service;

import com.remo.realestatemaintainceoptimizer.dto.TenantMatch;
import com.remo.realestatemaintainceoptimizer.entity.Tenant;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Assigns the payer name of a bank statement to one of the given tenants by comparing normalized name parts, so a wrong guess of the AI can never reach the books: an ambiguous name stays unassigned.
 */
@Component
public class TenantNameMatcher {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_LETTERS = Pattern.compile("[^a-z]+");

    /**
     * The tenant a payer name was assigned to, or none, together with the reliability of that assignment.
     */
    public record Assignment(Tenant tenant, TenantMatch match) {

        static Assignment none() {
            return new Assignment(null, TenantMatch.NONE);
        }
    }

    /**
     * Returns the tenant whose last and first name both occur in the payer name, or else the one whose last name occurs alone; several candidates in different apartments leave the payment unassigned, while several in one apartment pick the first since their rent is shared.
     */
    public Assignment assign(String payerName, List<Tenant> tenants) {
        Set<String> payerTokens = tokens(payerName);
        List<Tenant> fullMatches = tenants.stream().filter(tenant -> matchesFullName(payerTokens, tenant)).toList();
        if (!fullMatches.isEmpty()) {
            return pickWithinOneApartment(fullMatches, TenantMatch.MATCH);
        }
        List<Tenant> lastNameMatches = tenants.stream().filter(tenant -> containsAll(payerTokens, tokens(tenant.lastName()))).toList();
        return pickWithinOneApartment(lastNameMatches, TenantMatch.POSSIBLE);
    }

    private static Assignment pickWithinOneApartment(List<Tenant> candidates, TenantMatch match) {
        if (candidates.isEmpty()) {
            return Assignment.none();
        }
        String apartmentId = candidates.getFirst().apartment().id();
        boolean sameApartment = candidates.stream().allMatch(candidate -> candidate.apartment().id().equals(apartmentId));
        return sameApartment ? new Assignment(candidates.getFirst(), match) : Assignment.none();
    }

    private static boolean matchesFullName(Set<String> payerTokens, Tenant tenant) {
        Set<String> lastNameTokens = tokens(tenant.lastName());
        Set<String> firstNameTokens = tokens(tenant.firstName());
        return !lastNameTokens.isEmpty()
                && containsAll(payerTokens, lastNameTokens)
                && (containsAll(payerTokens, firstNameTokens) || hasInitial(payerTokens, firstNameTokens));
    }

    private static boolean hasInitial(Set<String> payerTokens, Set<String> firstNameTokens) {
        return firstNameTokens.stream().anyMatch(firstName -> payerTokens.contains(firstName.substring(0, 1)));
    }

    private static boolean containsAll(Set<String> payerTokens, Set<String> nameTokens) {
        return !nameTokens.isEmpty() && payerTokens.containsAll(nameTokens);
    }

    private static Set<String> tokens(String name) {
        if (name == null) {
            return Set.of();
        }
        String folded = DIACRITICS.matcher(Normalizer.normalize(name.toLowerCase(Locale.ROOT).replace("ß", "ss"), Normalizer.Form.NFD))
                .replaceAll("");
        return new HashSet<>(Arrays.stream(NON_LETTERS.split(folded)).filter(token -> !token.isEmpty()).toList());
    }
}
