package org.orcid.api.memberV3.server.delegator.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Resource;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.orcid.api.common.util.ApiUtils;
import org.orcid.api.common.util.v3.ActivityUtils;
import org.orcid.api.common.util.v3.ElementUtils;
import org.orcid.api.memberV3.server.delegator.MemberV3ApiServiceDelegator;
import org.orcid.core.common.manager.EmailDomainManager;
import org.orcid.core.common.manager.SummaryManager;
import org.orcid.core.exception.DeactivatedException;
import org.orcid.core.exception.DuplicatedGroupIdRecordException;
import org.orcid.core.exception.MismatchedPutCodeException;
import org.orcid.core.exception.OrcidAccessControlException;
import org.orcid.core.exception.OrcidBadRequestException;
import org.orcid.core.exception.OrcidCoreExceptionMapper;
import org.orcid.core.exception.OrcidNoBioException;
import org.orcid.core.exception.OrcidNoResultException;
import org.orcid.core.groupIds.issn.IssnGroupIdPatternMatcher;
import org.orcid.core.locale.LocaleManager;
import org.orcid.core.manager.StatusManager;
import org.orcid.core.manager.impl.OrcidUrlManager;
import org.orcid.core.manager.read_only.ClientDetailsManagerReadOnly;
import org.orcid.core.manager.v3.AddressManager;
import org.orcid.core.manager.v3.AffiliationsManager;
import org.orcid.core.manager.v3.ExternalIdentifierManager;
import org.orcid.core.manager.v3.GroupIdRecordManager;
import org.orcid.core.manager.v3.OrcidSearchManager;
import org.orcid.core.manager.v3.OrcidSecurityManager;
import org.orcid.core.manager.v3.OtherNameManager;
import org.orcid.core.manager.v3.PeerReviewManager;
import org.orcid.core.manager.v3.ProfileEntityManager;
import org.orcid.core.manager.v3.ProfileFundingManager;
import org.orcid.core.manager.v3.ProfileKeywordManager;
import org.orcid.core.manager.v3.ResearchResourceManager;
import org.orcid.core.manager.v3.ResearcherUrlManager;
import org.orcid.core.manager.v3.SourceManager;
import org.orcid.core.manager.v3.WorkManager;
import org.orcid.core.manager.v3.read_only.ActivitiesSummaryManagerReadOnly;
import org.orcid.core.manager.v3.read_only.AddressManagerReadOnly;
import org.orcid.core.manager.v3.read_only.AffiliationsManagerReadOnly;
import org.orcid.core.manager.v3.read_only.BiographyManagerReadOnly;
import org.orcid.core.manager.v3.read_only.ClientManagerReadOnly;
import org.orcid.core.manager.v3.read_only.EmailManagerReadOnly;
import org.orcid.core.manager.v3.read_only.ExternalIdentifierManagerReadOnly;
import org.orcid.core.manager.v3.read_only.GroupIdRecordManagerReadOnly;
import org.orcid.core.manager.v3.read_only.OtherNameManagerReadOnly;
import org.orcid.core.manager.v3.read_only.PeerReviewManagerReadOnly;
import org.orcid.core.manager.v3.read_only.PersonDetailsManagerReadOnly;
import org.orcid.core.manager.v3.read_only.PersonalDetailsManagerReadOnly;
import org.orcid.core.manager.v3.read_only.ProfileFundingManagerReadOnly;
import org.orcid.core.manager.v3.read_only.ProfileKeywordManagerReadOnly;
import org.orcid.core.manager.v3.read_only.RecordManagerReadOnly;
import org.orcid.core.manager.v3.read_only.ResearchResourceManagerReadOnly;
import org.orcid.core.manager.v3.read_only.ResearcherUrlManagerReadOnly;
import org.orcid.core.manager.v3.read_only.WorkManagerReadOnly;
import org.orcid.core.model.RecordSummary;
import org.orcid.core.utils.SourceEntityUtils;
import org.orcid.core.utils.v3.ContributorUtils;
import org.orcid.core.utils.v3.SourceUtils;
import org.orcid.core.version.impl.Api3_0LastModifiedDatesHelper;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.message.ScopePathType;
import org.orcid.jaxb.model.v3.release.client.ClientSummary;
import org.orcid.jaxb.model.v3.release.groupid.GroupIdRecord;
import org.orcid.jaxb.model.v3.release.groupid.GroupIdRecords;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Addresses;
import org.orcid.jaxb.model.v3.release.record.Biography;
import org.orcid.jaxb.model.v3.release.record.Distinction;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.jaxb.model.v3.release.record.Emails;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.InvitedPosition;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.Keywords;
import org.orcid.jaxb.model.v3.release.record.Membership;
import org.orcid.jaxb.model.v3.release.record.OtherName;
import org.orcid.jaxb.model.v3.release.record.OtherNames;
import org.orcid.jaxb.model.v3.release.record.PeerReview;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifiers;
import org.orcid.jaxb.model.v3.release.record.PersonalDetails;
import org.orcid.jaxb.model.v3.release.record.Qualification;
import org.orcid.jaxb.model.v3.release.record.Record;
import org.orcid.jaxb.model.v3.release.record.ResearchResource;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrl;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrls;
import org.orcid.jaxb.model.v3.release.record.Service;
import org.orcid.jaxb.model.v3.release.record.SourceAware;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkBulk;
import org.orcid.jaxb.model.v3.release.record.summary.ActivitiesSummary;
import org.orcid.jaxb.model.v3.release.record.summary.DistinctionSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Distinctions;
import org.orcid.jaxb.model.v3.release.record.summary.EducationSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Educations;
import org.orcid.jaxb.model.v3.release.record.summary.EmploymentSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Employments;
import org.orcid.jaxb.model.v3.release.record.summary.FundingSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Fundings;
import org.orcid.jaxb.model.v3.release.record.summary.InvitedPositionSummary;
import org.orcid.jaxb.model.v3.release.record.summary.InvitedPositions;
import org.orcid.jaxb.model.v3.release.record.summary.MembershipSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Memberships;
import org.orcid.jaxb.model.v3.release.record.summary.PeerReviewSummary;
import org.orcid.jaxb.model.v3.release.record.summary.PeerReviews;
import org.orcid.jaxb.model.v3.release.record.summary.QualificationSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Qualifications;
import org.orcid.jaxb.model.v3.release.record.summary.ResearchResourceSummary;
import org.orcid.jaxb.model.v3.release.record.summary.ResearchResources;
import org.orcid.jaxb.model.v3.release.record.summary.ServiceSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Services;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.search.Search;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;
import org.orcid.persistence.jpa.entities.EmailDomainEntity;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class MemberV3ApiServiceDelegatorImpl implements
        MemberV3ApiServiceDelegator<Distinction, Education, Employment, PersonExternalIdentifier, InvitedPosition, Funding, GroupIdRecord, Membership, OtherName, PeerReview, Qualification, ResearcherUrl, Service, Work, WorkBulk, Address, Keyword, ResearchResource> {

    // Managers that goes to the primary database
    @Resource(name = "workManagerV3")
    private WorkManager workManager;

    @Resource(name = "profileFundingManagerV3")
    private ProfileFundingManager profileFundingManager;

    @Resource(name = "profileEntityManagerV3")
    private ProfileEntityManager profileEntityManager;

    @Resource(name = "affiliationsManagerV3")
    private AffiliationsManager affiliationsManager;

    @Resource(name = "peerReviewManagerV3")
    private PeerReviewManager peerReviewManager;

    @Resource(name = "orcidSecurityManagerV3")
    private OrcidSecurityManager orcidSecurityManager;

    @Resource(name = "groupIdRecordManagerV3")
    private GroupIdRecordManager groupIdRecordManager;

    @Resource
    private LocaleManager localeManager;

    @Resource(name = "researcherUrlManagerV3")
    private ResearcherUrlManager researcherUrlManager;

    @Resource(name = "otherNameManagerV3")
    private OtherNameManager otherNameManager;

    @Resource(name = "externalIdentifierManagerV3")
    private ExternalIdentifierManager externalIdentifierManager;

    @Resource(name = "profileKeywordManagerV3")
    private ProfileKeywordManager profileKeywordManager;

    @Resource(name = "addressManagerV3")
    private AddressManager addressManager;

    @Resource(name = "sourceUtilsV3")
    private SourceUtils sourceUtils;

    @Resource(name = "contributorUtilsV3")
    private ContributorUtils contributorUtils;

    @Resource(name = "orcidSearchManagerV3")
    private OrcidSearchManager orcidSearchManager;
    
    @Resource
    private OrcidCoreExceptionMapper orcidCoreExceptionMapper;

    // Managers that goes to the replication database
    // Activities managers
    @Resource(name = "workManagerReadOnlyV3")
    private WorkManagerReadOnly workManagerReadOnly;

    @Resource(name = "profileFundingManagerReadOnlyV3")
    private ProfileFundingManagerReadOnly profileFundingManagerReadOnly;

    @Resource(name = "affiliationsManagerReadOnlyV3")
    private AffiliationsManagerReadOnly affiliationsManagerReadOnly;

    @Resource(name = "peerReviewManagerReadOnlyV3")
    private PeerReviewManagerReadOnly peerReviewManagerReadOnly;

    @Resource(name = "activitiesSummaryManagerReadOnlyV3")
    private ActivitiesSummaryManagerReadOnly activitiesSummaryManagerReadOnly;
    
    @Resource(name = "researchResourceManagerV3")
    private ResearchResourceManager researchResourceManager;

    @Resource(name = "researchResourceManagerReadOnlyV3")
    private ResearchResourceManagerReadOnly researchResourceManagerReadOnly;

    // Person managers
    @Resource(name = "researcherUrlManagerReadOnlyV3")
    private ResearcherUrlManagerReadOnly researcherUrlManagerReadOnly;

    @Resource(name = "otherNameManagerReadOnlyV3")
    private OtherNameManagerReadOnly otherNameManagerReadOnly;

    @Resource(name = "emailManagerReadOnlyV3")
    private EmailManagerReadOnly emailManagerReadOnly;

    @Resource(name = "externalIdentifierManagerReadOnlyV3")
    private ExternalIdentifierManagerReadOnly externalIdentifierManagerReadOnly;

    @Resource(name = "personalDetailsManagerReadOnlyV3")
    private PersonalDetailsManagerReadOnly personalDetailsManagerReadOnly;

    @Resource(name = "profileKeywordManagerReadOnlyV3")
    private ProfileKeywordManagerReadOnly profileKeywordManagerReadOnly;

    @Resource(name = "addressManagerReadOnlyV3")
    private AddressManagerReadOnly addressManagerReadOnly;

    @Resource(name = "biographyManagerReadOnlyV3")
    private BiographyManagerReadOnly biographyManagerReadOnly;

    @Resource(name = "personDetailsManagerReadOnlyV3")
    private PersonDetailsManagerReadOnly personDetailsManagerReadOnly;

    // Record manager
    @Resource(name = "recordManagerReadOnlyV3")
    private RecordManagerReadOnly recordManagerReadOnly;

    // Other managers
    @Resource(name = "groupIdRecordManagerReadOnlyV3")
    private GroupIdRecordManagerReadOnly groupIdRecordManagerReadOnly;

    @Resource
    private ClientDetailsManagerReadOnly clientDetailsManagerReadOnly;

    @Resource(name = "clientManagerReadOnlyV3")
    private ClientManagerReadOnly clientManagerReadOnly;

    @Resource
    private MessageSource messageSource;
    
    @Resource
    private StatusManager statusManager;
    
    @Resource(name = "sourceManagerV3")
    private SourceManager sourceManager;

    private Boolean filterVersionOfIdentifiers = false;
    
    @Resource
    private ApiUtils apiUtils;
    
    @Resource
    private OrcidUrlManager orcidUrlManager;
    
    @Resource
    private SummaryManager summaryManager;

    @Resource
    private EmailDomainManager emailDomainManager;

    @Resource
    private SourceEntityUtils sourceEntityUtils;

    public Boolean getFilterVersionOfIdentifiers() {
        return filterVersionOfIdentifiers;
    }

    public void setFilterVersionOfIdentifiers(Boolean filterVersionOfIdentifiers) {
        this.filterVersionOfIdentifiers = filterVersionOfIdentifiers;
    }

    @Override
    public Response viewStatusSimple() {
        Map<String, Boolean> statusMap = statusManager.createStatusMapSimple();
        return Response.ok(statusMap).build();
    }
    
    @Override
    public Response viewStatus() {
        Map<String, Boolean> statusMap = statusManager.createStatusMap();
        Status responseStatus = statusMap.get(StatusManager.OVERALL_OK) ? Status.OK : Status.INTERNAL_SERVER_ERROR;
        return Response.status(responseStatus).entity(statusMap).build();
    }

    @Override
    public Response viewRecord(String orcid) {
        checkProfileStatus(orcid, true);
        Record record = recordManagerReadOnly.getRecord(orcid, filterVersionOfIdentifiers);
        orcidSecurityManager.checkAndFilter(orcid, record);
        if (record.getPerson() != null) {
            sourceUtils.setSourceName(record.getPerson());
        }
        if (record.getActivitiesSummary() != null) {
            ActivityUtils.cleanEmptyFields(record.getActivitiesSummary());
            sourceUtils.setSourceName(record.getActivitiesSummary());
        }
        ElementUtils.setPathToRecord(record, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(record);
        return Response.ok(record).build();
    }

    @Override
    public Response viewActivities(String orcid) {
        checkProfileStatus(orcid, true);
        ActivitiesSummary as = activitiesSummaryManagerReadOnly.getActivitiesSummary(orcid, filterVersionOfIdentifiers);
        orcidSecurityManager.checkAndFilter(orcid, as);
        ActivityUtils.cleanEmptyFields(as);
        ActivityUtils.setPathToActivity(as, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(as);
        sourceUtils.setSourceName(as);
        return Response.ok(as).build();
    }

    @Override
    public Response viewWork(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Work w = workManagerReadOnly.getWork(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, w, ScopePathType.ORCID_WORKS_READ_LIMITED);
        ActivityUtils.cleanEmptyFields(w);
        ActivityUtils.setPathToActivity(w, orcid);
        sourceUtils.setSourceName(w);
        return Response.ok(w).build();
    }

    @Override
    public Response viewWorks(String orcid) {
        checkProfileStatus(orcid, true);
        List<WorkSummary> worksList = workManagerReadOnly.getWorksSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<WorkSummary> filteredList = null;
        if (worksList != null) {
            filteredList = new ArrayList<WorkSummary>(worksList);
        }
        worksList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, worksList, ScopePathType.ORCID_WORKS_READ_LIMITED);
        // Should we filter the version-of identifiers before grouping?
        if(filterVersionOfIdentifiers) {
            for(WorkSummary w : worksList) {
                if(w.getExternalIdentifiers() != null && !w.getExternalIdentifiers().getExternalIdentifier().isEmpty()) {
                    Iterator<ExternalID> it = w.getExternalIdentifiers().getExternalIdentifier().iterator();
                    while(it.hasNext()) {
                        ExternalID extId = it.next();
                        if(Relationship.VERSION_OF.equals(extId.getRelationship())) {
                            it.remove();
                        }
                    }
                }
            }
        }
        Works works = workManager.groupWorks(worksList, false);
        Api3_0LastModifiedDatesHelper.calculateLastModified(works);
        ActivityUtils.cleanEmptyFields(works);
        ActivityUtils.setPathToWorks(works, orcid);
        sourceUtils.setSourceName(works);
        return Response.ok(works).build();
    }

    @Override
    public Response viewWorkSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        WorkSummary ws = workManagerReadOnly.getWorkSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, ws, ScopePathType.ORCID_WORKS_READ_LIMITED);
        ActivityUtils.cleanEmptyFields(ws);
        ActivityUtils.setPathToActivity(ws, orcid);
        sourceUtils.setSourceName(ws);
        return Response.ok(ws).build();
    }

    @Override
    public Response createWork(String orcid, Work work) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_WORKS_CREATE, ScopePathType.ORCID_WORKS_UPDATE);
        clearSource(work);
        Work w = workManager.createWork(orcid, work, true);
        sourceUtils.setSourceName(w);
        return apiUtils.buildApiResponse(orcid, "work", String.valueOf(w.getPutCode()), "apiError.creatework_response.exception");
    }

    @Override
    public Response updateWork(String orcid, Long putCode, Work work) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_WORKS_UPDATE);
        if (!putCode.equals(work.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, work.getPutCode()));                                     
        }
        clearSource(work);
        Work w = workManager.updateWork(orcid, work, true);
        sourceUtils.setSourceName(w);
        return Response.ok(w).build();
    }

    @Override
    public Response createWorks(String orcid, WorkBulk works) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_WORKS_CREATE, ScopePathType.ORCID_WORKS_UPDATE);
        if (works != null) {
            for (int i = 0; i < works.getBulk().size(); i++) {
                if (Work.class.isAssignableFrom(works.getBulk().get(i).getClass())) {
                    Work work = (Work) works.getBulk().get(i);
                    clearSource(work);
                }
            }
        }
        works = workManager.createWorks(orcid, works);
        sourceUtils.setSourceName(works);
        return Response.ok(works).build();
    }

    @Override
    public Response deleteWork(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_WORKS_UPDATE);
        workManager.checkSourceAndRemoveWork(orcid, putCode);
        return Response.noContent().build();
    }

    @Override
    public Response viewFunding(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Funding f = profileFundingManagerReadOnly.getFunding(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, f, ScopePathType.FUNDING_READ_LIMITED);
        ActivityUtils.setPathToActivity(f, orcid);
        ActivityUtils.cleanEmptyFields(f);
        sourceUtils.setSourceName(f);
        contributorUtils.filterContributorPrivateData(f);
        return Response.ok(f).build();
    }

    @Override
    public Response viewFundings(String orcid) {
        checkProfileStatus(orcid, true);
        List<FundingSummary> fundingSummaries = profileFundingManagerReadOnly.getFundingSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<FundingSummary> filteredList = null;
        if (fundingSummaries != null) {
            filteredList = new ArrayList<FundingSummary>(fundingSummaries);
        }
        fundingSummaries = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, fundingSummaries, ScopePathType.FUNDING_READ_LIMITED);
        Fundings fundings = profileFundingManager.groupFundings(fundingSummaries, false);
        ActivityUtils.setPathToFundings(fundings, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(fundingSummaries);
        Api3_0LastModifiedDatesHelper.calculateLastModified(fundings);
        sourceUtils.setSourceName(fundings);
        return Response.ok(fundings).build();
    }

    @Override
    public Response viewFundingSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        FundingSummary fs = profileFundingManagerReadOnly.getSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, fs, ScopePathType.FUNDING_READ_LIMITED);
        ActivityUtils.setPathToActivity(fs, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(fs);
        sourceUtils.setSourceName(fs);
        return Response.ok(fs).build();
    }

    @Override
    public Response createFunding(String orcid, Funding funding) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.FUNDING_CREATE, ScopePathType.FUNDING_UPDATE);
        clearSource(funding);
        Funding f = profileFundingManager.createFunding(orcid, funding, true);
        sourceUtils.setSourceName(f);
        return apiUtils.buildApiResponse(orcid, "funding", String.valueOf(f.getPutCode()), "apiError.createfunding_response.exception");
    }

    @Override
    public Response updateFunding(String orcid, Long putCode, Funding funding) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.FUNDING_UPDATE);
        if (!putCode.equals(funding.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, funding.getPutCode()));                            
        }
        clearSource(funding);
        Funding f = profileFundingManager.updateFunding(orcid, funding, true);
        sourceUtils.setSourceName(f);
        return Response.ok(f).build();
    }

    @Override
    public Response deleteFunding(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.FUNDING_UPDATE);
        profileFundingManager.checkSourceAndDelete(orcid, putCode);
        return Response.noContent().build();
    }

    @Override
    public Response viewEducation(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Education e = affiliationsManagerReadOnly.getEducationAffiliation(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(e);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewEducations(String orcid) {
        checkProfileStatus(orcid, true);
        List<EducationSummary> educationsList = affiliationsManagerReadOnly.getEducationSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<EducationSummary> filteredList = null;
        if (educationsList != null) {
            filteredList = new ArrayList<EducationSummary>(educationsList);
        }
        educationsList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, educationsList, ScopePathType.AFFILIATIONS_READ_LIMITED);
        Educations educations = new Educations(affiliationsManagerReadOnly.groupAffiliations(educationsList, false));
        ActivityUtils.setPathToAffiliations(educations, orcid);
        sourceUtils.setSourceName(educations);
        Api3_0LastModifiedDatesHelper.calculateLastModified(educations);
        return Response.ok(educations).build();
    }

    @Override
    public Response viewEducationSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        EducationSummary es = affiliationsManagerReadOnly.getEducationSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, es, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(es, orcid);
        sourceUtils.setSourceName(es);
        return Response.ok(es).build();
    }

    @Override
    public Response createEducation(String orcid, Education education) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(education);
        Education e = affiliationsManager.createEducationAffiliation(orcid, education, true);
        sourceUtils.setSourceName(e);
        return apiUtils.buildApiResponse(orcid, "education", String.valueOf(e.getPutCode()), "apiError.createeducation_response.exception");
    }

    @Override
    public Response updateEducation(String orcid, Long putCode, Education education) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(education.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, education.getPutCode()));                            
        }
        clearSource(education);
        Education e = affiliationsManager.updateEducationAffiliation(orcid, education, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewEmployment(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Employment e = affiliationsManagerReadOnly.getEmploymentAffiliation(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(e);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewEmployments(String orcid) {
        checkProfileStatus(orcid, true);
        List<EmploymentSummary> employmentsList = affiliationsManagerReadOnly.getEmploymentSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<EmploymentSummary> filteredList = null;
        if (employmentsList != null) {
            filteredList = new ArrayList<EmploymentSummary>(employmentsList);
        }
        employmentsList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, employmentsList, ScopePathType.AFFILIATIONS_READ_LIMITED);
        Employments employments = new Employments(affiliationsManagerReadOnly.groupAffiliations(employmentsList, false));
        ActivityUtils.setPathToAffiliations(employments, orcid);
        sourceUtils.setSourceName(employments);
        Api3_0LastModifiedDatesHelper.calculateLastModified(employments);
        return Response.ok(employments).build();
    }

    @Override
    public Response viewEmploymentSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        EmploymentSummary es = affiliationsManagerReadOnly.getEmploymentSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, es, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(es, orcid);
        sourceUtils.setSourceName(es);
        return Response.ok(es).build();
    }

    @Override
    public Response createEmployment(String orcid, Employment employment) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(employment);
        Employment e = affiliationsManager.createEmploymentAffiliation(orcid, employment, true);
        sourceUtils.setSourceName(e);
        return apiUtils.buildApiResponse(orcid, "employment", String.valueOf(e.getPutCode()), "apiError.createemployment_response.exception");
    }

    @Override
    public Response updateEmployment(String orcid, Long putCode, Employment employment) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(employment.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, employment.getPutCode()));                            
        }
        clearSource(employment);
        Employment e = affiliationsManager.updateEmploymentAffiliation(orcid, employment, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response deleteAffiliation(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        affiliationsManager.checkSourceAndDelete(orcid, putCode);
        return Response.noContent().build();
    }

    @Override
    public Response viewPeerReview(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        PeerReview p = peerReviewManagerReadOnly.getPeerReview(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, p, ScopePathType.PEER_REVIEW_READ_LIMITED);
        ActivityUtils.setPathToActivity(p, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(p);
        sourceUtils.setSourceName(p);
        return Response.ok(p).build();
    }

    @Override
    public Response viewPeerReviews(String orcid) {
        checkProfileStatus(orcid, true);
        List<PeerReviewSummary> peerReviewList = peerReviewManagerReadOnly.getPeerReviewSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<PeerReviewSummary> filteredList = null;
        if (peerReviewList != null) {
            filteredList = new ArrayList<PeerReviewSummary>(peerReviewList);
        }
        peerReviewList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, peerReviewList, ScopePathType.PEER_REVIEW_READ_LIMITED);
        PeerReviews peerReviews = peerReviewManager.groupPeerReviews(peerReviewList, false);
        ActivityUtils.setPathToPeerReviews(peerReviews, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(peerReviewList);
        Api3_0LastModifiedDatesHelper.calculateLastModified(peerReviews);
        sourceUtils.setSourceName(peerReviews);
        return Response.ok(peerReviews).build();
    }

    @Override
    public Response viewPeerReviewSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        PeerReviewSummary ps = peerReviewManagerReadOnly.getPeerReviewSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, ps, ScopePathType.PEER_REVIEW_READ_LIMITED);
        ActivityUtils.setPathToActivity(ps, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(ps);
        sourceUtils.setSourceName(ps);
        return Response.ok(ps).build();
    }

    @Override
    public Response createPeerReview(String orcid, PeerReview peerReview) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.PEER_REVIEW_CREATE, ScopePathType.PEER_REVIEW_UPDATE);
        clearSource(peerReview);

        PeerReview newPeerReview = peerReviewManager.createPeerReview(orcid, peerReview, true);
        sourceUtils.setSourceName(newPeerReview);
        return apiUtils.buildApiResponse(orcid, "peer-review", String.valueOf(newPeerReview.getPutCode()), "apiError.createpeerreview_response.exception");
    }

    @Override
    public Response updatePeerReview(String orcid, Long putCode, PeerReview peerReview) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.PEER_REVIEW_UPDATE);
        if (!putCode.equals(peerReview.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, peerReview.getPutCode()));                            
        }
        clearSource(peerReview);
        
        PeerReview updatedPeerReview = peerReviewManager.updatePeerReview(orcid, peerReview, true);
        sourceUtils.setSourceName(updatedPeerReview);
        return Response.ok(updatedPeerReview).build();
    }

    @Override
    public Response deletePeerReview(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.PEER_REVIEW_UPDATE);
        peerReviewManager.checkSourceAndDelete(orcid, putCode);
        return Response.noContent().build();
    }

    @Override
    public Response viewGroupIdRecord(Long putCode) {
        orcidSecurityManager.checkScopes(ScopePathType.GROUP_ID_RECORD_READ);
        GroupIdRecord record = groupIdRecordManagerReadOnly.getGroupIdRecord(putCode);
        return Response.ok(record).build();
    }

    @Override
    public Response createGroupIdRecord(GroupIdRecord groupIdRecord) {
        orcidSecurityManager.checkScopes(ScopePathType.GROUP_ID_RECORD_UPDATE);
        
        if (!groupIdRecordManager.exists(groupIdRecord.getGroupId()) && IssnGroupIdPatternMatcher.isIssnGroupType(groupIdRecord.getGroupId())) {
            // issn group type
            groupIdRecordManager.createOrcidSourceIssnGroupIdRecord(groupIdRecord.getGroupId(), IssnGroupIdPatternMatcher.getIssnFromIssnGroupId(groupIdRecord.getGroupId()));
            throw new DuplicatedGroupIdRecordException();
        }
        // filter out invisible control characters such as \u0098
        groupIdRecord.setName(groupIdRecord.getName().replaceAll("\\p{C}", ""));
        GroupIdRecord newRecord = groupIdRecordManager.createGroupIdRecord(groupIdRecord);
        return apiUtils.buildApiResponse(null, "group-id-record", String.valueOf(newRecord.getPutCode()), "apiError.creategroupidrecord_response.exception");
    }

    @Override
    public Response updateGroupIdRecord(GroupIdRecord groupIdRecord, Long putCode) {
        orcidSecurityManager.checkScopes(ScopePathType.GROUP_ID_RECORD_UPDATE);
        if (!putCode.equals(groupIdRecord.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, groupIdRecord.getPutCode()));                            
        }
        // filter out invisible control characters such as \u0098
        groupIdRecord.setName(groupIdRecord.getName().replaceAll("\\p{C}", ""));
        GroupIdRecord updatedRecord = groupIdRecordManager.updateGroupIdRecord(putCode, groupIdRecord);
        return Response.ok(updatedRecord).build();
    }

    @Override
    public Response deleteGroupIdRecord(Long putCode) {
        orcidSecurityManager.checkScopes(ScopePathType.GROUP_ID_RECORD_UPDATE);
        groupIdRecordManager.deleteGroupIdRecord(putCode);
        return Response.noContent().build();
    }

    @Override
    public Response viewGroupIdRecords(String pageSize, String pageNum) {
        orcidSecurityManager.checkScopes(ScopePathType.GROUP_ID_RECORD_READ);
        GroupIdRecords records = groupIdRecordManagerReadOnly.getGroupIdRecords(pageSize, pageNum);
        Api3_0LastModifiedDatesHelper.calculateLastModified(records);
        return Response.ok(records).build();
    }

    @Override
    public Response findGroupIdRecordByName(String name) {
        orcidSecurityManager.checkScopes(ScopePathType.GROUP_ID_RECORD_READ);
        Optional<GroupIdRecord> record = groupIdRecordManager.findGroupIdRecordByName(name);
        if (record.isPresent()) {
            return Response.ok(record.get()).build();
        } 
        return Response.ok(new GroupIdRecord()).build();
    }
    
    @Override
    public Response findGroupIdRecordByGroupId(String groupId) {
        orcidSecurityManager.checkScopes(ScopePathType.GROUP_ID_RECORD_READ);
        Optional<GroupIdRecord> record = groupIdRecordManager.findByGroupId(groupId);
        if (record.isPresent()) {
            return Response.ok(record.get()).build();
        } else if (IssnGroupIdPatternMatcher.isIssnGroupType(groupId)) {
            // issn group type
            return Response.ok(groupIdRecordManager.createOrcidSourceIssnGroupIdRecord(groupId, IssnGroupIdPatternMatcher.getIssnFromIssnGroupId(groupId))).build();
        }
        return Response.ok(new GroupIdRecord()).build();
    }

    /**
     * BIOGRAPHY ELEMENTS
     */
    @Override
    public Response viewResearcherUrls(String orcid) {
        checkProfileStatus(orcid, true);
        ResearcherUrls researcherUrls = researcherUrlManagerReadOnly.getResearcherUrls(orcid);

        // Lets copy the list so we don't modify the cached collection
        if (researcherUrls.getResearcherUrls() != null) {
            List<ResearcherUrl> filteredList = new ArrayList<ResearcherUrl>(researcherUrls.getResearcherUrls());
            researcherUrls = new ResearcherUrls();
            researcherUrls.setResearcherUrls(filteredList);
        }

        orcidSecurityManager.checkAndFilter(orcid, researcherUrls.getResearcherUrls(), ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToResearcherUrls(researcherUrls, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(researcherUrls);
        sourceUtils.setSourceName(researcherUrls);
        return Response.ok(researcherUrls).build();
    }

    public Response viewResearcherUrl(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        ResearcherUrl researcherUrl = researcherUrlManagerReadOnly.getResearcherUrl(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, researcherUrl, ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToResearcherUrl(researcherUrl, orcid);
        sourceUtils.setSourceName(researcherUrl);
        return Response.ok(researcherUrl).build();
    }

    @Override
    public Response updateResearcherUrl(String orcid, Long putCode, ResearcherUrl researcherUrl) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        if (!putCode.equals(researcherUrl.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, researcherUrl.getPutCode()));                            
        }
        clearSource(researcherUrl);
        ResearcherUrl updatedResearcherUrl = researcherUrlManager.updateResearcherUrl(orcid, researcherUrl, true);
        ElementUtils.setPathToResearcherUrl(updatedResearcherUrl, orcid);
        sourceUtils.setSourceName(updatedResearcherUrl);
        return Response.ok(updatedResearcherUrl).build();
    }

    @Override
    public Response createResearcherUrl(String orcid, ResearcherUrl researcherUrl) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        clearSource(researcherUrl);
        researcherUrl = researcherUrlManager.createResearcherUrl(orcid, researcherUrl, true);
        sourceUtils.setSourceName(researcherUrl);
        return apiUtils.buildApiResponse(orcid, "researcher-urls", String.valueOf(researcherUrl.getPutCode()), "apiError.createelement_response.exception");
    }

    @Override
    public Response deleteResearcherUrl(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        researcherUrlManager.deleteResearcherUrl(orcid, putCode, true);
        return Response.noContent().build();
    }

    @Override
    public Response viewEmails(String orcid) {
        checkProfileStatus(orcid, true);
        Emails emails = null;

        try {
            // return all emails if client has /email/read-private scope
            orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.EMAIL_READ_PRIVATE);
            emails = emailManagerReadOnly.getVerifiedEmails(orcid);
            
            // Lets copy the list so we don't modify the cached collection
            List<Email> filteredList = new ArrayList<Email>(emails.getEmails());
            emails = new Emails();
            emails.setEmails(filteredList);
        } catch (OrcidAccessControlException e) {
            emails = emailManagerReadOnly.getVerifiedEmails(orcid);
            
            // Lets copy the list so we don't modify the cached collection
            List<Email> filteredList = new ArrayList<Email>(emails.getEmails());
            emails = new Emails();
            emails.setEmails(filteredList);

            // Filter just in case client doesn't have the /email/read-private
            // scope
            orcidSecurityManager.checkAndFilter(orcid, emails.getEmails(), ScopePathType.ORCID_BIO_READ_LIMITED);
        }

        ElementUtils.setPathToEmail(emails, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(emails);
        sourceUtils.setSourceName(emails);
        return Response.ok(emails).build();
    }

    @Override
    public Response viewOtherNames(String orcid) {
        checkProfileStatus(orcid, true);
        OtherNames otherNames = otherNameManagerReadOnly.getOtherNames(orcid);

        // Lets copy the list so we don't modify the cached collection
        if (otherNames.getOtherNames() != null) {
            List<OtherName> filteredList = new ArrayList<OtherName>(otherNames.getOtherNames());
            otherNames = new OtherNames();
            otherNames.setOtherNames(filteredList);
        }

        orcidSecurityManager.checkAndFilter(orcid, otherNames.getOtherNames(), ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToOtherNames(otherNames, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(otherNames);
        sourceUtils.setSourceName(otherNames);
        return Response.ok(otherNames).build();
    }

    @Override
    public Response viewOtherName(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        OtherName otherName = otherNameManagerReadOnly.getOtherName(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, otherName, ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToOtherName(otherName, orcid);
        sourceUtils.setSourceName(otherName);
        return Response.ok(otherName).build();
    }

    @Override
    public Response createOtherName(String orcid, OtherName otherName) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        clearSource(otherName);
        otherName = otherNameManager.createOtherName(orcid, otherName, true);
        sourceUtils.setSourceName(otherName);
        return apiUtils.buildApiResponse(orcid, "other-names", String.valueOf(otherName.getPutCode()), "apiError.createelement_response.exception");
    }

    @Override
    public Response updateOtherName(String orcid, Long putCode, OtherName otherName) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        if (!putCode.equals(otherName.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, otherName.getPutCode()));                            
        }
        clearSource(otherName);
        OtherName updatedOtherName = otherNameManager.updateOtherName(orcid, putCode, otherName, true);
        ElementUtils.setPathToOtherName(updatedOtherName, orcid);
        sourceUtils.setSourceName(updatedOtherName);
        return Response.ok(updatedOtherName).build();
    }

    @Override
    public Response deleteOtherName(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        otherNameManager.deleteOtherName(orcid, putCode, true);
        return Response.noContent().build();
    }

    @Override
    public Response viewExternalIdentifiers(String orcid) {
        checkProfileStatus(orcid, true);
        PersonExternalIdentifiers extIds = externalIdentifierManagerReadOnly.getExternalIdentifiers(orcid);

        // Lets copy the list so we don't modify the cached collection
        if (extIds.getExternalIdentifiers() != null) {
            List<PersonExternalIdentifier> filteredList = new ArrayList<PersonExternalIdentifier>(extIds.getExternalIdentifiers());
            extIds = new PersonExternalIdentifiers();
            extIds.setExternalIdentifiers(filteredList);
        }

        orcidSecurityManager.checkAndFilter(orcid, extIds.getExternalIdentifiers(), ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToExternalIdentifiers(extIds, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(extIds);
        sourceUtils.setSourceName(extIds);
        return Response.ok(extIds).build();
    }

    @Override
    public Response viewExternalIdentifier(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        PersonExternalIdentifier extId = externalIdentifierManagerReadOnly.getExternalIdentifier(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, extId, ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToExternalIdentifier(extId, orcid);
        sourceUtils.setSourceName(extId);
        return Response.ok(extId).build();
    }

    @Override
    public Response updateExternalIdentifier(String orcid, Long putCode, PersonExternalIdentifier externalIdentifier) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_EXTERNAL_IDENTIFIERS_CREATE);
        if (!putCode.equals(externalIdentifier.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, externalIdentifier.getPutCode()));                            
        }
        clearSource(externalIdentifier);
        PersonExternalIdentifier extId = externalIdentifierManager.updateExternalIdentifier(orcid, externalIdentifier, true);
        ElementUtils.setPathToExternalIdentifier(extId, orcid);
        sourceUtils.setSourceName(extId);
        return Response.ok(extId).build();
    }

    @Override
    public Response createExternalIdentifier(String orcid, PersonExternalIdentifier externalIdentifier) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_EXTERNAL_IDENTIFIERS_CREATE);
        clearSource(externalIdentifier);
        externalIdentifier = externalIdentifierManager.createExternalIdentifier(orcid, externalIdentifier, true);
        return apiUtils.buildApiResponse(orcid, "external-identifiers", String.valueOf(externalIdentifier.getPutCode()), "apiError.createelement_response.exception");
    }

    @Override
    public Response deleteExternalIdentifier(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        externalIdentifierManager.deleteExternalIdentifier(orcid, putCode, true);
        return Response.noContent().build();
    }

    @Override
    public Response viewKeywords(String orcid) {
        checkProfileStatus(orcid, true);
        Keywords keywords = profileKeywordManagerReadOnly.getKeywords(orcid);

        // Lets copy the list so we don't modify the cached collection
        if (keywords.getKeywords() != null) {
            List<Keyword> filteredList = new ArrayList<Keyword>(keywords.getKeywords());
            keywords = new Keywords();
            keywords.setKeywords(filteredList);
        }

        orcidSecurityManager.checkAndFilter(orcid, keywords.getKeywords(), ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToKeywords(keywords, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(keywords);
        sourceUtils.setSourceName(keywords);
        return Response.ok(keywords).build();
    }

    @Override
    public Response viewKeyword(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Keyword keyword = profileKeywordManagerReadOnly.getKeyword(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, keyword, ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToKeyword(keyword, orcid);
        sourceUtils.setSourceName(keyword);
        return Response.ok(keyword).build();
    }

    @Override
    public Response createKeyword(String orcid, Keyword keyword) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        clearSource(keyword);
        keyword = profileKeywordManager.createKeyword(orcid, keyword, true);
        sourceUtils.setSourceName(keyword);
        return apiUtils.buildApiResponse(orcid, "keywords", String.valueOf(keyword.getPutCode()), "apiError.createelement_response.exception");
    }

    @Override
    public Response updateKeyword(String orcid, Long putCode, Keyword keyword) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        if (!putCode.equals(keyword.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, keyword.getPutCode()));                            
        }
        clearSource(keyword);
        keyword = profileKeywordManager.updateKeyword(orcid, putCode, keyword, true);
        ElementUtils.setPathToKeyword(keyword, orcid);
        sourceUtils.setSourceName(keyword);
        return Response.ok(keyword).build();
    }

    @Override
    public Response deleteKeyword(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        profileKeywordManager.deleteKeyword(orcid, putCode, true);
        return Response.noContent().build();
    }

    @Override
    public Response viewAddresses(String orcid) {
        checkProfileStatus(orcid, true);
        Addresses addresses = addressManagerReadOnly.getAddresses(orcid);

        // Lets copy the list so we don't modify the cached collection
        if (addresses.getAddress() != null) {
            List<Address> filteredAddresses = new ArrayList<Address>(addresses.getAddress());
            addresses = new Addresses();
            addresses.setAddress(filteredAddresses);
        }

        orcidSecurityManager.checkAndFilter(orcid, addresses.getAddress(), ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToAddresses(addresses, orcid);
        // Set the latest last modified
        Api3_0LastModifiedDatesHelper.calculateLastModified(addresses);
        sourceUtils.setSourceName(addresses);
        return Response.ok(addresses).build();
    }

    @Override
    public Response viewAddress(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Address address = addressManagerReadOnly.getAddress(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, address, ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToAddress(address, orcid);
        sourceUtils.setSourceName(address);
        return Response.ok(address).build();
    }

    @Override
    public Response createAddress(String orcid, Address address) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        clearSource(address);
        address = addressManager.createAddress(orcid, address, true);
        sourceUtils.setSourceName(address);
        return apiUtils.buildApiResponse(orcid, "address", String.valueOf(address.getPutCode()), "apiError.createelement_response.exception");
    }

    @Override
    public Response updateAddress(String orcid, Long putCode, Address address) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        if (!putCode.equals(address.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, address.getPutCode()));                            
        }
        clearSource(address);
        address = addressManager.updateAddress(orcid, putCode, address, true);
        ElementUtils.setPathToAddress(address, orcid);
        sourceUtils.setSourceName(address);
        return Response.ok(address).build();
    }

    @Override
    public Response deleteAddress(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.ORCID_BIO_UPDATE);
        addressManager.deleteAddress(orcid, putCode);
        return Response.noContent().build();
    }

    @Override
    public Response viewBiography(String orcid) {
        checkProfileStatus(orcid, true);
        Biography bio = biographyManagerReadOnly.getBiography(orcid);
        if (bio == null) {
            throw new OrcidNoBioException();
        }
        orcidSecurityManager.checkAndFilter(orcid, bio, ScopePathType.ORCID_BIO_READ_LIMITED);
        ElementUtils.setPathToBiography(bio, orcid);
        return Response.ok(bio).build();
    }

    @Override
    public Response viewPersonalDetails(String orcid) {
        checkProfileStatus(orcid, true);
        PersonalDetails personalDetails = personalDetailsManagerReadOnly.getPersonalDetails(orcid);
        orcidSecurityManager.checkAndFilter(orcid, personalDetails);
        ElementUtils.setPathToPersonalDetails(personalDetails, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(personalDetails);
        sourceUtils.setSourceName(personalDetails);
        return Response.ok(personalDetails).build();
    }

    @Override
    public Response viewPerson(String orcid) {
        checkProfileStatus(orcid, true);
        Person person = personDetailsManagerReadOnly.getPersonDetails(orcid, false);
        orcidSecurityManager.checkAndFilter(orcid, person);
        ElementUtils.setPathToPerson(person, orcid);
        Api3_0LastModifiedDatesHelper.calculateLastModified(person);
        sourceUtils.setSourceName(person);
        return Response.ok(person).build();
    }

    @Override
    public Response searchByQuery(Map<String, List<String>> solrParams) {
        orcidSecurityManager.checkScopes(ScopePathType.READ_PUBLIC);
        validateSearchParams(solrParams);
        Search search = orcidSearchManager.findOrcidIds(solrParams);
        return Response.ok(search).build();
    }
    
    @Override
    public Response searchByQueryCSV(Map<String, List<String>> solrParams) {
        validateSearchParams(solrParams);
        String search = orcidSearchManager.findOrcidIdsAsCSV(solrParams);
        return Response.ok(search).build();
    }
    
    @Override
    public Response expandedSearchByQuery(Map<String, List<String>> solrParams) {
        validateSearchParams(solrParams);
        ExpandedSearch search = orcidSearchManager.expandedSearch(solrParams);
        return Response.ok(search).build();
    }

    @Override
    public Response viewBulkWorks(String orcid, String putCodes) {
        checkProfileStatus(orcid, true);
        if (!profileEntityManager.orcidExists(orcid)) {
            throw new OrcidNoResultException("No such record: " + orcid);
        }
        
        WorkBulk workBulk = workManagerReadOnly.findWorkBulk(orcid, putCodes);
        orcidSecurityManager.checkAndFilter(orcid, workBulk, ScopePathType.ORCID_WORKS_READ_LIMITED);
        ActivityUtils.cleanEmptyFields(workBulk);
        ActivityUtils.setPathToBulk(workBulk, orcid);
        sourceUtils.setSourceName(workBulk);
        return Response.ok(workBulk).build();
    }

    private void validateSearchParams(Map<String, List<String>> queryMap) {
        List<String> rowsList = queryMap.get("rows");
        if (rowsList != null && !rowsList.isEmpty()) {
            try {
                String rowsString = rowsList.get(0);
                int rows = Integer.valueOf(rowsString);
                if (rows < 0 || rows > OrcidSearchManager.MAX_SEARCH_ROWS) {
                    throw new OrcidBadRequestException(
                            localeManager.resolveMessage("apiError.badrequest_invalid_search_rows.exception", OrcidSearchManager.MAX_SEARCH_ROWS));
                }
            } catch (NumberFormatException e) {
                throw new OrcidBadRequestException(localeManager.resolveMessage("apiError.badrequest_invalid_search_rows.exception"));
            }
        } else {
            // Set the default number of results
            queryMap.put("rows", Arrays.asList(String.valueOf(OrcidSearchManager.DEFAULT_SEARCH_ROWS)));
        }
    }

    @Override
    public Response viewClient(String clientId) {
        orcidSecurityManager.checkScopes(ScopePathType.READ_PUBLIC);
        ClientSummary client = clientManagerReadOnly.getSummary(clientId);
        return Response.ok(client).build();
    }
    
    @Override
    public Response viewDistinction(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Distinction e = affiliationsManagerReadOnly.getDistinctionAffiliation(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(e);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewDistinctions(String orcid) {
        checkProfileStatus(orcid, true);
        List<DistinctionSummary> distinctionsList = affiliationsManagerReadOnly.getDistinctionSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<DistinctionSummary> filteredList = null;
        if (distinctionsList != null) {
            filteredList = new ArrayList<DistinctionSummary>(distinctionsList);
        }
        distinctionsList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, distinctionsList, ScopePathType.AFFILIATIONS_READ_LIMITED);
        Distinctions distinctions = new Distinctions(affiliationsManagerReadOnly.groupAffiliations(distinctionsList, false));
        ActivityUtils.setPathToAffiliations(distinctions, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(distinctionsList);
        sourceUtils.setSourceName(distinctions);
        Api3_0LastModifiedDatesHelper.calculateLastModified(distinctions);
        return Response.ok(distinctions).build();
    }

    @Override
    public Response viewDistinctionSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        DistinctionSummary es = affiliationsManagerReadOnly.getDistinctionSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, es, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(es, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(es);
        sourceUtils.setSourceName(es);
        return Response.ok(es).build();
    }

    @Override
    public Response createDistinction(String orcid, Distinction distinction) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(distinction);
        Distinction e = affiliationsManager.createDistinctionAffiliation(orcid, distinction, true);
        sourceUtils.setSourceName(e);
        return apiUtils.buildApiResponse(orcid, "distinction", String.valueOf(e.getPutCode()), "apiError.createdistinction_response.exception");
    }

    @Override
    public Response updateDistinction(String orcid, Long putCode, Distinction distinction) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(distinction.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, distinction.getPutCode()));                            
        }
        clearSource(distinction);
        Distinction e = affiliationsManager.updateDistinctionAffiliation(orcid, distinction, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewInvitedPosition(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        InvitedPosition e = affiliationsManagerReadOnly.getInvitedPositionAffiliation(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(e);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewInvitedPositions(String orcid) {
        checkProfileStatus(orcid, true);
        List<InvitedPositionSummary> invitedPositionsList = affiliationsManagerReadOnly.getInvitedPositionSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<InvitedPositionSummary> filteredList = null;
        if (invitedPositionsList != null) {
            filteredList = new ArrayList<InvitedPositionSummary>(invitedPositionsList);
        }
        invitedPositionsList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, invitedPositionsList, ScopePathType.AFFILIATIONS_READ_LIMITED);
        InvitedPositions inivitedPositions = new InvitedPositions(affiliationsManagerReadOnly.groupAffiliations(invitedPositionsList, false));
        ActivityUtils.setPathToAffiliations(inivitedPositions, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(invitedPositionsList);
        sourceUtils.setSourceName(inivitedPositions);
        Api3_0LastModifiedDatesHelper.calculateLastModified(inivitedPositions);
        return Response.ok(inivitedPositions).build();
    }

    @Override
    public Response viewInvitedPositionSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        InvitedPositionSummary es = affiliationsManagerReadOnly.getInvitedPositionSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, es, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(es, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(es);
        sourceUtils.setSourceName(es);
        return Response.ok(es).build();
    }

    @Override
    public Response createInvitedPosition(String orcid, InvitedPosition invitedPosition) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(invitedPosition);
        InvitedPosition e = affiliationsManager.createInvitedPositionAffiliation(orcid, invitedPosition, true);
        sourceUtils.setSourceName(e);
        return apiUtils.buildApiResponse(orcid, "invited-position", String.valueOf(e.getPutCode()), "apiError.createdistinction_response.exception");
    }

    @Override
    public Response updateInvitedPosition(String orcid, Long putCode, InvitedPosition invitedPosition) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(invitedPosition.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, invitedPosition.getPutCode()));                 
        }
        clearSource(invitedPosition);
        InvitedPosition e = affiliationsManager.updateInvitedPositionAffiliation(orcid, invitedPosition, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewMembership(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Membership e = affiliationsManagerReadOnly.getMembershipAffiliation(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(e);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewMemberships(String orcid) {
        checkProfileStatus(orcid, true);
        List<MembershipSummary> membershipsList = affiliationsManagerReadOnly.getMembershipSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<MembershipSummary> filteredList = null;
        if (membershipsList != null) {
            filteredList = new ArrayList<MembershipSummary>(membershipsList);
        }
        membershipsList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, membershipsList, ScopePathType.AFFILIATIONS_READ_LIMITED);
        Memberships memberships = new Memberships(affiliationsManagerReadOnly.groupAffiliations(membershipsList, false));
        ActivityUtils.setPathToAffiliations(memberships, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(membershipsList);
        sourceUtils.setSourceName(memberships);
        Api3_0LastModifiedDatesHelper.calculateLastModified(memberships);
        return Response.ok(memberships).build();
    }

    @Override
    public Response viewMembershipSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        MembershipSummary es = affiliationsManagerReadOnly.getMembershipSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, es, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(es, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(es);
        sourceUtils.setSourceName(es);
        return Response.ok(es).build();
    }

    @Override
    public Response createMembership(String orcid, Membership membership) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(membership);
        Membership e = affiliationsManager.createMembershipAffiliation(orcid, membership, true);
        sourceUtils.setSourceName(e);
        return apiUtils.buildApiResponse(orcid, "membership", String.valueOf(e.getPutCode()), "apiError.createdistinction_response.exception");
    }

    @Override
    public Response updateMembership(String orcid, Long putCode, Membership membership) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(membership.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, membership.getPutCode()));
        }
        clearSource(membership);
        Membership e = affiliationsManager.updateMembershipAffiliation(orcid, membership, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewQualification(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Qualification e = affiliationsManagerReadOnly.getQualificationAffiliation(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(e);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewQualifications(String orcid) {
        checkProfileStatus(orcid, true);
        List<QualificationSummary> qualificationsList = affiliationsManagerReadOnly.getQualificationSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<QualificationSummary> filteredList = null;
        if (qualificationsList != null) {
            filteredList = new ArrayList<QualificationSummary>(qualificationsList);
        }
        qualificationsList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, qualificationsList, ScopePathType.AFFILIATIONS_READ_LIMITED);
        Qualifications qualifications = new Qualifications(affiliationsManagerReadOnly.groupAffiliations(qualificationsList, false));
        ActivityUtils.setPathToAffiliations(qualifications, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(qualificationsList);
        sourceUtils.setSourceName(qualifications);
        Api3_0LastModifiedDatesHelper.calculateLastModified(qualifications);
        return Response.ok(qualifications).build();
    }

    @Override
    public Response viewQualificationSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        QualificationSummary es = affiliationsManagerReadOnly.getQualificationSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, es, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(es, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(es);
        sourceUtils.setSourceName(es);
        return Response.ok(es).build();
    }

    @Override
    public Response createQualification(String orcid, Qualification qualification) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(qualification);
        Qualification e = affiliationsManager.createQualificationAffiliation(orcid, qualification, true);
        sourceUtils.setSourceName(e);
        return apiUtils.buildApiResponse(orcid, "qualification", String.valueOf(e.getPutCode()), "apiError.createdistinction_response.exception");
    }

    @Override
    public Response updateQualification(String orcid, Long putCode, Qualification qualification) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(qualification.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, qualification.getPutCode()));                 
        }
        clearSource(qualification);
        Qualification e = affiliationsManager.updateQualificationAffiliation(orcid, qualification, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewService(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        Service e = affiliationsManagerReadOnly.getServiceAffiliation(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(e);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewServices(String orcid) {
        checkProfileStatus(orcid, true);
        List<ServiceSummary> servicesList = affiliationsManagerReadOnly.getServiceSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<ServiceSummary> filteredList = null;
        if (servicesList != null) {
            filteredList = new ArrayList<ServiceSummary>(servicesList);
        }
        servicesList = filteredList;

        orcidSecurityManager.checkAndFilter(orcid, servicesList, ScopePathType.AFFILIATIONS_READ_LIMITED);
        Services services = new Services(affiliationsManagerReadOnly.groupAffiliations(servicesList, false));
        ActivityUtils.setPathToAffiliations(services, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(servicesList);
        sourceUtils.setSourceName(services);
        Api3_0LastModifiedDatesHelper.calculateLastModified(services);
        return Response.ok(services).build();
    }

    @Override
    public Response viewServiceSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        ServiceSummary es = affiliationsManagerReadOnly.getServiceSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, es, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(es, orcid);
        ActivityUtils.cleanOrganizationEmptyFields(es);
        sourceUtils.setSourceName(es);
        return Response.ok(es).build();
    }

    @Override
    public Response createService(String orcid, Service service) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(service);
        Service e = affiliationsManager.createServiceAffiliation(orcid, service, true);
        sourceUtils.setSourceName(e);
        return apiUtils.buildApiResponse(orcid, "service", String.valueOf(e.getPutCode()), "apiError.createdistinction_response.exception");
    }

    @Override
    public Response updateService(String orcid, Long putCode, Service service) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(service.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, service.getPutCode()));      
        }
        clearSource(service);
        Service e = affiliationsManager.updateServiceAffiliation(orcid, service, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    private void clearSource(SourceAware element) {
        element.setSource(null);
    }
    
    @Override
    public Response viewResearchResource(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        ResearchResource e = researchResourceManagerReadOnly.getResearchResource(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, e, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(e, orcid);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response viewResearchResources(String orcid) {
        checkProfileStatus(orcid, true);
        List<ResearchResourceSummary> list = researchResourceManagerReadOnly.getResearchResourceSummaryList(orcid);

        // Lets copy the list so we don't modify the cached collection
        List<ResearchResourceSummary> filteredList = null;
        if (list != null) {
            filteredList = new ArrayList<ResearchResourceSummary>(list);
        }
        list = filteredList;

        // Should we filter the version-of identifiers before grouping?
        if(filterVersionOfIdentifiers) {
            for(ResearchResourceSummary rr : list) {
                if(rr.getExternalIdentifiers() != null && !rr.getExternalIdentifiers().getExternalIdentifier().isEmpty()) {
                    Iterator<ExternalID> it = rr.getExternalIdentifiers().getExternalIdentifier().iterator();
                    while(it.hasNext()) {
                        ExternalID extId = it.next();
                        if(Relationship.VERSION_OF.equals(extId.getRelationship())) {
                            it.remove();
                        }
                    }
                }
            }
        }        
        
        orcidSecurityManager.checkAndFilter(orcid, list, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ResearchResources rr = researchResourceManagerReadOnly.groupResearchResources(list, false);
        ActivityUtils.setPathToResearchResources(rr, orcid);
        sourceUtils.setSourceName(rr);
        Api3_0LastModifiedDatesHelper.calculateLastModified(rr);
        return Response.ok(rr).build();
    }

    @Override
    public Response viewResearchResourceSummary(String orcid, Long putCode) {
        checkProfileStatus(orcid, true);
        ResearchResourceSummary r = researchResourceManagerReadOnly.getResearchResourceSummary(orcid, putCode);
        orcidSecurityManager.checkAndFilter(orcid, r, ScopePathType.AFFILIATIONS_READ_LIMITED);
        ActivityUtils.setPathToActivity(r, orcid);
        sourceUtils.setSourceName(r);
        return Response.ok(r).build();
    }

    @Override
    public Response createResearchResource(String orcid, ResearchResource researchResource) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_CREATE, ScopePathType.AFFILIATIONS_UPDATE);
        clearSource(researchResource);
        ResearchResource e = researchResourceManager.createResearchResource(orcid, researchResource, true);
        sourceUtils.setSourceName(e);
        //TODO: update errors.
        return apiUtils.buildApiResponse(orcid, "research-resource", String.valueOf(e.getPutCode()), "apiError.createresearch_resource_response.exception");
    }

    @Override
    public Response updateResearchResource(String orcid, Long putCode, ResearchResource researchResource) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        if (!putCode.equals(researchResource.getPutCode())) {
            throw new MismatchedPutCodeException(addParmsMismatchedPutCode(putCode, researchResource.getPutCode()));
        }
        clearSource(researchResource);
        ResearchResource e = researchResourceManager.updateResearchResource(orcid, researchResource, true);
        sourceUtils.setSourceName(e);
        return Response.ok(e).build();
    }

    @Override
    public Response deleteResearchResource(String orcid, Long putCode) {
        checkProfileStatus(orcid, false);
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.AFFILIATIONS_UPDATE);
        researchResourceManager.checkSourceAndRemoveResearchResource(orcid, putCode);
        return Response.noContent().build();
    }

    private void checkProfileStatus(String orcid, boolean readOperation) throws DeactivatedException {
        orcidSecurityManager.checkProfile(orcid);        
    } 
    
    private Map<String, String> addParmsMismatchedPutCode(Long urlPutCode, Long bodyPutCode) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("urlPutCode", String.valueOf(urlPutCode));
        params.put("bodyPutCode", String.valueOf(bodyPutCode));
        params.put("clientName", SourceEntityUtils.getSourceName(sourceManager.retrieveActiveSource()));
        return params;
    }

    @Override
    public Response getRecordSummary(String orcid) {
        orcidSecurityManager.checkClientAccessAndScopes(orcid, ScopePathType.READ_PUBLIC);
        checkProfileStatus(orcid, false);
        RecordSummary summary = summaryManager.getRecordSummary(orcid);
        return Response.ok(summary).build();
    }

}
