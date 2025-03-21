package org.orcid.frontend.web.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.orcid.core.oauth.OrcidOAuth2Authentication;
import org.orcid.core.oauth.service.OrcidAuthorizationEndpoint;
import org.orcid.frontend.util.RequestInfoFormLocalCache;
import org.orcid.jaxb.model.message.CreationMethod;
import org.orcid.pojo.ajaxForm.Checkbox;
import org.orcid.pojo.ajaxForm.OauthRegistrationForm;
import org.orcid.pojo.ajaxForm.Registration;
import org.orcid.pojo.ajaxForm.RequestInfoForm;
import org.orcid.pojo.ajaxForm.Text;
import org.orcid.test.DBUnitTest;
import org.orcid.test.OrcidJUnit4ClassRunner;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.view.RedirectView;

import com.google.common.collect.Lists;

public class OauthRegistrationControllerTest {
    OauthRegistrationController oauthRegistrationController = new OauthRegistrationController();
    
    @Mock
    RegistrationController registrationController;

    @Mock
    OrcidAuthorizationEndpoint authorizationEndpoint;
    
    @Mock
    AuthenticationManager authenticationManager;
    
    @Mock
    private HttpServletRequest servletRequest;
    
    @Mock
    private HttpServletResponse servletResponse;

    @Mock
    private RequestInfoFormLocalCache requestInfoFormLocalCache;
    
    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        oauthRegistrationController.setRegistrationController(registrationController);
        oauthRegistrationController.setAuthorizationEndpoint(authorizationEndpoint);
        oauthRegistrationController.setAuthenticationManager(authenticationManager);
    }

    @Test
    public void testStripHtmlFromNames() throws UnsupportedEncodingException {
        RequestInfoForm rf = new RequestInfoForm();
        HttpSession session = mock(HttpSession.class);
        requestInfoFormLocalCache = mock(RequestInfoFormLocalCache.class);
        when(requestInfoFormLocalCache.get(any())).thenReturn(rf);
        oauthRegistrationController.setRequestInfoFormLocalCache(requestInfoFormLocalCache);
        RedirectView mv = new RedirectView();
        when(servletRequest.getSession()).thenReturn(session);
        when(session.getId()).thenReturn("ID1");
        when(authorizationEndpoint.approveOrDeny(Matchers.anyMap(), Matchers.anyMap(), any(SessionStatus.class), any(Principal.class))).thenReturn(mv);
        when(authenticationManager.authenticate(any(Authentication.class))).thenAnswer(new Answer<Authentication>(){
            @Override
            public Authentication answer(InvocationOnMock invocation) throws Throwable {
                OrcidOAuth2Authentication mockedAuthentication = mock(OrcidOAuth2Authentication.class);
                return mockedAuthentication;
            }
        });
        
        Text email = Text.valueOf(System.currentTimeMillis() + "@test.orcid.org");
        
        OauthRegistrationForm reg = new OauthRegistrationForm();
        org.orcid.pojo.ajaxForm.Visibility fv = new org.orcid.pojo.ajaxForm.Visibility();
        fv.setVisibility(org.orcid.jaxb.model.v3.release.common.Visibility.PUBLIC);
        reg.setActivitiesVisibilityDefault(fv);        
        reg.setEmail(email);
        reg.setEmailConfirm(email);
        reg.setFamilyNames(Text.valueOf("<button onclick=\"alert('hello')\">Family Name</button>"));
        reg.setGivenNames(Text.valueOf("<button onclick=\"alert('hello')\">Given Names</button>"));
        reg.setPassword(Text.valueOf("1234abcd"));
        reg.setPasswordConfirm(Text.valueOf("1234abcd"));
        reg.setValNumClient(2L);
        reg.setValNumServer(4L);
        reg.setApproved(true);
        Checkbox c = new Checkbox();
        c.setValue(true);
        reg.setTermsOfUse(c);
        reg.setCreationType(Text.valueOf(CreationMethod.DIRECT.value()));
        reg.setPersistentTokenEnabled(true);
        oauthRegistrationController.registerAndAuthorize(servletRequest, servletResponse, reg);
        
        ArgumentCaptor<HttpServletRequest> argument1 = ArgumentCaptor.forClass(HttpServletRequest.class);
        ArgumentCaptor<Registration> argument2 = ArgumentCaptor.forClass(Registration.class);
        ArgumentCaptor<Boolean> argument3 = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Locale> argument4 = ArgumentCaptor.forClass(Locale.class);
        ArgumentCaptor<String> argument5 = ArgumentCaptor.forClass(String.class);        
                
        verify(registrationController).createMinimalRegistration(argument1.capture(), argument2.capture(), argument3.capture(), argument4.capture(), argument5.capture());
        assertNotNull(argument2.getValue());
        Registration registration = argument2.getValue();
        assertEquals(email.getValue(), registration.getEmail().getValue());
        assertEquals("Given Names", registration.getGivenNames().getValue());
        assertEquals("Family Name", registration.getFamilyNames().getValue());               
    }
}
