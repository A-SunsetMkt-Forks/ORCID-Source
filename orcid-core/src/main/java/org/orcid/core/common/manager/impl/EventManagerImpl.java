package org.orcid.core.common.manager.impl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.orcid.core.common.manager.EventManager;
import org.orcid.core.constants.OrcidOauth2Constants;
import org.orcid.core.manager.ClientDetailsEntityCacheManager;
import org.orcid.core.manager.v3.read_only.RecordNameManagerReadOnly;
import org.orcid.core.utils.EventType;
import org.orcid.jaxb.model.clientgroup.ClientType;
import org.orcid.jaxb.model.v3.release.record.Name;
import org.orcid.persistence.dao.EventDao;
import org.orcid.persistence.jpa.entities.ClientDetailsEntity;
import org.orcid.persistence.jpa.entities.EventEntity;
import org.orcid.pojo.ajaxForm.PojoUtil;
import org.orcid.pojo.ajaxForm.RequestInfoForm;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 *
 * @author Daniel Palafox
 *
 */
public class EventManagerImpl implements EventManager {

    @Resource
    private EventDao eventDao;

    @Resource
    private ClientDetailsEntityCacheManager clientDetailsEntityCacheManager;

    @Resource(name = "recordNameManagerReadOnlyV3")
    private RecordNameManagerReadOnly recordNameManagerReadOnly;

    @Override
    public boolean removeEvents(String orcid) {
        return eventDao.removeEvents(orcid);
    }

    @Override
    public void createEvent(String orcid, EventType eventType, HttpServletRequest request) {
        String label = "Website";
        String clientId = null;
        String redirectUrl = null;
        String publicPage = null;

        if (eventType == EventType.PUBLIC_PAGE) {
            publicPage = orcid;
            orcid = null;
        } else {
            if (request != null) {
                Boolean isOauth2ScreensRequest = (Boolean) request.getSession().getAttribute(OrcidOauth2Constants.OAUTH_2SCREENS);
                RequestInfoForm requestInfoForm = (RequestInfoForm) request.getSession().getAttribute("requestInfoForm");
                if (requestInfoForm != null) {
                    clientId = requestInfoForm.getClientId();
                    redirectUrl = removeAttributesFromUrl(requestInfoForm.getRedirectUrl());
                    label = "OAuth " + requestInfoForm.getMemberName() + " " + requestInfoForm.getClientName();
                } else if (isOauth2ScreensRequest != null && isOauth2ScreensRequest) {
                    String queryString = (String) request.getSession().getAttribute(OrcidOauth2Constants.OAUTH_QUERY_STRING);
                    clientId = getParameterValue(queryString, "client_id");
                    redirectUrl = getParameterValue(queryString, "redirect_uri");
                    ClientDetailsEntity clientDetailsEntity = clientDetailsEntityCacheManager.retrieve(clientId);
                    String memberName = "";
                    String clientName = clientDetailsEntity.getClientName();

                    if (ClientType.PUBLIC_CLIENT.equals(clientDetailsEntity.getClientType())) {
                        memberName = "PubApp";
                    } else if (!PojoUtil.isEmpty(clientDetailsEntity.getGroupProfileId())) {
                        Name name = recordNameManagerReadOnly.getRecordName(clientDetailsEntity.getGroupProfileId());
                        if (name != null) {
                            memberName = name.getCreditName() != null ? name.getCreditName().getContent() : "";
                        }
                    }

                    if (StringUtils.isBlank(memberName)) {
                        memberName = clientName;
                    }
                    label = "OAuth " + memberName + " " + clientName;
                }
            }
        }

        EventEntity eventEntity = new EventEntity();

        eventEntity.setOrcid(orcid);
        eventEntity.setEventType(eventType.getValue());
        eventEntity.setClientId(clientId);
        eventEntity.setRedirectUrl(redirectUrl);
        eventEntity.setLabel(label);
        eventEntity.setPublicPage(publicPage);

        eventDao.createEvent(eventEntity);
    }

    private String getParameterValue(String queryString, String parameter)  {
        if (StringUtils.isNotEmpty(queryString)) {
            try {
                queryString = URLDecoder.decode(queryString, StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException u) {
                // l
            }
            String[] parameters = queryString.split("&");
            for (String p : parameters) {
                String[] keyValuePair = p.split("=");
                if (parameter.equals(keyValuePair[0])) {
                    return keyValuePair[1];
                }
            }
        }
        return null;
    }

    private String removeAttributesFromUrl(String url) {
        if (url.contains("?")) {
            return url.substring(0, url.indexOf("?"));
        }
        return url;
    }
}