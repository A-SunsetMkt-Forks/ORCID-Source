package org.orcid.core.manager.v3;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.orcid.core.common.manager.EmailDomainManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.core.manager.v3.impl.ProfileEmailDomainManagerImpl;
import org.orcid.jaxb.model.v3.release.common.Visibility;
import org.orcid.persistence.dao.EmailDomainDao;
import org.orcid.persistence.dao.ProfileEmailDomainDao;
import org.orcid.persistence.jpa.entities.EmailDomainEntity;
import org.orcid.persistence.jpa.entities.EmailDomainEntity.DomainCategory;
import org.orcid.persistence.jpa.entities.ProfileEmailDomainEntity;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.pojo.EmailDomain;
import org.orcid.pojo.ajaxForm.ProfileEmailDomain;
import org.orcid.test.TargetProxyHelper;

public class ProfileEmailDomainManagerTest {
    @Mock
    private ProfileEmailDomainDao profileEmailDomainDaoMock;

    @Mock
    private ProfileEmailDomainDao profileEmailDomainDaoReadOnlyMock;

    @Mock
    private ProfileEntityCacheManager profileEntityCacheManagerMock;

    @Mock
    private EmailDomainDao emailDomainDaoMock;
    
    @Mock
    private EmailDomainManager emailDomainManagerMock;

    ProfileEmailDomainManager pedm = new ProfileEmailDomainManagerImpl();


    private static final String ORCID = "0000-0000-0000-0001";
    private static final String ORCID_TWO = "0000-0000-0000-0002";
    private static final String EMAIL_DOMAIN = "orcid.org";
    private static final String EMAIL_DOMAIN_TWO = "email.com";
    private static final String EMAIL_DOMAIN_THREE = "domain.net";


    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        TargetProxyHelper.injectIntoProxy(pedm, "profileEmailDomainDao", profileEmailDomainDaoMock);
        TargetProxyHelper.injectIntoProxy(pedm, "profileEmailDomainDaoReadOnly", profileEmailDomainDaoReadOnlyMock);
        TargetProxyHelper.injectIntoProxy(pedm, "emailDomainDao", emailDomainDaoMock);
        TargetProxyHelper.injectIntoProxy(pedm, "profileEntityCacheManager", profileEntityCacheManagerMock);
        TargetProxyHelper.injectIntoProxy(pedm, "emailDomainManager", emailDomainManagerMock);

        ProfileEmailDomainEntity ped1 = new ProfileEmailDomainEntity();
        ProfileEmailDomainEntity ped2 = new ProfileEmailDomainEntity();
        ProfileEmailDomainEntity ped3 = new ProfileEmailDomainEntity();

        ped1.setEmailDomain(EMAIL_DOMAIN);
        ped1.setOrcid(ORCID);
        ped1.setVisibility(Visibility.PUBLIC.value());

        ped2.setEmailDomain(EMAIL_DOMAIN_TWO);
        ped2.setOrcid(ORCID);
        ped2.setVisibility(Visibility.LIMITED.value());

        ped3.setEmailDomain(EMAIL_DOMAIN);
        ped3.setOrcid(ORCID_TWO);
        ped3.setVisibility(Visibility.PUBLIC.value());
        
        EmailDomainEntity e1 = new EmailDomainEntity();
        e1.setEmailDomain(EMAIL_DOMAIN);
        e1.setCategory(DomainCategory.PROFESSIONAL);
        
        EmailDomainEntity e2 = new EmailDomainEntity();
        e2.setEmailDomain(EMAIL_DOMAIN_TWO);
        e2.setCategory(DomainCategory.PROFESSIONAL);

        EmailDomain edPojo1 = new EmailDomain();
        edPojo1.setEmailDomain(EMAIL_DOMAIN);
        edPojo1.setCategory(DomainCategory.PROFESSIONAL);

        EmailDomain edPojo2 = new EmailDomain();
        edPojo2.setEmailDomain(EMAIL_DOMAIN_TWO);
        edPojo2.setCategory(DomainCategory.PROFESSIONAL);
      
        
        when(profileEmailDomainDaoReadOnlyMock.findByEmailDomain(eq(ORCID), eq(EMAIL_DOMAIN))).thenReturn(ped1);
        when(profileEmailDomainDaoReadOnlyMock.findByEmailDomain(eq(ORCID), eq(EMAIL_DOMAIN_TWO))).thenReturn(ped2);
        when(profileEmailDomainDaoReadOnlyMock.findByEmailDomain(eq(ORCID_TWO), eq(EMAIL_DOMAIN))).thenReturn(ped3);
        when(profileEmailDomainDaoReadOnlyMock.findByEmailDomain(eq(ORCID), eq(EMAIL_DOMAIN_THREE))).thenReturn(null);

