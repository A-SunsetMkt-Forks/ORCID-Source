package org.orcid.api.memberV3.server.delegator;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.orcid.core.exception.OrcidUnauthorizedException;
import org.orcid.core.togglz.Features;
import org.orcid.core.utils.SecurityContextTestUtils;
import org.orcid.jaxb.model.groupid_v2.GroupIdRecord;
import org.orcid.jaxb.model.message.ScopePathType;
import org.orcid.jaxb.model.v3.release.common.Visibility;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Distinction;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.jaxb.model.v3.release.record.Emails;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.InvitedPosition;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.Membership;
import org.orcid.jaxb.model.v3.release.record.OtherName;
import org.orcid.jaxb.model.v3.release.record.PeerReview;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.ResearchResource;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrl;
import org.orcid.jaxb.model.v3.release.record.Service;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkBulk;
import org.orcid.test.DBUnitTest;
import org.orcid.test.OrcidJUnit4ClassRunner;
import org.orcid.test.helper.v3.Utils;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OrcidJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-orcid-api-web-context.xml" })
public class MemberV3ApiServiceDelegator_EmailsTest extends DBUnitTest {
    protected static final List<String> DATA_FILES = Arrays.asList("/data/EmptyEntityData.xml",
            "/data/SourceClientDetailsEntityData.xml", "/data/ProfileEntityData.xml", "/data/ClientDetailsEntityData.xml", "/data/RecordNameEntityData.xml",
            "/data/BiographyEntityData.xml");

    // Now on, for any new test, PLAESE USER THIS ORCID ID
    protected final String ORCID = "0000-0000-0000-0003";

    @Resource(name = "memberV3ApiServiceDelegator")
    protected MemberV3ApiServiceDelegator<Distinction, Education, Employment, PersonExternalIdentifier, InvitedPosition, Funding, GroupIdRecord, Membership, OtherName, PeerReview, Qualification, ResearcherUrl, Service, Work, WorkBulk, Address, Keyword, ResearchResource> serviceDelegator;

    @BeforeClass
    public static void initDBUnitData() throws Exception {
        initDBUnitData(DATA_FILES);
    }

    @AfterClass
    public static void removeDBUnitData() throws Exception {
        Collections.reverse(DATA_FILES);
        removeDBUnitData(DATA_FILES);
    }

    @Test(expected = OrcidUnauthorizedException.class)
    public void testViewEmailsWrongToken() {
        SecurityContextTestUtils.setUpSecurityContext("some-other-user", ScopePathType.READ_LIMITED);
        serviceDelegator.viewEmails(ORCID);
    }

    @Test
    public void testViewEmailsReadPublic() {
        SecurityContextTestUtils.setUpSecurityContextForClientOnly("APP-5555555555555555", ScopePathType.READ_PUBLIC);
        Response r = serviceDelegator.viewEmails(ORCID);
        Emails element = (Emails) r.getEntity();
        assertNotNull(element);
        assertEquals("/0000-0000-0000-0003/email", element.getPath());
        assertEquals(4, element.getEmails().size());
        List<String> emails = new ArrayList<>();
        emails.add("public_0000-0000-0000-0003@test.orcid.org");
        emails.add("public_0000-0000-0000-0003@orcid.org");
        emails.add("limited_0000-0000-0000-0003@test.orcid.org");
        emails.add("private_0000-0000-0000-0003@test.orcid.org");

        for(Email e : element.getEmails()) {
            if(!emails.contains(e.getEmail())) {
                fail(e.getEmail() + " is not in the email list");
            }
            emails.remove(e.getEmail());
        }
        assertTrue(emails.isEmpty());
    }

    @Test
    public void testViewEmailsReadPublic_ClientNotSourceOfAnyEmail() {
        SecurityContextTestUtils.setUpSecurityContextForClientOnly("APP-5555555555555556", ScopePathType.READ_PUBLIC);
        Response r = serviceDelegator.viewEmails(ORCID);
        Emails element = (Emails) r.getEntity();
        assertNotNull(element);
        assertEquals("/0000-0000-0000-0003/email", element.getPath());
        assertEquals(2, element.getEmails().size());
        List<String> emails = new ArrayList<>();
        emails.add("public_0000-0000-0000-0003@test.orcid.org");
        emails.add("public_0000-0000-0000-0003@orcid.org");

        for(Email e : element.getEmails()) {
            if(!emails.contains(e.getEmail())) {
                fail(e.getEmail() + " is not in the email list");
            }
            emails.remove(e.getEmail());
        }
        assertTrue(emails.isEmpty());
    }

