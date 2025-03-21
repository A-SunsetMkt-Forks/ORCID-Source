package org.orcid.frontend.web.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.core.manager.EncryptionManager;
import org.orcid.core.manager.ProfileEntityCacheManager;
import org.orcid.core.manager.RegistrationManager;
import org.orcid.core.manager.v3.EmailManager;
import org.orcid.core.manager.v3.ProfileEntityManager;
import org.orcid.core.manager.v3.ProfileHistoryEventManager;
import org.orcid.core.manager.v3.read_only.EmailManagerReadOnly;
import org.orcid.core.profile.history.ProfileHistoryEventType;
import org.orcid.core.security.OrcidRoles;
import org.orcid.core.togglz.Features;
import org.orcid.core.utils.SecurityContextTestUtils;
import org.orcid.frontend.email.RecordEmailSender;
import org.orcid.jaxb.model.common.AvailableLocales;
import org.orcid.jaxb.model.message.CreationMethod;
import org.orcid.jaxb.model.v3.release.common.Visibility;
import org.orcid.persistence.jpa.entities.ProfileEntity;
import org.orcid.pojo.Redirect;
import org.orcid.pojo.ajaxForm.Checkbox;
import org.orcid.pojo.ajaxForm.Registration;
import org.orcid.pojo.ajaxForm.Text;
import org.orcid.test.DBUnitTest;
import org.orcid.test.OrcidJUnit4ClassRunner;
import org.orcid.test.TargetProxyHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.togglz.junit.TogglzRule;

import com.google.common.collect.Lists;