        when(profileEmailDomainDaoReadOnlyMock.findByOrcid(eq(ORCID))).thenReturn(List.of(ped1, ped2));
        when(profileEmailDomainDaoReadOnlyMock.findByOrcid(eq(ORCID_TWO))).thenReturn(List.of(ped3));

        when(profileEmailDomainDaoReadOnlyMock.findPublicEmailDomains(eq(ORCID))).thenReturn(List.of(ped1));
        when(profileEmailDomainDaoReadOnlyMock.findPublicEmailDomains(eq(ORCID_TWO))).thenReturn(List.of(ped2));

        when(profileEmailDomainDaoMock.addEmailDomain(eq(ORCID), eq(EMAIL_DOMAIN_TWO), eq(Visibility.LIMITED.value()))).thenReturn(ped2);

        when(profileEmailDomainDaoMock.updateVisibility(eq(ORCID), eq(EMAIL_DOMAIN_TWO), eq(Visibility.LIMITED.value()))).thenReturn(true);
        
        when(emailDomainManagerMock.findByEmailDomain(eq(EMAIL_DOMAIN))).thenReturn(List.of(edPojo1));
        when(emailDomainManagerMock.findByEmailDomain(eq(EMAIL_DOMAIN_TWO))).thenReturn(List.of(edPojo2));

