package dev.esgenius.service;

import dev.esgenius.config.AuthProperties;
import dev.esgenius.config.AuthenticatedUser;
import dev.esgenius.config.SupabaseAuthProperties;
import dev.esgenius.entity.Document;
import dev.esgenius.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentAccessPolicy {

    public static final String SHARED_DOCUMENT_MODIFY_MESSAGE =
            "Shared documents are view-only. Only the owner or an admin can delete or start an analysis.";

    private final AuthProperties authProperties;
    private final SupabaseAuthProperties supabaseAuthProperties;

    public DocumentAccessPolicy(AuthProperties authProperties, SupabaseAuthProperties supabaseAuthProperties) {
        this.authProperties = authProperties;
        this.supabaseAuthProperties = supabaseAuthProperties;
    }

    public Caller resolve(Optional<AuthenticatedUser> authenticatedUser) {
        if (authenticatedUser != null && authenticatedUser.isPresent()) {
            AuthenticatedUser user = authenticatedUser.get();
            return new Caller(user.userId(), authProperties.isAdmin(user.userId()));
        }
        return callerWhenIdentityAbsent();
    }

    /**
     * No authenticated identity. Treated as admin only when auth is disabled
     * ({@code AUTH_REQUIRED=false} and Supabase is not configured).
     */
    public Caller callerWhenIdentityAbsent() {
        if (!supabaseAuthProperties.isRequired() && !supabaseAuthProperties.isConfigured()) {
            return Caller.unidentifiedAdmin();
        }
        return Caller.anonymous();
    }

    public boolean isShared(Document document) {
        return document.getOwnerUserId() == null;
    }

    public boolean canView(Document document, Caller caller) {
        if (caller.admin()) {
            return true;
        }
        UUID ownerUserId = document.getOwnerUserId();
        return ownerUserId == null || ownerUserId.equals(caller.userId());
    }

    public boolean canModify(Document document, Caller caller) {
        if (caller.admin()) {
            return true;
        }
        return caller.userId() != null && caller.userId().equals(document.getOwnerUserId());
    }

    public void requireView(Document document, Caller caller, String notFoundMessage) {
        if (!canView(document, caller)) {
            throw new ResourceNotFoundException(notFoundMessage);
        }
    }

    public void requireModify(Document document, Caller caller, String notFoundMessage) {
        requireView(document, caller, notFoundMessage);
        if (!canModify(document, caller)) {
            throw new DocumentAccessDeniedException(SHARED_DOCUMENT_MODIFY_MESSAGE);
        }
    }
}