    @Test
    public void testReadPublicScope_Emails() {
        SecurityContextTestUtils.setUpSecurityContext(ORCID, ScopePathType.READ_PUBLIC);
        Response r = serviceDelegator.viewEmails(ORCID);
        assertNotNull(r);
        assertEquals(Emails.class.getName(), r.getEntity().getClass().getName());
        Emails email = (Emails) r.getEntity();
        assertNotNull(email);
        assertEquals("/0000-0000-0000-0003/email", email.getPath());
        Utils.verifyLastModified(email.getLastModifiedDate());
        assertEquals(4, email.getEmails().size());
        boolean found1 = false;
        boolean found2 = false;
        boolean found3 = false;
        boolean found4 = false;
        for (Email element : email.getEmails()) {
            Utils.verifyLastModified(element.getLastModifiedDate());
            if (element.getEmail().equals("public_0000-0000-0000-0003@test.orcid.org")) {
                found1 = true;
            } else if (element.getEmail().equals("limited_0000-0000-0000-0003@test.orcid.org")) {
                found2 = true;
            } else if (element.getEmail().equals("private_0000-0000-0000-0003@test.orcid.org")) {
                found3 = true;
            } else if (element.getEmail().equals("public_0000-0000-0000-0003@orcid.org")) {
                found4 = true;
            } else {
                fail("Invalid put code " + element.getPutCode());
            }

        }
        assertTrue(found1);
        assertTrue(found2);
        assertTrue(found3);
        assertTrue(found4);
    }

    @Test
    public void testReadEmailPrivate() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4497", ScopePathType.EMAIL_READ_PRIVATE);
        Response r = serviceDelegator.viewEmails("4444-4444-4444-4497");
        assertNotNull(r);
        assertEquals(Emails.class.getName(), r.getEntity().getClass().getName());
        Emails email = (Emails) r.getEntity();
        assertNotNull(email);
        assertEquals("/4444-4444-4444-4497/email", email.getPath());
        assertNotNull(email.getLastModifiedDate());
        assertEquals(3, email.getEmails().size());
        assertEquals("public_4444-4444-4444-4497@test.orcid.org", email.getEmails().get(0).getEmail());
        assertEquals(Visibility.PUBLIC, email.getEmails().get(0).getVisibility());
        assertEquals("limited_4444-4444-4444-4497@test.orcid.org", email.getEmails().get(1).getEmail());
        assertEquals(Visibility.LIMITED, email.getEmails().get(1).getVisibility());
        assertEquals("private_4444-4444-4444-4497@test.orcid.org", email.getEmails().get(2).getEmail());
        assertEquals(Visibility.PRIVATE, email.getEmails().get(2).getVisibility());
    }

    @Test
    public void testViewEmails() {
        SecurityContextTestUtils.setUpSecurityContext("4444-4444-4444-4443", ScopePathType.PERSON_READ_LIMITED);
        Response response = serviceDelegator.viewEmails("4444-4444-4444-4443");
        assertNotNull(response);
        Emails emails = (Emails) response.getEntity();
        assertNotNull(emails);
        assertEquals("/4444-4444-4444-4443/email", emails.getPath());
        Utils.verifyLastModified(emails.getLastModifiedDate());
        assertNotNull(emails.getEmails());
        assertEquals(1, emails.getEmails().size());
        Email email = emails.getEmails().get(0);
        assertEquals("teddybass3private@semantico.com", email.getEmail());
        assertEquals(Visibility.PRIVATE, email.getVisibility());
        assertEquals("APP-5555555555555555", email.retrieveSourcePath());
        assertEquals(true, email.isVerified());
        assertEquals(false, email.isPrimary());       
    }

    @Test
    public void checkSourceOnEmail_EmailEndpointTest() {
        String orcid = "0000-0000-0000-0001";
        SecurityContextTestUtils.setUpSecurityContextForClientOnly("APP-5555555555555555", ScopePathType.EMAIL_READ_PRIVATE);
        Response r = serviceDelegator.viewEmails(orcid);
        Emails emails = (Emails) r.getEntity();
        checkEmails(emails);
    }

    private void checkEmails(Emails emails) {
        assertEquals(2, emails.getEmails().size());
        for(Email e : emails.getEmails()) {
            if(e.getEmail().equals("limited_verified_0000-0000-0000-0001@test.orcid.org")) {
                assertTrue(e.isVerified());
                // The source and name on non verified professional email addresses should not change
                assertEquals("APP-5555555555555555", e.getSource().retrieveSourcePath());
                assertEquals("Source Client 1", e.getSource().getSourceName().getContent());
            } else if(e.getEmail().equals("verified_non_professional@nonprofessional.org")) {
                assertTrue(e.isVerified());
                // The source and name on non professional email addresses should not change
                assertEquals("APP-5555555555555555", e.getSource().retrieveSourcePath());
                assertEquals("Source Client 1", e.getSource().getSourceName().getContent());
            } else {
                fail("Unexpected email " + e.getEmail());
            }
        }
    }
}