        ProfileEntity profile = new ProfileEntity();
        profile.setActivitiesVisibilityDefault(Visibility.PUBLIC.value());
        when(profileEntityCacheManagerMock.retrieve(anyString())).thenReturn(profile);
    }

    @Test(expected = IllegalArgumentException.class)
    public void processDomain_nullOrcid() {
        pedm.processDomain(null, "email@orcid.org");
    }

    @Test(expected = IllegalArgumentException.class)
    public void processDomain_nullDomain() {
        pedm.processDomain(ORCID, null);
    }

    @Test
    public void processDomain_domainAlreadyAdded() {
        EmailDomain professionalEmailDomain = new EmailDomain();
        professionalEmailDomain.setCategory(DomainCategory.PROFESSIONAL);
        professionalEmailDomain.setEmailDomain(EMAIL_DOMAIN);
        when(emailDomainManagerMock.findByEmailDomain(eq(EMAIL_DOMAIN))).thenReturn(List.of(professionalEmailDomain));
        pedm.processDomain(ORCID, "email@orcid.org");
        verify(profileEmailDomainDaoReadOnlyMock, times(1)).findByEmailDomain(eq(ORCID), eq(EMAIL_DOMAIN));
        verify(profileEmailDomainDaoMock, never()).addEmailDomain(anyString(), anyString(), anyString());
    }

    @Test
    public void processDomain_doNotAddUnknownDomain() {
        when(emailDomainManagerMock.findByEmailDomain(eq(EMAIL_DOMAIN))).thenReturn(null);
        pedm.processDomain(ORCID, "email@orcid.org");
        verify(profileEmailDomainDaoReadOnlyMock, never()).findByEmailDomain(anyString(), anyString());
        verify(profileEmailDomainDaoMock, never()).addEmailDomain(anyString(), anyString(), anyString());
    }

    @Test
    public void processDomain_doNotAddPersonalDomain() {
        EmailDomain professionalEmailDomain = new EmailDomain();
        professionalEmailDomain.setCategory(DomainCategory.PERSONAL);
        professionalEmailDomain.setEmailDomain(EMAIL_DOMAIN);
        when(emailDomainManagerMock.findByEmailDomain(eq(EMAIL_DOMAIN))).thenReturn(List.of(professionalEmailDomain));
        pedm.processDomain(ORCID, "email@orcid.org");
        verify(profileEmailDomainDaoReadOnlyMock, never()).findByEmailDomain(anyString(), anyString());
        verify(profileEmailDomainDaoMock, never()).addEmailDomain(anyString(), anyString(), anyString());
    }

    @Test
    public void processDomain_addDomain() {
        EmailDomain professionalEmailDomain = new EmailDomain();
        professionalEmailDomain.setCategory(DomainCategory.PROFESSIONAL);
        professionalEmailDomain.setEmailDomain(EMAIL_DOMAIN_THREE);
        when(emailDomainManagerMock.findByEmailDomain(eq(EMAIL_DOMAIN_THREE))).thenReturn(List.of(professionalEmailDomain));
        pedm.processDomain(ORCID, "email@domain.net");
        verify(profileEmailDomainDaoReadOnlyMock, times(1)).findByEmailDomain(eq(ORCID), eq(EMAIL_DOMAIN_THREE));
        verify(profileEmailDomainDaoMock, times(1)).addEmailDomain(ORCID, EMAIL_DOMAIN_THREE, Visibility.PUBLIC.value());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateEmailDomains_nullOrcid() {
        pedm.updateEmailDomains(null, new org.orcid.pojo.ajaxForm.Emails(), new org.orcid.jaxb.model.v3.release.record.Emails());
    }

    @Test
    public void updateEmailDomains_updateVisibility() {
        org.orcid.pojo.ajaxForm.Emails emails = new org.orcid.pojo.ajaxForm.Emails();
        ProfileEmailDomain ed1 = new ProfileEmailDomain();
        ed1.setVisibility(Visibility.LIMITED.value());
        ed1.setValue(EMAIL_DOMAIN);
        ProfileEmailDomain ed2 = new ProfileEmailDomain();
        ed2.setVisibility(Visibility.PRIVATE.value());
        ed2.setValue(EMAIL_DOMAIN_TWO);
        emails.setEmailDomains(List.of(ed1, ed2));
        
        //current emails
        org.orcid.jaxb.model.v3.release.record.Emails recordEmails  = new org.orcid.jaxb.model.v3.release.record.Emails();
        org.orcid.jaxb.model.v3.release.record.Email email1 = new org.orcid.jaxb.model.v3.release.record.Email();
        email1.setEmail("one@" + EMAIL_DOMAIN);
        email1.setVerified(true);
        org.orcid.jaxb.model.v3.release.record.Email email2 = new org.orcid.jaxb.model.v3.release.record.Email();
        email2.setEmail("two@" + EMAIL_DOMAIN_TWO);
        email2.setVerified(true);
        recordEmails.setEmails(List.of(email1, email2));
        pedm.updateEmailDomains(ORCID, emails, recordEmails);
        
        verify(profileEmailDomainDaoMock, times(1)).updateVisibility(ORCID, EMAIL_DOMAIN, Visibility.LIMITED.value());
        verify(profileEmailDomainDaoMock, times(1)).updateVisibility(ORCID, EMAIL_DOMAIN_TWO, Visibility.PRIVATE.value());
        verify(profileEmailDomainDaoMock, never()).removeEmailDomain(anyString(), anyString());
    }

    @Test
    public void updateEmailDomains_makeNoChanges() {
        // Visibility setting is the same -- no change necessary
        org.orcid.pojo.ajaxForm.Emails emails = new org.orcid.pojo.ajaxForm.Emails();
        ProfileEmailDomain ed1 = new ProfileEmailDomain();
        ed1.setVisibility(Visibility.PUBLIC.value());
        ed1.setValue(EMAIL_DOMAIN);
        emails.setEmailDomains(List.of(ed1));
      //current emails
        org.orcid.jaxb.model.v3.release.record.Emails recordEmails  = new org.orcid.jaxb.model.v3.release.record.Emails();
        org.orcid.jaxb.model.v3.release.record.Email email1 = new org.orcid.jaxb.model.v3.release.record.Email();
        email1.setEmail("one@" + EMAIL_DOMAIN);
        email1.setVerified(true);
        recordEmails.setEmails(List.of(email1));
        pedm.updateEmailDomains(ORCID_TWO, emails, recordEmails);
        verify(profileEmailDomainDaoMock, never()).updateVisibility(anyString(), anyString(), anyString());
        verify(profileEmailDomainDaoMock, never()).removeEmailDomain(anyString(), anyString());
    }

    @Test
    public void updateEmailDomains_removeDomain() {
        org.orcid.pojo.ajaxForm.Emails emails = new org.orcid.pojo.ajaxForm.Emails();
        emails.setEmailDomains(List.of(new ProfileEmailDomain()));
        pedm.updateEmailDomains(ORCID, emails, new org.orcid.jaxb.model.v3.release.record.Emails());
        verify(profileEmailDomainDaoMock, never()).updateVisibility(anyString(), anyString(), anyString());
        verify(profileEmailDomainDaoMock, times(1)).removeEmailDomain(ORCID, EMAIL_DOMAIN);
        verify(profileEmailDomainDaoMock, times(1)).removeEmailDomain(ORCID, EMAIL_DOMAIN_TWO);
    }

    @Test
    public void moveEmailDomainToAnotherAccount() {
        pedm.moveEmailDomainToAnotherAccount(EMAIL_DOMAIN, ORCID, ORCID_TWO);
        verify(profileEmailDomainDaoMock, never()).moveEmailDomainToAnotherAccount(anyString(), anyString(), anyString());
    }

    @Test
    public void moveEmailDomainToAnotherAccount_AlreadyExists() {
        pedm.moveEmailDomainToAnotherAccount(EMAIL_DOMAIN_THREE, ORCID_TWO, ORCID);
        verify(profileEmailDomainDaoMock, times(1)).moveEmailDomainToAnotherAccount(EMAIL_DOMAIN_THREE, ORCID_TWO, ORCID);
    }
}