@RunWith(OrcidJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(locations = { "classpath:test-frontend-web-servlet.xml" })
public class RegistrationControllerTest extends DBUnitTest {

    private static final List<String> DATA_FILES = Arrays.asList("/data/EmptyEntityData.xml",
            "/data/SourceClientDetailsEntityData.xml", "/data/ProfileEntityData.xml", "/data/ClientDetailsEntityData.xml", "/data/RecordNameEntityData.xml", "/data/BiographyEntityData.xml");
    
    @Resource(name = "registrationController")
    RegistrationController registrationController;

    @Mock
    RegistrationManager registrationManager;
    
    @Mock
    private HttpServletRequest servletRequest;
    
    @Mock
    private HttpServletResponse servletResponse;
    
    @Mock
    private EmailManager emailManager;
    
    @Mock
    private ProfileEntityManager profileEntityManager;
    
    @Mock
    private ProfileHistoryEventManager profileHistoryEventManager;
    
    @Mock
    private RecordEmailSender recordEmailSender;    
    
    @Mock
    private EncryptionManager encryptionManagerMock;
    
    @Mock
    private EmailManagerReadOnly emailManagerReadOnlyMock;
    
    @Mock
    private AuthenticationManager authenticationManagerMock; 
    
    @Rule
    public TogglzRule togglzRule = TogglzRule.allDisabled(Features.class);
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        initDBUnitData(DATA_FILES);
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        removeDBUnitData(Lists.reverse(DATA_FILES));
    }
    
    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);        
        TargetProxyHelper.injectIntoProxy(registrationController, "registrationManager", registrationManager);        
        TargetProxyHelper.injectIntoProxy(registrationController, "emailManager", emailManager); 
        TargetProxyHelper.injectIntoProxy(registrationController, "profileEntityManager", profileEntityManager);        
        TargetProxyHelper.injectIntoProxy(registrationController, "encryptionManager", encryptionManagerMock);
        TargetProxyHelper.injectIntoProxy(registrationController, "emailManagerReadOnly", emailManagerReadOnlyMock);
        TargetProxyHelper.injectIntoProxy(registrationController, "authenticationManager", authenticationManagerMock); 
        TargetProxyHelper.injectIntoProxy(registrationController, "profileHistoryEventManager", profileHistoryEventManager); 
        TargetProxyHelper.injectIntoProxy(registrationController, "recordEmailSender", recordEmailSender);         
        
        when(servletRequest.getLocale()).thenReturn(Locale.ENGLISH);
        
        HttpSession session = mock(HttpSession.class);
        when(servletRequest.getSession()).thenReturn(session);
        
        when(authenticationManagerMock.authenticate(Mockito.any())).thenAnswer(new Answer<UsernamePasswordAuthenticationToken>() {
            @Override
            public UsernamePasswordAuthenticationToken answer(InvocationOnMock invocation) throws Throwable {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("0000-0000-0000-0000", "pwd", Arrays.asList(new SimpleGrantedAuthority(OrcidRoles.ROLE_USER.name())));
                auth.setDetails(new User("0000-0000-0000-0000", "pwd", List.of()));
                return auth;
            }
        });
        
        doNothing().when(profileHistoryEventManager).recordEvent(Mockito.any(ProfileHistoryEventType.class), Mockito.anyString());
        doNothing().when(recordEmailSender).sendWelcomeEmail(Mockito.anyString(), Mockito.anyString());        
    }
    
    @Test
    public void testStripHtmlFromNames() throws UnsupportedEncodingException {
        Text email = Text.valueOf(System.currentTimeMillis() + "@test.orcid.org");
        
        when(registrationManager.createMinimalRegistration(Mockito.any(Registration.class), eq(false), Mockito.any(java.util.Locale.class), Mockito.anyString())).thenAnswer(new Answer<String>(){
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return "0000-0000-0000-0000";                
            }
        });
        Registration reg = new Registration();
        org.orcid.pojo.ajaxForm.Visibility fv = new org.orcid.pojo.ajaxForm.Visibility();
        fv.setVisibility(Visibility.PUBLIC);
        reg.setActivitiesVisibilityDefault(fv);        
        reg.setEmail(email);
        reg.setEmailConfirm(email);
        reg.setFamilyNames(Text.valueOf("<button onclick=\"alert('hello')\">Family Name</button>"));
        reg.setGivenNames(Text.valueOf("<button onclick=\"alert('hello')\">Given Names</button>"));
        reg.setPassword(Text.valueOf("1234abcd"));
        reg.setPasswordConfirm(Text.valueOf("1234abcd"));
        reg.setValNumClient(2L);
        reg.setValNumServer(4L);
        Checkbox c = new Checkbox();
        c.setValue(true);
        reg.setTermsOfUse(c);
        reg.setCreationType(Text.valueOf(CreationMethod.API.value()));

        Redirect redirect = registrationController.setRegisterConfirm(servletRequest, servletResponse, reg);
        assertTrue(redirect.getUrl().endsWith("?justRegistered"));
        
        ArgumentCaptor<Registration> argument1 = ArgumentCaptor.forClass(Registration.class);
        ArgumentCaptor<Boolean> argument2 = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Locale> argument3 = ArgumentCaptor.forClass(Locale.class);
        ArgumentCaptor<String> argument4 = ArgumentCaptor.forClass(String.class);        
        verify(registrationManager).createMinimalRegistration(argument1.capture(), argument2.capture(), argument3.capture(), argument4.capture());
        assertNotNull(argument1.getValue());
        Registration form = argument1.getValue();
        assertEquals("Given Names", form.getGivenNames().getValue());
        assertEquals("Family Name", form.getFamilyNames().getValue());      
    }
    
    @Test
    public void regActivitiesVisibilityDefaultIsNotNull() {
        Registration reg = new Registration();
        reg.getActivitiesVisibilityDefault().setVisibility(null);
        assertNull(reg.getActivitiesVisibilityDefault().getVisibility());
        reg = registrationController.registerActivitiesVisibilityDefaultValidate(reg);
        assertNotNull(reg);
        assertNotNull(reg.getActivitiesVisibilityDefault().getErrors());
        assertEquals(1, reg.getActivitiesVisibilityDefault().getErrors().size());
        assertEquals("Please choose a default visibility setting.", reg.getActivitiesVisibilityDefault().getErrors().get(0));
    }
    @Test
    public void regEmailsAdditonalValidateNotSameAsOtherAdditional() {
        String additionalEmail = "email1@test.orcid.org";

        Registration reg = new Registration();
        List<Text> emailsAdditionalList = new ArrayList<Text>();
        Text emailAdditional01 = new Text();
        Text emailAdditional02 = new Text();
        emailAdditional01.setValue(additionalEmail);
        emailAdditional02.setValue(additionalEmail);
        emailsAdditionalList.add(emailAdditional01);
        emailsAdditionalList.add(emailAdditional02);
        reg.setEmailsAdditional(emailsAdditionalList);
        
        registrationController.additionalEmailsValidateOnRegister(servletRequest, reg);
        
        assertNotNull(reg);
        assertNotNull(reg.getEmailsAdditional());
        for(Text emailAdditionalListItem : reg.getEmailsAdditional()){
            assertNotNull(emailAdditionalListItem.getErrors());
            assertEquals(1, emailAdditionalListItem.getErrors().size());
            assertEquals("Additional email cannot match another email", emailAdditionalListItem.getErrors().get(0));
        }
    }
    
    @Test
    public void regEmailValidateUnclaimedAccountTest() {
        String email = "email1@test.orcid.org";
        String orcid = "0000-0000-0000-0000";
        when(emailManager.emailExists(email)).thenReturn(true); 
        when(emailManager.findOrcidIdByEmail(email)).thenReturn(orcid);
        when(profileEntityManager.isProfileClaimedByEmail(email)).thenReturn(false);
        when(profileEntityManager.isDeactivated(orcid)).thenReturn(false);
        when(emailManager.isAutoDeprecateEnableForEmail(email)).thenReturn(true);
        
        Registration reg = new Registration();
        reg.setEmail(Text.valueOf("email1@test.orcid.org"));
        reg.setEmailConfirm(Text.valueOf("email1@test.orcid.org"));
        reg = registrationController.regEmailValidate(servletRequest, reg, false, true);
        
        assertNotNull(reg);
        assertNotNull(reg.getEmail());
        assertNotNull(reg.getEmail().getErrors());
        //No errors, since the account can be auto deprecated
        assertTrue(reg.getEmail().getErrors().isEmpty());       
    }
    
    @Test
    public void regEmailsAdditonalValidateUnclaimedAccountTest() {
        String additionalEmail = "email1@test.orcid.org";
        String orcid = "0000-0000-0000-0000";
        when(emailManager.emailExists(additionalEmail)).thenReturn(true); 
        when(emailManager.findOrcidIdByEmail(additionalEmail)).thenReturn(orcid);
        when(profileEntityManager.isProfileClaimedByEmail(additionalEmail)).thenReturn(false);
        when(profileEntityManager.isDeactivated(orcid)).thenReturn(false);
        when(emailManager.isAutoDeprecateEnableForEmail(additionalEmail)).thenReturn(true);
        
        Registration reg = new Registration();
        List<Text> emailsAdditionalList = new ArrayList<Text>();
        Text emailAdditional = new Text();
        emailAdditional.setValue(additionalEmail);
        emailsAdditionalList.add(emailAdditional);
        reg.setEmailsAdditional(emailsAdditionalList);
        
        registrationController.additionalEmailsValidateOnRegister(servletRequest, reg);
        
        assertNotNull(reg);
        //No errors, since the account can be auto deprecated
        assertTrue(reg.getEmail().getErrors().isEmpty());     
        assertNotNull(reg.getEmailsAdditional());
        for(Text emailAdditionalListItem : reg.getEmailsAdditional()){
            assertNotNull(emailAdditionalListItem.getErrors());
            assertTrue(emailAdditionalListItem.getErrors().isEmpty());
        }
    }
    
    @Test
    public void regEmailValidateUnclaimedAccountButEnableAutoDeprecateDisableOnClientTest() {
    	String email = "email1@test.orcid.org";
    	String orcid = "0000-0000-0000-0000";
    	when(emailManager.emailExists(email)).thenReturn(true); 
    	when(emailManager.findOrcidIdByEmail(email)).thenReturn(orcid);
    	when(profileEntityManager.isProfileClaimedByEmail(email)).thenReturn(false);
    	when(profileEntityManager.isDeactivated(orcid)).thenReturn(false);
    	//Set enable auto deprecate off
    	when(emailManager.isAutoDeprecateEnableForEmail(email)).thenReturn(false);
    	when(servletRequest.getScheme()).thenReturn("http");    	
    	
    	Registration reg = new Registration();
    	reg.setEmail(Text.valueOf("email1@test.orcid.org"));
    	reg.setEmailConfirm(Text.valueOf("email1@test.orcid.org"));
    	reg = registrationController.regEmailValidate(servletRequest, reg, false, true);
    	
    	assertNotNull(reg);
    	assertNotNull(reg.getEmail());
    	assertNotNull(reg.getEmail().getErrors());
    	assertEquals(1, reg.getEmail().getErrors().size());
    	assertEquals("orcid.frontend.verify.unclaimed_email", reg.getEmail().getErrors().get(0));    	
    }
    
    @Test
    public void regEmailsAdditionalValidateUnclaimedAccountButEnableAutoDeprecateDisableOnClientTest() {
        String additionalEmail = "email1@test.orcid.org";
        String orcid = "0000-0000-0000-0000";
        when(emailManager.emailExists(additionalEmail)).thenReturn(true); 
        when(emailManager.findOrcidIdByEmail(additionalEmail)).thenReturn(orcid);
        when(profileEntityManager.isProfileClaimedByEmail(additionalEmail)).thenReturn(false);
        when(profileEntityManager.isDeactivated(orcid)).thenReturn(false);
        when(emailManager.isAutoDeprecateEnableForEmail(additionalEmail)).thenReturn(false);
        when(servletRequest.getScheme()).thenReturn("http");            
        
        Registration reg = new Registration();
        List<Text> emailsAdditionalList = new ArrayList<Text>();
        Text emailAdditional = new Text();
        emailAdditional.setValue(additionalEmail);
        emailsAdditionalList.add(emailAdditional);
        reg.setEmailsAdditional(emailsAdditionalList);
        registrationController.additionalEmailsValidateOnRegister(servletRequest, reg);
         
        assertNotNull(reg);
        assertNotNull(reg.getEmailsAdditional());
        for(Text emailAdditionalListItem : reg.getEmailsAdditional()){
            assertNotNull(emailAdditionalListItem.getErrors());
            assertEquals(1, emailAdditionalListItem.getErrors().size());
            assertEquals("orcid.frontend.verify.unclaimed_email", emailAdditionalListItem.getErrors().get(0));
        }
    }
    
    @Test
    public void regEmailValidateDeactivatedAccountTest() {
    	String email = "email1@test.orcid.org";
    	String orcid = "0000-0000-0000-0000";
    	when(emailManager.emailExists(email)).thenReturn(true); 
    	when(emailManager.findOrcidIdByEmail(email)).thenReturn(orcid);
    	when(profileEntityManager.isProfileClaimedByEmail(email)).thenReturn(false);
    	//Set it as deactivated
    	when(profileEntityManager.isDeactivated(orcid)).thenReturn(true);
    	    	
    	Registration reg = new Registration();
    	reg.setEmail(Text.valueOf("email1@test.orcid.org"));
    	reg.setEmailConfirm(Text.valueOf("email1@test.orcid.org"));
    	reg = registrationController.regEmailValidate(servletRequest, reg, false, true);
    	
    	assertNotNull(reg);
    	assertNotNull(reg.getEmail());
    	assertNotNull(reg.getEmail().getErrors());
    	assertEquals(1, reg.getEmail().getErrors().size());
    	assertTrue(reg.getEmail().getErrors().get(0).startsWith("orcid.frontend.verify.deactivated_email"));
    }
    
    @Test
    public void regEmailsAdditionalValidateDeactivatedAccountTest() {
        String additionalEmail = "email1@test.orcid.org";
        String orcid = "0000-0000-0000-0000";
        when(emailManager.emailExists(additionalEmail)).thenReturn(true); 
        when(emailManager.findOrcidIdByEmail(additionalEmail)).thenReturn(orcid);
        when(profileEntityManager.isProfileClaimedByEmail(additionalEmail)).thenReturn(false);
        //Set it as deactivated
        when(profileEntityManager.isDeactivated(orcid)).thenReturn(true);
                
        Registration reg = new Registration();
        List<Text> emailsAdditionalList = new ArrayList<Text>();
        Text emailAdditional = new Text();
        emailAdditional.setValue(additionalEmail);
        emailsAdditionalList.add(emailAdditional);
        reg.setEmailsAdditional(emailsAdditionalList);
        registrationController.additionalEmailsValidateOnRegister(servletRequest, reg);
         
        assertNotNull(reg);
        assertNotNull(reg.getEmailsAdditional());
        for(Text emailAdditionalListItem : reg.getEmailsAdditional()){
            assertNotNull(emailAdditionalListItem.getErrors());
            assertEquals(1, emailAdditionalListItem.getErrors().size());
            assertTrue(emailAdditionalListItem.getErrors().get(0).startsWith("orcid.frontend.verify.deactivated_email"));
        }
    }
    
    @Test
    public void regEmailsAdditionalValidateDeactivatedAndUnclaimedAccountTest() {
        String additionalEmail = "email1@test.orcid.org";
        String orcid = "0000-0000-0000-0000";
        when(emailManager.emailExists(additionalEmail)).thenReturn(true);
        //Set it as unclaimed
        when(emailManager.findOrcidIdByEmail(additionalEmail)).thenReturn(orcid);
        when(profileEntityManager.isProfileClaimedByEmail(additionalEmail)).thenReturn(false);
        //And set it as deactivated
        when(profileEntityManager.isDeactivated(orcid)).thenReturn(true);
    	when(emailManager.isAutoDeprecateEnableForEmail(additionalEmail)).thenReturn(true);
    	
    	Registration reg = new Registration();
        List<Text> emailsAdditionalList = new ArrayList<Text>();
        Text emailAdditional = new Text();
        emailAdditional.setValue(additionalEmail);
        emailsAdditionalList.add(emailAdditional);
        reg.setEmailsAdditional(emailsAdditionalList);
        registrationController.additionalEmailsValidateOnRegister(servletRequest, reg);
    	
    	assertNotNull(reg);
        assertNotNull(reg.getEmailsAdditional());
        for(Text emailAdditionalListItem : reg.getEmailsAdditional()){
            assertNotNull(emailAdditionalListItem.getErrors());
            assertEquals(1, emailAdditionalListItem.getErrors().size());
            assertTrue(emailAdditionalListItem.getErrors().get(0).startsWith("orcid.frontend.verify.deactivated_email"));
        }
    }
    
    @Test
    public void regEmailValidateDeactivatedAndUnclaimedAccountTest() {
        String email = "email1@test.orcid.org";
        String orcid = "0000-0000-0000-0000";
        when(emailManager.emailExists(email)).thenReturn(true); 
        when(emailManager.findOrcidIdByEmail(email)).thenReturn(orcid);
        //Set it as unclaimed
        when(profileEntityManager.isProfileClaimedByEmail(email)).thenReturn(false);
        //And set it as deactivated
        when(profileEntityManager.isDeactivated(orcid)).thenReturn(true);
        when(emailManager.isAutoDeprecateEnableForEmail(email)).thenReturn(true);
        
        Registration reg = new Registration();
        reg.setEmail(Text.valueOf("email1@test.orcid.org"));
        reg.setEmailConfirm(Text.valueOf("email1@test.orcid.org"));
        reg = registrationController.regEmailValidate(servletRequest, reg, false, true);
        
        assertNotNull(reg);
        assertNotNull(reg.getEmail());
        assertNotNull(reg.getEmail().getErrors());
        assertEquals(1, reg.getEmail().getErrors().size());
        assertTrue(reg.getEmail().getErrors().get(0).startsWith("orcid.frontend.verify.deactivated_email"));
    }
    
    @Test
    public void regEmailsAdditionalValidateClaimedAccountTest() {
        String additionalEmail = "email1@test.orcid.org";
        String orcid = "0000-0000-0000-0000";
        when(emailManager.emailExists(additionalEmail)).thenReturn(true); 
        when(emailManager.findOrcidIdByEmail(additionalEmail)).thenReturn(orcid);
        //Set it as claimed
        when(profileEntityManager.isProfileClaimedByEmail(additionalEmail)).thenReturn(true);
        //And set it as active
        when(profileEntityManager.isDeactivated(orcid)).thenReturn(false);
        
        Registration reg = new Registration();
        List<Text> emailsAdditionalList = new ArrayList<Text>();
        Text emailAdditional = new Text();
        emailAdditional.setValue(additionalEmail);
        emailsAdditionalList.add(emailAdditional);
        reg.setEmailsAdditional(emailsAdditionalList);
        registrationController.additionalEmailsValidateOnRegister(servletRequest, reg);
        
        assertNotNull(reg);
        assertNotNull(reg.getEmailsAdditional());
        for(Text emailAdditionalListItem : reg.getEmailsAdditional()){
            assertNotNull(emailAdditionalListItem.getErrors());
            assertEquals(1, emailAdditionalListItem.getErrors().size());
            assertTrue(emailAdditionalListItem.getErrors().get(0).startsWith("orcid.frontend.verify.duplicate_email"));
        }     
    }
    
    @Test
    public void regEmailValidateClaimedAccountTest() {
    	String email = "email1@test.orcid.org";
    	String orcid = "0000-0000-0000-0000";
    	when(emailManager.emailExists(email)).thenReturn(true); 
    	when(emailManager.findOrcidIdByEmail(email)).thenReturn(orcid);
    	//Set it as claimed
    	when(profileEntityManager.isProfileClaimedByEmail(email)).thenReturn(true);
    	//And set it as active
    	when(profileEntityManager.isDeactivated(orcid)).thenReturn(false);
    	
    	Registration reg = new Registration();
    	reg.setEmail(Text.valueOf("email1@test.orcid.org"));
    	reg.setEmailConfirm(Text.valueOf("email1@test.orcid.org"));
    	reg = registrationController.regEmailValidate(servletRequest, reg, false, true);
    	
    	assertNotNull(reg);
    	assertNotNull(reg.getEmail());
    	assertNotNull(reg.getEmail().getErrors());
    	assertEquals(1, reg.getEmail().getErrors().size());
    	assertTrue(reg.getEmail().getErrors().get(0).startsWith("orcid.frontend.verify.duplicate_email"));    	
    }             
    
    @Test
    public void verifyEmailTest() throws UnsupportedEncodingException {
        String orcid = "0000-0000-0000-0000";
        String email = "user_1@test.orcid.org";
        SecurityContextTestUtils.setupSecurityContextForWebUser(orcid, email);
        String encodedEmail = new String(Base64.encodeBase64(email.getBytes()));
        when(encryptionManagerMock.decryptForExternalUse(Mockito.anyString())).thenReturn(email);
        when(emailManagerReadOnlyMock.emailExists(email)).thenReturn(true);
        when(emailManagerReadOnlyMock.findOrcidIdByEmail(email)).thenReturn(orcid);
        when(emailManager.verifyEmail(orcid, email)).thenReturn(true);
        when(emailManagerReadOnlyMock.isPrimaryEmail(orcid, email)).thenReturn(true);
        when(emailManagerReadOnlyMock.isPrimaryEmailVerified(orcid)).thenReturn(true);
        
        ModelAndView mav = registrationController.verifyEmail(servletRequest, servletResponse, encodedEmail);
        assertNotNull(mav);
        assertEquals("redirect:https://testserver.orcid.org/my-orcid?emailVerified=true", mav.getViewName());
        verify(emailManager, times(1)).verifyEmail(orcid, email);
        verify(profileEntityManager, times(1)).updateLocale(eq(orcid), eq(AvailableLocales.EN));
    }
        
    @Test
    public void verifyEmail_InvalidEmailTest() throws UnsupportedEncodingException {
        String orcid = "0000-0000-0000-0000";
        String email = "user_1@test.orcid.org";
        SecurityContextTestUtils.setupSecurityContextForWebUser(orcid, email);
        String encodedEmail = new String(Base64.encodeBase64(email.getBytes()));
        when(encryptionManagerMock.decryptForExternalUse(Mockito.anyString())).thenReturn(email);
        // Email doesn't exists
        when(emailManagerReadOnlyMock.emailExists(email)).thenReturn(false);
        when(emailManagerReadOnlyMock.findOrcidIdByEmail(email)).thenReturn(orcid);
        when(emailManager.verifyEmail(orcid, email)).thenReturn(true);
        when(emailManagerReadOnlyMock.isPrimaryEmail(orcid, email)).thenReturn(true);
        when(emailManagerReadOnlyMock.isPrimaryEmailVerified(orcid)).thenReturn(true);

        ModelAndView mav = registrationController.verifyEmail(servletRequest, servletResponse, encodedEmail);
        assertNotNull(mav);
        assertEquals("redirect:https://testserver.orcid.org/signin", mav.getViewName());
        verify(emailManager, times(0)).verifyEmail(Mockito.anyString(), Mockito.anyString());
    }
    
    @Test
    public void verifyEmail_NotVerifiedTest() throws UnsupportedEncodingException {
        String orcid = "0000-0000-0000-0000";
        String email = "user_1@test.orcid.org";
        SecurityContextTestUtils.setupSecurityContextForWebUser(orcid, email);
        String encodedEmail = new String(Base64.encodeBase64(email.getBytes()));
        when(encryptionManagerMock.decryptForExternalUse(Mockito.anyString())).thenReturn(email);
        when(emailManagerReadOnlyMock.emailExists(email)).thenReturn(true);
        when(emailManagerReadOnlyMock.findOrcidIdByEmail(email)).thenReturn(orcid);
        // For some reason the email wasn't verified
        when(emailManager.verifyEmail(orcid, email)).thenReturn(false);
        when(emailManagerReadOnlyMock.isPrimaryEmail(orcid, email)).thenReturn(true);
        when(emailManagerReadOnlyMock.isPrimaryEmailVerified(orcid)).thenReturn(true);
        
        ModelAndView mav = registrationController.verifyEmail(servletRequest, servletResponse, encodedEmail);
        assertNotNull(mav);
        assertEquals("redirect:https://testserver.orcid.org/my-orcid?emailVerified=false", mav.getViewName());
        verify(emailManager, times(1)).verifyEmail(Mockito.anyString(), Mockito.anyString());
    }
    
    @Test
    public void verifyEmail_UnableToDecryptEmailTest() throws UnsupportedEncodingException {
        String orcid = "0000-0000-0000-0000";
        String email = "user_1@test.orcid.org";
        SecurityContextTestUtils.setupSecurityContextForWebUser(orcid, email);
        String encodedEmail = new String(Base64.encodeBase64(email.getBytes()));
        when(encryptionManagerMock.decryptForExternalUse(Mockito.anyString())).thenThrow(new EncryptionOperationNotPossibleException());
        
        ModelAndView mav = registrationController.verifyEmail(servletRequest, servletResponse, encodedEmail);
        assertNotNull(mav);
        assertEquals("redirect:https://testserver.orcid.org/signin?invalidVerifyUrl=true", mav.getViewName());
        verify(emailManager, times(0)).verifyEmail(Mockito.anyString(), Mockito.anyString());
    }
}
