package org.orcid.core.manager.v3.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;
import javax.transaction.Transactional;

import org.orcid.core.adapter.v3.JpaJaxbClientAdapter;
import org.orcid.core.constants.OrcidOauth2Constants;
import org.orcid.core.manager.AppIdGenerationManager;
import org.orcid.core.manager.EncryptionManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.core.manager.v3.ClientManager;
import org.orcid.core.manager.v3.SourceManager;
import org.orcid.core.manager.v3.read_only.ClientManagerReadOnly;
import org.orcid.jaxb.model.clientgroup.ClientType;
import org.orcid.jaxb.model.clientgroup.MemberType;
import org.orcid.jaxb.model.clientgroup.RedirectUriType;
import org.orcid.jaxb.model.v3.release.client.Client;
import org.orcid.jaxb.model.v3.release.client.ClientRedirectUri;
import org.orcid.persistence.dao.ClientDetailsDao;
import org.orcid.persistence.dao.ClientRedirectDao;
import org.orcid.persistence.dao.ClientSecretDao;
import org.orcid.persistence.dao.ProfileDao;
import org.orcid.persistence.dao.ProfileLastModifiedDao;
import org.orcid.persistence.jpa.entities.*;
import org.orcid.persistence.jpa.entities.keys.*;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class ClientManagerImpl implements ClientManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientManagerImpl.class);

    @Resource(name = "jpaJaxbClientAdapterV3")
    private JpaJaxbClientAdapter jpaJaxbClientAdapter;

    @Resource
    private ClientDetailsDao clientDetailsDao;

    @Resource
    private ClientSecretDao clientSecretDao;

    @Resource
    private ClientRedirectDao clientRedirectDao;

    @Resource
    private EncryptionManager encryptionManager;

    @Resource
    private AppIdGenerationManager appIdGenerationManager;

    @Resource(name = "sourceManagerV3")
    private SourceManager sourceManager;

    @Resource
    private ProfileEntityCacheManager profileEntityCacheManager;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource(name = "clientManagerReadOnlyV3")
    private ClientManagerReadOnly clientManagerReadOnly;

    @Resource
    private ProfileDao profileDao;
    
    @Resource
    private ProfileLastModifiedDao profileLastModifiedDao;

    @Override
    @Transactional
    public Client create(Client newClient) throws IllegalArgumentException {
        return create(newClient, false, false);
    }

    @Override
    @Transactional
    public Client createPublicClient(Client newClient) {
        return create(newClient, true, false);
    }

    @Override
    @Transactional
    public Client createWithConfigValues(Client newClient) {
        return create(newClient, false, true);
    }

    private Client create(Client newClient, boolean publicClient, boolean addConfigValues) {
        // If the member id comes in the newClient, use that one, if not, use the active source id
        String memberId = PojoUtil.isEmpty(newClient.getGroupProfileId()) ? null : newClient.getGroupProfileId();
        if(memberId == null) {
            memberId = sourceManager.retrieveActiveSourceId();
        }
        ProfileEntity memberEntity = profileEntityCacheManager.retrieve(memberId);

        if(memberEntity == null) {
            LOGGER.error("Unable to find member with id {}", memberId);
            throw new IllegalArgumentException("Unable to find member with id " + memberId);
        }

        // Verify if the member type allow him to create another client
        if (publicClient) {
            ClientDetailsEntity existingPublicClient = clientDetailsDao.getPublicClient(memberId);
            if (existingPublicClient != null) {
                return jpaJaxbClientAdapter.toClient(existingPublicClient);
            }
        } else {
            validateCreateClientRequest(memberId);
        }

        ClientDetailsEntity newEntity = jpaJaxbClientAdapter.toEntity(newClient);
        newEntity.setId(appIdGenerationManager.createNewAppId());
        newEntity.setClientSecretForJpa(encryptionManager.encryptForInternalUse(UUID.randomUUID().toString()), true);
        newEntity.setGroupProfileId(memberId);

        // Set persistent tokens enabled by default
        newEntity.setPersistentTokensEnabled(true);

        // Set authentication provider id
        newEntity.setAuthenticationProviderId(newClient.getAuthenticationProviderId());

        // Set ClientType
        if (!publicClient) {
            newEntity.setClientType(getClientType(MemberType.valueOf(memberEntity.getGroupType())).name());
        } else {
            newEntity.setClientType(ClientType.PUBLIC_CLIENT.name());
        }

        // Set ClientResourceIdEntity
        Set<ClientResourceIdEntity> clientResourceIdEntities = new HashSet<ClientResourceIdEntity>();
        ClientResourceIdEntity clientResourceIdEntity = new ClientResourceIdEntity();
        clientResourceIdEntity.setClientId(newEntity.getClientId());
        clientResourceIdEntity.setResourceId("orcid");
        clientResourceIdEntities.add(clientResourceIdEntity);
        newEntity.setClientResourceIds(clientResourceIdEntities);

        for(ClientRedirectUriEntity rUri : newEntity.getClientRegisteredRedirectUris()) {
            rUri.setClientId(newEntity.getClientId());
        }

        // Set ClientAuthorisedGrantTypeEntity
        Set<ClientAuthorisedGrantTypeEntity> clientAuthorisedGrantTypeEntities = new HashSet<ClientAuthorisedGrantTypeEntity>();
        for (String clientAuthorisedGrantType : Arrays.asList("client_credentials", "authorization_code", "refresh_token", "implicit")) {
            ClientAuthorisedGrantTypeEntity grantTypeEntity = new ClientAuthorisedGrantTypeEntity();
            grantTypeEntity.setClientId(newEntity.getClientId());
            grantTypeEntity.setGrantType(clientAuthorisedGrantType);
            clientAuthorisedGrantTypeEntities.add(grantTypeEntity);
        }
        newEntity.setClientAuthorizedGrantTypes(clientAuthorisedGrantTypeEntities);

        // Set ClientGrantedAuthorityEntity
        List<ClientGrantedAuthorityEntity> clientGrantedAuthorityEntities = new ArrayList<ClientGrantedAuthorityEntity>();
        ClientGrantedAuthorityEntity clientGrantedAuthorityEntity = new ClientGrantedAuthorityEntity();
        clientGrantedAuthorityEntity.setClientId(newEntity.getClientId());
        if (publicClient) {
            clientGrantedAuthorityEntity.setAuthority("ROLE_PUBLIC");
        } else {
            clientGrantedAuthorityEntity.setAuthority("ROLE_CLIENT");
        }
        clientGrantedAuthorityEntities.add(clientGrantedAuthorityEntity);
        newEntity.setClientGrantedAuthorities(clientGrantedAuthorityEntities);

        // Set ClientScopeEntity
        Set<ClientScopeEntity> clientScopeEntities = new HashSet<ClientScopeEntity>();
        for (String clientScope : ClientType.getScopes(ClientType.valueOf(newEntity.getClientType()))) {
            ClientScopeEntity clientScopeEntity = new ClientScopeEntity();
            clientScopeEntity.setClientId(newEntity.getClientId());
            clientScopeEntity.setScopeType(clientScope);
            clientScopeEntities.add(clientScopeEntity);
        }
        newEntity.setClientScopes(clientScopeEntities);

        if (addConfigValues) {
            newEntity.setUserOBOEnabled(newClient.isUserOBOEnabled());
            refreshGrantTypesForObo(newEntity, newClient.isOboEnabled());
        }

        try {
            clientDetailsDao.persist(newEntity);
        } catch (Exception e) {
            LOGGER.error("Unable to create client with id {}", newEntity.getId(), e);
            throw e;
        }

        return jpaJaxbClientAdapter.toClient(newEntity);
    }

    @Override
    @Transactional
    public Client edit(Client existingClient, boolean updateConfigValues) {
        if (!clientDetailsDao.exists(existingClient.getId())) {
            throw new IllegalArgumentException("Invalid client id provided: " + existingClient.getId());
        }
        ClientDetailsEntity clientDetails = clientDetailsDao.find(existingClient.getId());

        if (ClientType.PUBLIC_CLIENT.name().equals(clientDetails.getClientType())) {
            for (ClientRedirectUri rUri : existingClient.getClientRedirectUris()) {
                rUri.setRedirectUriType(RedirectUriType.SSO_AUTHENTICATION.value());
                rUri.setUriActType(null);
                rUri.setUriGeoArea(null);
            }
        }

        jpaJaxbClientAdapter.toEntity(existingClient, clientDetails);        
        clientDetails.manuallyUpdateLastModified();
        
        // Check if we should update client configuration values
        if (updateConfigValues) {
            // Authentication provider id
            clientDetails.setAuthenticationProviderId(existingClient.getAuthenticationProviderId());
            // Enable persistent tokens
            clientDetails.setPersistentTokensEnabled(existingClient.isPersistentTokensEnabled());
            clientDetails.setUserOBOEnabled(existingClient.isUserOBOEnabled());
            refreshGrantTypesForObo(clientDetails, existingClient.isOboEnabled());
        }

        clientDetails = clientDetailsDao.merge(clientDetails);
        return jpaJaxbClientAdapter.toClient(clientDetails);
    }

    @Override
    public Boolean resetClientSecret(String clientId) {
        return transactionTemplate.execute(new TransactionCallback<Boolean>() {
            @SuppressWarnings("deprecation")
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                try {
                    // Generate new secret
                    String clientSecret = encryptionManager.encryptForInternalUse(UUID.randomUUID().toString());
                    // Set all existing secrets as non primary
                    clientSecretDao.revokeAllKeys(clientId);
                    // #2 Create the new client secret as primary
                    boolean result = clientSecretDao.createClientSecret(clientId, clientSecret);
                    // #3 if it was created, update the last modified for the
                    // client and for the member as well
                    if (result) {
                        clientDetailsDao.updateLastModified(clientId);
                        profileLastModifiedDao.updateLastModifiedDateWithoutResult(sourceManager.retrieveActiveSourceId());
                    }
                    return result;
                } catch (Exception e) {
                    LOGGER.error("Unable to reset client secret for client {}", clientId, e);
                    throw e;
                }
            }
        });
    }

    @Override
    @Transactional
    public String resetAndGetClientSecret(String clientId) {
        try {
            String newSecret = encryptionManager.encryptForInternalUse(UUID.randomUUID().toString());
            clientSecretDao.revokeAllKeys(clientId);
            boolean created  = clientSecretDao.createClientSecret(clientId, newSecret);
            if (created) {
                clientDetailsDao.updateLastModified(clientId);
                String sourceId = sourceManager.retrieveActiveSourceId();
                profileLastModifiedDao.updateLastModifiedDateWithoutResult(sourceId);
            } else {
                LOGGER.warn("Client secret creation failed for client {}", clientId);
            }

            return encryptionManager.decryptForInternalUse(newSecret);
        } catch (Exception e) {
            LOGGER.error("Unable to reset client secret for client {}", clientId, e);
            throw e;
        }
    }

    private void refreshGrantTypesForObo(ClientDetailsEntity clientDetails, boolean enableObo) {
        boolean oboAlreadyEnabled = false;
        Iterator<ClientAuthorisedGrantTypeEntity> grantTypes = clientDetails.getClientAuthorizedGrantTypes().iterator();
        while (grantTypes.hasNext()) {
            ClientAuthorisedGrantTypeEntity g = grantTypes.next();
            if (g != null && OrcidOauth2Constants.IETF_EXCHANGE_GRANT_TYPE.equals(g.getGrantType())) {
                oboAlreadyEnabled = true;
                if (!enableObo) {
                    grantTypes.remove();                    
                }
                break;
            }
        }
        
        if (!oboAlreadyEnabled && enableObo) {
            ClientAuthorisedGrantTypeEntity obo = new ClientAuthorisedGrantTypeEntity();
            obo.setClientId(clientDetails.getClientId());
            obo.setGrantType(OrcidOauth2Constants.IETF_EXCHANGE_GRANT_TYPE);
            clientDetails.getClientAuthorizedGrantTypes().add(obo);
        }
    }
    
    private void validateCreateClientRequest(String memberId) throws IllegalArgumentException {
        ProfileEntity member = profileEntityCacheManager.retrieve(memberId);
        if (member == null || member.getGroupType() == null) {
            throw new IllegalArgumentException("Illegal member id: " + memberId);
        }
        if (MemberType.BASIC_INSTITUTION.name().equals(member.getGroupType())) {
            Set<Client> clients = clientManagerReadOnly.getClients(memberId);
            if (clients != null && !clients.isEmpty()) {
                throw new IllegalArgumentException("Your membership doesn't allow you to have more clients");
            }
        }
    }

    private ClientType getClientType(MemberType memberType) {
        if (memberType == null) {
            return ClientType.PUBLIC_CLIENT;
        }

        switch (memberType) {
        case BASIC:
            return ClientType.UPDATER;
        case BASIC_INSTITUTION:
            return ClientType.CREATOR;
        case PREMIUM:
            return ClientType.PREMIUM_UPDATER;
        case PREMIUM_INSTITUTION:
            return ClientType.PREMIUM_CREATOR;
        default:
            throw new IllegalArgumentException("Invalid member type: " + memberType);
        }
    }
}
