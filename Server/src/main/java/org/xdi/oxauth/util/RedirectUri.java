package org.xdi.oxauth.util;

import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.common.ResponseMode;
import org.xdi.oxauth.model.common.ResponseType;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * @author Javier Rojas Blum Date: 02.27.2014
 */
public class RedirectUri {

    private String baseRedirectUri;
    private List<ResponseType> responseTypes;
    private ResponseMode responseMode;
    private Map<String, String> responseParameters;

    public RedirectUri(String baseRedirectUri, List<ResponseType> responseTypes, ResponseMode responseMode) {
        this.baseRedirectUri = baseRedirectUri;
        this.responseTypes = responseTypes;
        this.responseMode = responseMode;

        responseParameters = new HashMap<String, String>();
    }

    public String getBaseRedirectUri() {
        return baseRedirectUri;
    }

    public void setBaseRedirectUri(String baseRedirectUri) {
        this.baseRedirectUri = baseRedirectUri;
        this.responseMode = ResponseMode.QUERY;
    }

    public void addResponseParameter(String key, String value) {
        if (StringUtils.isNotBlank(key)) {
            responseParameters.put(key, value);
        }
    }

    public void parseQueryString(String queryString) {
        if (queryString != null) {
            StringTokenizer st = new StringTokenizer(queryString, "&", false);
            while (st.hasMoreElements()) {
                String nameValueToken = st.nextElement().toString();

                StringTokenizer stParamValue = new StringTokenizer(nameValueToken, "=", false);

                if (stParamValue.countTokens() == 1) {
                    String paramName = stParamValue.nextElement().toString();
                    responseParameters.put(paramName, null);
                } else if (stParamValue.countTokens() == 2) {
                    try {
                        String paramName = stParamValue.nextElement().toString();
                        String paramValue = URLDecoder.decode(stParamValue.nextElement().toString(), Util.UTF8_STRING_ENCODING);
                        responseParameters.put(paramName, paramValue);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public String getQueryString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : responseParameters.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            try {
                sb.append(URLEncoder.encode(entry.getKey(), Util.UTF8_STRING_ENCODING));
                if (entry.getValue() != null) {
                    sb.append('=').append(URLEncoder.encode(entry.getValue(), Util.UTF8_STRING_ENCODING));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }

    private void appendQuerySymbol(StringBuilder sb) {
        if (!sb.toString().contains("?")) {
            sb.append("?");
        } else {
            sb.append("&");
        }
    }

    private void appendFragmentSymbol(StringBuilder sb) {
        if (!sb.toString().contains("#")) {
            sb.append("#");
        } else {
            sb.append("&");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(baseRedirectUri);

        if (responseMode != null) {
            if (responseMode == ResponseMode.QUERY) {
                appendQuerySymbol(sb);
            } else if (responseMode == ResponseMode.FRAGMENT) {
                appendFragmentSymbol(sb);
            }
        } else if (responseTypes.contains(ResponseType.TOKEN) || responseTypes.contains(ResponseType.ID_TOKEN)) {
            appendFragmentSymbol(sb);
        } else {
            appendQuerySymbol(sb);
        }

        sb.append(getQueryString());

        return sb.toString();
    }
}